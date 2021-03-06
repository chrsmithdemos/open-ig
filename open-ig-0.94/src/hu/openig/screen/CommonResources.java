/*
 * Copyright 2008-2012, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.screen;

import hu.openig.core.Action0;
import hu.openig.core.Action1;
import hu.openig.core.Configuration;
import hu.openig.core.Func0;
import hu.openig.core.Func1;
import hu.openig.core.Labels;
import hu.openig.core.ResourceLocator;
import hu.openig.core.ResourceLocator.ResourcePlace;
import hu.openig.core.ResourceType;
import hu.openig.core.SaveMode;
import hu.openig.core.SimulationSpeed;
import hu.openig.gfx.BackgroundGFX;
import hu.openig.gfx.ColonyGFX;
import hu.openig.gfx.CommonGFX;
import hu.openig.gfx.DatabaseGFX;
import hu.openig.gfx.DiplomacyGFX;
import hu.openig.gfx.EquipmentGFX;
import hu.openig.gfx.InfoGFX;
import hu.openig.gfx.ResearchGFX;
import hu.openig.gfx.SpacewarGFX;
import hu.openig.gfx.StarmapGFX;
import hu.openig.gfx.StatusbarGFX;
import hu.openig.mechanics.Allocator;
import hu.openig.mechanics.Radar;
import hu.openig.mechanics.Simulator;
import hu.openig.model.AIManager;
import hu.openig.model.GameEnvironment;
import hu.openig.model.Parameters;
import hu.openig.model.Player;
import hu.openig.model.Profile;
import hu.openig.model.Screens;
import hu.openig.model.SoundType;
import hu.openig.model.World;
import hu.openig.music.Music;
import hu.openig.render.TextRenderer;
import hu.openig.sound.Sounds;
import hu.openig.utils.WipPort;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;
import javax.swing.Timer;



/**
 * Contains all common ang game specific graphical and textual resources.
 * @author akarnokd, 2009.12.25.
 */
public class CommonResources implements GameEnvironment {
	/** The main configuration object. */
	public Configuration config;
	/** The main resource locator object. */
	public ResourceLocator rl;
	/** The global and game specific labels. */
	private Labels labels;
	/** The status bar graphics. */
	private StatusbarGFX statusbar;
	/** The background graphics. */
	private BackgroundGFX background;
	/** The equipment graphics. */
	private EquipmentGFX equipment;
	/** The space war graphics. */
	private SpacewarGFX spacewar;
	/** The info graphics. */
	private InfoGFX info;
	/** The research graphics. */
	private ResearchGFX research;
	/** The colony graphics. */
	private ColonyGFX colony;
	/** The starmap graphics. */
	private StarmapGFX starmap;
	/** The database graphics. */
	private DatabaseGFX database;
	/** The common graphics. */
	private CommonGFX common;
	/** The diplomacy graphics. */
	private DiplomacyGFX diplomacy;
	/** The text renderer. */
	private TextRenderer text;
	/** The general control interface. */
	private GameControls control;
	/** The current player's profile. */
	public Profile profile = new Profile();
	/**
	 * The queue for notifying the user about achievements.
	 */
	public final Deque<String> achievementNotifier = new LinkedList<String>();
	// --------------------------------------------
	// The various screen objects
	// --------------------------------------------
	/** The game world. */
	private World world;
	/** Flag to indicate the game world is loading. */
	public boolean worldLoading;
	/** The game is in battle mode. */
	public boolean battleMode;
	/** Flag indicating the statusbar screen to show a non-game statusbar. */
	public boolean nongame;
	/** The common executor service. */
	public final ScheduledExecutorService pool;
	/** The combined timer for synchronized frequency updates. */
	final Timer timer;
	/** The timer tick. */
	long tick;
	/** The registration map. */
	final Map<Closeable, TimerAction> timerHandlers = new HashMap<Closeable, TimerAction>();
	/** The timer action. */
	static class TimerAction {
		/** The operation frequency. */
		public int delay;
		/** The action to invoke. */
		public Action0 action;
		/** The flag to indicate the action was cancelled. */
		public boolean cancelled;
	}
	/** The radar handler. */
	protected Closeable radarHandler;
	/** The allocator handler. */
	protected Closeable allocatorHandler;
	/** The sound objects.*/
	public Sounds sounds;
	/** The music player. */
	public Music music;
	/** The current simulation controls. */
	public SimulationTimer simulation;
	/** The game simulation's parameters. */
	private Parameters params = new Parameters();
	/** Map of currently running AIs. */
	public final Map<Player, Future<?>> runningAI = new HashMap<Player, Future<?>>();
	/** Indicate if an asynchronous save is in operation. */
	public final WipPort saving = new WipPort();
	/** Disable controls and force watching the video. */
	public boolean force;
	/**
	 * Constructor. Initializes and loads all resources.
	 * @param config the configuration object.
	 * @param control the general control
	 */
	public CommonResources(Configuration config, GameControls control) {
		this.config = config;
		this.control = control;

		ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());
		scheduler.setKeepAliveTime(1500, TimeUnit.MILLISECONDS);
		scheduler.allowCoreThreadTimeOut(true);

		/* 
		 * the setRemoveOnCancelPolicy() was introduced in Java 7 to
		 * allow the option to remove tasks from work queue if its initial delay hasn't
		 * elapsed -> therfore, if no other tasks are present, the scheduler might go idle earlier
		 * instead of waiting for the initial delay to pass to discover there is nothing to do.
		 * Because the library is currenlty aimed at Java 6, we use a reflection to set this policy
		 * on a Java 7 runtime. 
		 */
		try {
			Method m = scheduler.getClass().getMethod("setRemoveOnCancelPolicy", Boolean.TYPE);
			m.invoke(scheduler, true);
		} catch (InvocationTargetException ex) {

		} catch (NoSuchMethodException e) {
		} catch (SecurityException e) {
		} catch (IllegalAccessException e) {
		} catch (IllegalArgumentException e) {
		}
		pool = scheduler;

		timer = new Timer(25, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tick++;
				doTimerTick();
			}
		});
		timer.start();
		
		init();
	}
	/** Initialize the resources in parallel. */
	private void init() {
		rl = config.newResourceLocator();
		final ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		try {
			final Future<Labels> labelFuture = exec.submit(new Callable<Labels>() {
				@Override
				public Labels call() throws Exception {
					return new Labels().load(rl, null);
				}
			});
			final Future<StatusbarGFX> statusbarFuture = exec.submit(new Callable<StatusbarGFX>() {
				@Override
				public StatusbarGFX call() throws Exception {
					return new StatusbarGFX().load(rl);
				}
			});
			final Future<BackgroundGFX> backgroundFuture = exec.submit(new Callable<BackgroundGFX>() {
				@Override
				public BackgroundGFX call() throws Exception {
					return new BackgroundGFX().load(rl);
				}
			});
			final Future<EquipmentGFX> equipmentFuture = exec.submit(new Callable<EquipmentGFX>() {
				@Override
				public EquipmentGFX call() throws Exception {
					return new EquipmentGFX().load(rl);
				}
			});
			final Future<SpacewarGFX> spacewarFuture = exec.submit(new Callable<SpacewarGFX>() {
				@Override
				public SpacewarGFX call() throws Exception {
					return new SpacewarGFX().load(rl);
				}
			});
			final Future<InfoGFX> infoFuture = exec.submit(new Callable<InfoGFX>() {
				@Override
				public InfoGFX call() throws Exception {
					return new InfoGFX().load(rl);
				}
			});
			final Future<ResearchGFX> researchFuture = exec.submit(new Callable<ResearchGFX>() {
				@Override
				public ResearchGFX call() throws Exception {
					return new ResearchGFX().load(rl);
				}
			});
			final Future<ColonyGFX> colonyFuture = exec.submit(new Callable<ColonyGFX>() {
				@Override
				public ColonyGFX call() throws Exception {
					return new ColonyGFX().load(rl);
				}
			});
			final Future<StarmapGFX> starmapFuture = exec.submit(new Callable<StarmapGFX>() {
				@Override
				public StarmapGFX call() throws Exception {
					return new StarmapGFX().load(rl);
				}
			});
			final Future<DatabaseGFX> databaseFuture = exec.submit(new Callable<DatabaseGFX>() {
				@Override
				public DatabaseGFX call() throws Exception {
					return new DatabaseGFX().load(rl);
				}
			});
			final Future<TextRenderer> textFuture = exec.submit(new Callable<TextRenderer>() {
				@Override
				public TextRenderer call() throws Exception {
					return new TextRenderer(rl);
				}
			});
			final Future<DiplomacyGFX> diplomacyFuture = exec.submit(new Callable<DiplomacyGFX>() {
				@Override
				public DiplomacyGFX call() throws Exception {
					return new DiplomacyGFX().load(rl);
				}
			});
			final Future<CommonGFX> commonFuture = pool.submit(new Callable<CommonGFX>() {
				@Override
				public CommonGFX call() throws Exception {
					return new CommonGFX().load(rl);
				}
			});
			labels = get(labelFuture);
			statusbar = get(statusbarFuture);
			background = get(backgroundFuture);
			equipment = get(equipmentFuture);
			spacewar = get(spacewarFuture);
			info = get(infoFuture);
			research = get(researchFuture);
			colony = get(colonyFuture);
			starmap = get(starmapFuture);
			database = get(databaseFuture);
			text = get(textFuture);
			diplomacy = get(diplomacyFuture);
			common = get(commonFuture);

			sounds = new Sounds(rl);
			sounds.initialize(config.audioChannels, new Func0<Integer>() {
				@Override
				public Integer invoke() {
					return config.effectVolume;
				}
			});
			
			music = new Music(rl);
			music.setVolume(config.musicVolume);
			
		} finally {
			exec.shutdown();
		}
	}
	/**
	 * Reinitialize the resources by reloading them in the new language.
	 * @param newLanguage the new language
	 */
	public void reinit(String newLanguage) {
		config.language = newLanguage;
		config.save();
		sounds.close();
		init();
	}
	/**
	 * @return the current language code
	 */
	public String language() {
		return config.language;
	}
	/**
	 * Switch to the screen named.
	 * @param to the screen name
	 */
	public void switchScreen(String to) {
		if ("*bridge".equals(to)) {
			control.displayPrimary(Screens.BRIDGE);
		} else
		if ("*starmap".equals(to)) {
			control.displayPrimary(Screens.STARMAP);
		} else
		if ("*colony".equals(to)) {
			control.displayPrimary(Screens.COLONY);
		} else
		if ("*equipment".equals(to)) {
			control.displaySecondary(Screens.EQUIPMENT);
		} else
		if ("*research".equals(to)) {
			control.displaySecondary(Screens.RESEARCH);
		} else
		if ("*production".equals(to)) {
			control.displaySecondary(Screens.PRODUCTION);
		} else
		if ("*information".equals(to)) {
			control.displaySecondary(Screens.INFORMATION_PLANETS);
		} else
		if ("*database".equals(to)) {
			control.displaySecondary(Screens.DATABASE);
		} else
		if ("*bar".equals(to)) {
			control.displaySecondary(Screens.BAR);
		} else
		if ("*diplomacy".equals(to)) {
			control.displaySecondary(Screens.DIPLOMACY);
		}

	}
	/**
	 * Retrieve the result of the future and convert any exception
	 * to runtime exception.
	 * @param <T> the value type
	 * @param future the future for the computation
	 * @return the value
	 */
	public static <T> T get(Future<? extends T> future) {
		try {
			return future.get();
		} catch (ExecutionException ex) {
			throw new RuntimeException(ex);
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}
	/** @return lazily initialize the labels or return the existing one. */
	@Override
	public Labels labels() {
		return labels;
	}
	/** @return lazily initialize the status bar or return the existing one. */
	public StatusbarGFX statusbar() {
		return statusbar;
	}
	/** @return lazily initialize the background or return the existing one. */
	public BackgroundGFX background() {
		return background;
	}
	/** @return lazily initialize the equipment or return the existing one. */
	public EquipmentGFX equipment() {
		return equipment;
	}
	/** @return lazily initialize the spacewar or return the existing one. */
	public SpacewarGFX spacewar() {
		return spacewar;
	}
	/** @return lazily initialize the info or return the existing one. */
	public InfoGFX info() {
		return info;
	}
	/** @return lazily initialize the research or return the existing one. */
	public ResearchGFX research() {
		return research;
	}
	/** @return lazily initialize the colony or return the existing one. */
	public ColonyGFX colony() {
		return colony;
	}
	/** @return lazily initialize the starmap or return the existing one. */
	public StarmapGFX starmap() {
		return starmap;
	}
	/** @return lazily initialize the database or return the existing one. */
	public DatabaseGFX database() {
		return database;
	}
	/** @return lazily initialize the text or return the existing one. */
	public TextRenderer text() {
		return text;
	}
	/** @return lazily initialize the common graphics or return the existing one. */
	public CommonGFX common() {
		return common;
	}
	/** @return lazily initialize the diplomacy graphics or return the existing one. */
	public DiplomacyGFX diplomacy() {
		return diplomacy;
	}
	/**
	 * Convenience method to return a video for the current language.
	 * @param name the video name.
	 * @return the resource place for the video
	 */
	public ResourcePlace video(String name) {
		return rl.get(name, ResourceType.VIDEO);
	}
	/**
	 * Convenience method to return an audio for the current language.
	 * @param name the video name.
	 * @return the resource place for the video
	 */
	public ResourcePlace audio(String name) {
		return rl.get(name, ResourceType.AUDIO);
	}
	/** Close the resources. */
	public void stop() {
		timer.stop();
		
		close0(allocatorHandler);
		close0(radarHandler);
		close0(simulation);


		for (Future<?> sw : runningAI.values()) {
			sw.cancel(true);
		}
		runningAI.clear();
				
		allocatorHandler = null;
		radarHandler = null;
		simulation = null;

		stopMusic();
	}
	/**
	 * Close the given closeable silently.
	 * @param c the closeable
	 */
	void close0(Closeable c) {
		try {
			if (c != null) {
				c.close();
			}
		} catch (IOException ex) {
			// Ignored
		}
	}
	/** Restore the main simulation speed function. Call this function after the battle completes. */
	public void restoreMainSimulationSpeedFunction() {
		replaceSimulation(
				new Action0() {
					@Override
					public void invoke() {
						simulation();
					}
				},
				new Func1<SimulationSpeed, Integer>() {
					@Override
					public Integer invoke(SimulationSpeed value) {
						switch (value) {
						case NORMAL: return 1000;
						case FAST: return 500;
						case ULTRA_FAST: return 50;
						default:
							throw new AssertionError("" + value);
						}
					}
				}
		);
	}
	/**
	 * Invoke the AI for the player if not already running.
	 * @param p the player
	 * @param wip the work in progress port
	 */
	void invokeAI(final Player p, final WipPort wip) {

		Future<?> sw = runningAI.get(p);
		// if not present or finished, start a new
		if (sw == null) {
			wip.inc();
			Runnable run = new Runnable() {
				@Override
				public void run() {
					runAI(p, wip);
				}
			};
			runningAI.put(p, pool.submit(run));
		}
	}
	/**
	 * Run the AI body function.
	 * @param p the player
	 * @param wip the wip
	 */
	void runAI(final Player p, final WipPort wip) {
		try {
			try {
				// parallel convert world state
				p.ai.prepare();
			} finally {
				// wait for all to read world state
				wip.dec();
			}
			// act on the world state
			p.ai.manage();
			// issue commands
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					try {
						if (!battleMode) {
							p.ai.apply();
						}
					} finally {
						runningAI.remove(p);
					}
				}
			});
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	/**
	 * Execute a step of simulation.
	 */
	public void simulation() {
		if (Simulator.compute(world)) {
			if (world.scripting.mayAutoSave()) {
				control.save(null, SaveMode.AUTO);
			}
		}
		
		// run AI routines in background
		final WipPort wip = new WipPort(1);
		for (final Player p : world.players.values()) {
			if (p.ai != null) {
				invokeAI(p, wip);
			}
		}
		wip.dec();
		try {
			wip.await();
		} catch (InterruptedException ex) {
			// ignored
		}
		control.repaintInner();
	}
	/**
	 * Replace the current simulation controls with a new
	 * simulation controls.
	 * @param action the new simulation action
	 * @param delay the function to tell the delay value from speed enumeration.
	 */
	public void replaceSimulation(Action0 action, Func1<SimulationSpeed, Integer> delay) {
		close0(simulation);
		simulation = newSimulationTimer(action, delay);
	}
	/** 
	 * Start the timed actions.
	 * @param withMusic set if play music 
	 */
	public void start(boolean withMusic) {
		restoreMainSimulationSpeedFunction();

		radarHandler = register(1000, new Action0() {
			@Override
			public void invoke() {
				world.statistics.playTime++;
				if (!simulation.paused()) {
					world.statistics.simulationTime++;
				}
				Radar.compute(world);
				if (control.primary() == Screens.STARMAP) {
					control.repaintInner();
				}
			}
		});
		allocatorHandler = register(1000, new Action0() {
			@Override
			public void invoke() {
				Allocator.compute(world, pool);
				control.repaintInner();
			}
		});
		Allocator.compute(world, pool);
		Radar.compute(world);
		
		simulation.resume();
		
		timer.start();
		if (withMusic) {
			playRegularMusic();
		}
		
		battleMode = false;
	}
	/** @return the world instance. */
	@Override
	public World world() {
		return world;
	}
	/**
	 * Set the world.
	 * @param w the new world
	 * @return this
	 */
	public CommonResources world(World w) {
		if (this.world != null && w == null) {
			world.scripting.done();
		}
		this.world = w;
		return this;
	}
	/**
	 * Set the game control peer.
	 * @param ctrl the new game control peer
	 * @return this
	 */
	public CommonResources control(GameControls ctrl) {
		this.control = ctrl;
		return this;
	}
	/** @return the control object */
	public GameControls control() {
		return control;
	}
	/** Execute the timer tick actions. */
	void doTimerTick() {
		for (TimerAction act : new ArrayList<TimerAction>(timerHandlers.values())) {
			if (!act.cancelled) {
				if ((tick * timer.getDelay()) % act.delay == 0) {
					try {
						act.action.invoke();
					} catch (CancellationException ex) {
						act.cancelled = true;
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
		}
	}
	/**
	 * Register a repeating action with the given delay.
	 * @param delay the reqested frequency in milliseconds
	 * @param action the action to invoke
	 * @return the handler to close this instance
	 */
	public Closeable register(int delay, Action0 action) {
		if (delay % timer.getDelay() != 0 || delay == 0) {
			throw new IllegalArgumentException("The delay must be in multiples of " + timer.getDelay() + " milliseconds!");
		}
		if (action == null) {
			throw new IllegalArgumentException("action is null");
		}
		final TimerAction ta = new TimerAction();
		ta.delay = delay;
		ta.action = action;
		Closeable res = new Closeable() {
			@Override
			public void close() throws IOException {
				ta.cancelled = true;
				timerHandlers.remove(this);
			}
		};
		timerHandlers.put(res, ta);
		return res;
	}
	/**
	 * Convenience method to start playing the original three musics.
	 */
	public void playRegularMusic() {
		stopMusic();
		music.playLooped(new Func0<Integer>() {
			@Override
			public Integer invoke() {
				return config.musicVolume;
			}
		}, "music/Music1", "music/Music2", "music/Music3");
	}
	/** Convenience method to start playing the original battle music. */
	public void playBattleMusic() {
		stopMusic();
		music.playLooped(new Func0<Integer>() {
			@Override
			public Integer invoke() {
				return config.musicVolume;
			}
		}, "music/War");
	}
	@Override
	public void stopMusic() {
		music.close();
	}
	/**
	 * Create a new simulation timer controls with the given action.
	 * @param action the simulation action
		 * @param delay the function which tells the delay from the speed enumeration
	 * @return the new simulation timer
	 */
	public SimulationTimer newSimulationTimer(Action0 action, Func1<SimulationSpeed, Integer> delay) {
		return new SimulationTimer(action, delay);
	}
	/**
	 * The class to manage simulation timer related commands (pause, resume, curren).
	 * @author akarnokd, 2011.09.01.
	 */
	public class SimulationTimer implements Closeable {
		/** The handler for the timer. */
		protected Closeable handler;
		/** The current speed value. */
		protected SimulationSpeed speed = SimulationSpeed.NORMAL;
		/** Is the simulation paused? */
		protected boolean paused = true;
		/** The timer action. */
		protected final Action0 action;
		/** The delay computation function. */
		protected final Func1<SimulationSpeed, Integer> delay;
		/**
		 * Constructor.
		 * @param action the timer action.
		 * @param delay the function which tells the delay from the speed enumeration
		 */
		public SimulationTimer(Action0 action, Func1<SimulationSpeed, Integer> delay) {
			this.action = action;
			this.delay = delay;
		}
		/**
		 * Pauses the simulation if not already paused.
		 */
		public void pause() {
			if (!paused) {
				paused = true;
				close();
			}
		}
		/** Resumes the simulation if not already running. */
		public void resume() {
			if (paused) {
				speed(speed);
			}
		}
		/** Register the action with the timer and delay. */
		void registerAction() {
			close();
			handler = register(delay.invoke(speed), action);
		}
		/**
		 * Sets a new simulation speed and resumes the simulation.
		 * @param newSpeed the new speed
		 */
		public void speed(SimulationSpeed newSpeed) {
			if (newSpeed != speed || paused) {
				speed = newSpeed;
				paused = false;
				registerAction();
			}
		}
		/** @return the current simulation speed. */
		public SimulationSpeed speed() {
			return speed;
		}
		/** @return true if the simulation is paused. */
		public boolean paused() {
			return paused;
		}
		@Override
		public void close() {
			close0(handler);
			handler = null;
		}
		/**
		 * Returns the simulation speed in milliseconds.
		 * @return the simulation speed in milliseconds.
		 */
		public int speedValue() {
			return delay.invoke(speed);
		}
	}
	@Override
	public AIManager getAI(Player player) {
		return control.aiFactory().invoke(player);
	}
	@Override
	public void startBattle() {
		control.startBattle();
	}
	@Override
	public void playAudio(String name) {
		Music m = new Music(rl);
		m.onComplete = new Action1<String>() {
			@Override
			public void invoke(String value) {
				if (world != null) {
					world.scripting.onSoundComplete(value);
				}
			}
		};
		m.playSequence(new Func0<Integer>() {
			@Override
			public Integer invoke() {
				return config.effectVolume;
			}
		}, name);
	}
	@Override
	public void playSound(SoundType type) {
		sounds.play(type);
	}
	@Override
	public void playVideo(final String name, final Action0 action) {
		control.playVideos(new Action0() {
			@Override
			public void invoke() {
				if (action != null) {
					action.invoke();
				}
				if (world != null) {
					world.scripting.onVideoComplete(name);
				}
			}
		}, name);
	}
	@Override
	public Parameters params() {
		return params;
	}
	@Override
	public Configuration config() {
		return config;
	}
	@Override
	public Deque<String> achievementQueue() {
		return achievementNotifier;
	}
	@Override
	public Profile profile() {
		return profile;
	}
	@Override
	public void forceMessage(String messageId, Action0 onSeen) {
		control().forceMessage(messageId, onSeen);
	}
	@Override
	public void loseGame() {
		control().loseGame();
	}
	@Override
	public void winGame() {
		control().winGame();
	}
	@Override
	public void showObjectives(boolean state) {
		control().showObjectives(state);
	}
	@Override
	public int simulationSpeed() {
		return simulation.speedValue();
	}
	@Override
	public void pause() {
		simulation.pause();
	}
	@Override
	public void speed1() {
		if (!simulation.paused()) {
			simulation.speed(SimulationSpeed.NORMAL);
		}
	}
}

/*
 * Copyright 2008-2011, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.screens;

import hu.openig.core.Act;
import hu.openig.core.Configuration;
import hu.openig.core.ResourceLocator;
import hu.openig.model.Planet;
import hu.openig.ui.UIMouse;

import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * The base game window which handles paint and input events.
 * @author akarnokd, 2009.12.23.
 */
public class GameWindow extends JFrame implements GameControls {
	/**	 */
	private static final long serialVersionUID = 4521036079508511968L;
	/** 
	 * The component that renders the primary and secondary screens into the current window.
	 * @author akarnokd, 2009.12.23.
	 */
	class ScreenRenderer extends JComponent {
		/** */
		private static final long serialVersionUID = -4538476567504582641L;
		/** Constructor. */
		public ScreenRenderer() {
			addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					try {
						if (primary != null) {
							primary.resize();
						}
						if (secondary != null) {
							secondary.resize();
						}
						if (movie != null) {
							movie.resize();
						}
						if (statusbarVisible) {
							statusbar.resize();
						}
					} catch (Throwable t) {
						t.printStackTrace();
					}
					repaintRequest = true;
				}
			});
		}
		@Override
		public void paint(Graphics g) {
			boolean r0 = repaintRequest;
			boolean r1 = repaintRequestPartial;
			repaintRequest = false;
			repaintRequestPartial = false;
			
			Graphics2D g2 = (Graphics2D)g;
			try {
				if (movieVisible) {
					movie.draw(g2);
				} else {
					if (r1 && !r0) {
						if (secondary != null) {
							secondary.draw(g2);
						}
					} else {
						if (primary != null) {
							primary.draw(g2);
						}
						if (secondary != null) {
							secondary.draw(g2);
						}
						if (statusbarVisible) {
							statusbar.draw(g2);
						}
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	/** A pending repaint request. */
	boolean repaintRequest;
	/** A partial repaint request. */
	boolean repaintRequestPartial;
	/** The primary screen. */
	ScreenBase primary;
	/** The secondary screen drawn over the first. */
	ScreenBase secondary;
	/** The status bar to display over the primary and secondary screens. */
	ScreenBase statusbar;
	/** The top overlay for playing 'full screen' movies. */
	MovieScreen movie;
	/** Is the status bar visible? */
	boolean statusbarVisible;
	/** Is the movie visible. */
	boolean movieVisible;
	/** The configuration object. */
	Configuration config;
	/** The common resource locator. */
	ResourceLocator rl;
	/** The common resources. */
	CommonResources commons;
	/** The surface used to render the screens. */
	ScreenRenderer surface;
	/** The list of screens. */
	List<ScreenBase> screens;
	/** 
	 * Constructor. 
	 * @param config the configuration object.
	 */
	public GameWindow(Configuration config) {
		super("Open Imperium Galactica " + Configuration.VERSION);
		URL icon = this.getClass().getResource("/hu/openig/gfx/open-ig-logo.png");
		if (icon != null) {
			try {
				setIconImage(ImageIO.read(icon));
			} catch (IOException e) {
				e.printStackTrace();
//				config.log("ERROR", e.getMessage(), e);
			}
		}
		
		this.commons = new CommonResources(config, this);
		this.config = config;
		this.rl = commons.rl;
		this.surface = new ScreenRenderer();
		
		Container c = getContentPane();
		GroupLayout gl = new GroupLayout(c);
		c.setLayout(gl);
		
		
		gl.setHorizontalGroup(
			gl.createSequentialGroup()
			.addComponent(surface, 640, 640, Short.MAX_VALUE)
		);
		gl.setVerticalGroup(
			gl.createSequentialGroup()
			.addComponent(surface, 480, 480, Short.MAX_VALUE)
		);
		pack();
		setMinimumSize(getSize());
		setLocationRelativeTo(null);
//		if (config.width > 0) {
//			setBounds(config.left, config.top, config.width, config.height);
//		}
		
		// Event handling
		
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});
		MouseActions ma = new MouseActions();
		surface.addMouseListener(ma);
		surface.addMouseMotionListener(ma);
		surface.addMouseWheelListener(ma);
		addKeyListener(new KeyEvents());
		
		// load resources
		initScreens();
	}
	/** 
	 * A copy constructor to save the state of a previous window without reloading
	 * the resources. 
	 * @param that the previous window
	 * @param undecorated should it be undecorated?
	 */
	public GameWindow(GameWindow that, boolean undecorated) {
		setUndecorated(undecorated);
		this.setTitle(that.getTitle());
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});

		this.surface = new ScreenRenderer();
		
		this.commons = that.commons;
		this.commons.control(this);
		this.rl = that.rl;
		this.config = that.config;
		this.screens = that.screens;
		setIconImage(that.getIconImage());
		this.primary = that.primary;
		this.secondary = that.secondary;
		this.movie = that.movie;
		this.movieVisible = that.movieVisible;
		this.statusbar = that.statusbar;
		this.statusbarVisible = that.statusbarVisible;
		
		Container c = getContentPane();
		GroupLayout gl = new GroupLayout(c);
		c.setLayout(gl);
		
		
		gl.setHorizontalGroup(
			gl.createSequentialGroup()
			.addComponent(surface, 640, 640, Short.MAX_VALUE)
		);
		gl.setVerticalGroup(
			gl.createSequentialGroup()
			.addComponent(surface, 480, 480, Short.MAX_VALUE)
		);
		pack();
		setMinimumSize(getSize());
		setLocationRelativeTo(null);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});
		MouseActions ma = new MouseActions();
		surface.addMouseListener(ma);
		surface.addMouseMotionListener(ma);
		surface.addMouseWheelListener(ma);
		addKeyListener(new KeyEvents());
	}
	@Override
	public void exit() {
		commons.close();
		uninitScreens();
		dispose();
		try {
			config.watcherWindow.close();
		} catch (IOException e) {
		}
	}
	@Override
	public void switchLanguage(String newLanguage) {
		commons.reinit(newLanguage);
		surface.repaint();
		for (ScreenBase sb : screens) {
			sb.initialize(commons);
		}
	}
	/** Initialize the various screen renderers. */
	protected void initScreens() {
		screens = new ArrayList<ScreenBase>();
		
		try {
			for (Field f : commons.screens.getClass().getFields()) {
				if (ScreenBase.class.isAssignableFrom(f.getType())) {
					ScreenBase sb = ScreenBase.class.cast(f.getType().newInstance());
					f.set(commons.screens, sb);
					screens.add(sb);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		for (ScreenBase sb : screens) {
			sb.initialize(commons);
		}
		movie = commons.screens.movie;
		statusbar = commons.screens.statusbar;
		
		displayPrimary(Screens.MAIN);
	}
	/** Unitialize the screens. */
	protected void uninitScreens() {
		for (ScreenBase sb : screens) {
			if (primary == sb || secondary == sb) {
				sb.onLeave();
			}
			sb.onFinish();
		}
		primary = null;
		secondary = null;
	}
	/**
	 * Returns a screen instance for the given screen enum.
	 * @param screen the screen.
	 * @param asPrimary as primary screen?
	 * @return the reference to the new screen.
	 */
	ScreenBase display(Screens screen, boolean asPrimary) {
		ScreenBase sb = null;
		Screens mode = null;
		switch (screen) {
		case ACHIEVEMENTS:
			sb = commons.screens.statisticsAchievements;
			mode = Screens.ACHIEVEMENTS;
			break;
		case STATISTICS:
			sb = commons.screens.statisticsAchievements;
			mode = Screens.STATISTICS;
			break;
		case BAR:
			sb = commons.screens.bar;
			break;
		case BRIDGE:
			sb = commons.screens.bridge;
			break;
		case COLONY:
			sb = commons.screens.colony;
			break;
		case DIPLOMACY:
			sb = commons.screens.diplomacy;
			break;
		case EQUIPMENT_FLEET:
		case EQUIPMENT_PLANET:
			sb = commons.screens.equipment;
			mode = screen;
			break;
		case INFORMATION_COLONY:
		case INFORMATION_ALIENS:
		case INFORMATION_BUILDINGS:
		case INFORMATION_FINANCIAL:
		case INFORMATION_FLEETS:
		case INFORMATION_INVENTIONS:
		case INFORMATION_MILITARY:
		case INFORMATION_PLANETS:
			sb = commons.screens.info;
			mode = screen;
			break;
		case PRODUCTION:
			sb = commons.screens.researchProduction;
			mode = Screens.PRODUCTION;
			break;
		case RESEARCH:
			sb = commons.screens.researchProduction;
			mode = Screens.RESEARCH;
			break;
		case SPACEWAR:
			sb = commons.screens.spacewar;
			break;
		case STARMAP:
			sb = commons.screens.starmap;
			break;
		case SHIPWALK:
			sb = commons.screens.shipwalk;
			break;
		case DATABASE:
			sb = commons.screens.database;
			break;
		case LOADING:
			sb = commons.screens.loading;
			break;
		case LOAD_SAVE:
			sb = commons.screens.loadSave;
			break;
		case MAIN:
			sb = commons.screens.main;
			break;
		case MULTIPLAYER:
			sb = null; // TODO multiplayer screen
			break;
		case SINGLEPLAYER:
			sb = commons.screens.singleplayer;
			break;
		case VIDEOS:
			sb = commons.screens.videos;
			break;
		default:
		}
		if (asPrimary) {
			hideMovie();
			if (secondary != null) {
				secondary.onLeave();
				secondary = null;
				surface.repaint();
			}
			if (primary == null || primary.screen() != screen) {
				if (primary != null) {
					primary.onLeave();
				}
				primary = sb;
				if (primary != null) {
					primary.resize();
					primary.onEnter(mode);
					surface.repaint();
				}
			}
		} else {
			if (secondary == null || secondary.screen() != screen) {
				if (secondary != null) {
					secondary.onLeave();
				}
				secondary = sb;
				if (secondary != null) {
					secondary.resize();
					secondary.onEnter(mode);
					surface.repaint();
				}
			}
		}
		return sb;
	}
	@Override
	public ScreenBase displayPrimary(Screens screen) {
		return display(screen, true);
	}
	@Override
	public ScreenBase displaySecondary(Screens screen) {
		return display(screen, false);
	}
	/**
	 * Display the movie window.
	 */
	public void displayMovie() {
		if (!movieVisible) {
			movieVisible = true;
			movie.onEnter(null);
			doMoveMouseAgain();
			surface.repaint();
		}
	}
	/**
	 * Hide the movie windows.
	 */
	public void hideMovie() {
		if (movieVisible) {
			movieVisible = false;
			movie.onLeave();
			doMoveMouseAgain();
			surface.repaint();
		}
	}
	/**
	 * Display the status bar.
	 */
	@Override 
	public void displayStatusbar() {
		if (!statusbarVisible) {
			statusbarVisible = true;
			statusbar.resize();
			statusbar.onEnter(null);
			doMoveMouseAgain();
			surface.repaint();
		}
	}
	/* (non-Javadoc)
	 * @see hu.openig.v1.GameControls#hideSecondary()
	 */
	@Override
	public void hideSecondary() {
		if (secondary != null) {
			secondary.onLeave();
			secondary = null;
			doMoveMouseAgain();
			surface.repaint();
		}
	}
	/**
	 * Hide the status bar.
	 */
	@Override 
	public void hideStatusbar() {
		if (statusbarVisible) {
			statusbarVisible = true;
			statusbar.onLeave();
			doMoveMouseAgain();
			surface.repaint();
		}
	}
	/**
	 * On screen transitions, issue a fake mouse moved events to support the highlighting.
	 */
	public void doMoveMouseAgain() {
		boolean result = false;
		ScreenBase sb = statusbar;
		UIMouse m = UIMouse.createCurrent(surface);
		if (statusbarVisible) {
			result |= sb.mouse(m);
		}
		ScreenBase pri = primary;
		ScreenBase sec = secondary;
		if (pri != null) {
			result |= pri.mouse(m);
		} else
		if (sec != null) {
			result |= sec.mouse(m);
		}
		if (result) {
			repaint();
		}
	}
	/**
	 * The common key manager.
	 * @author akarnokd, 2009.12.24.
	 */
	class KeyEvents extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			if (!handleScreenSwitch(e)) {
				boolean rep = false;
				ScreenBase pri = primary;
				ScreenBase sec = secondary;
				if (movieVisible) {
					rep |= movie.keyboard(e);
				} else
				if (sec != null) {
					rep |= sec.keyboard(e);
				} else
				if (pri != null) {
					rep |= pri.keyboard(e);
				}
				if (rep) {
					repaint();
				}
			}
		}
		/**
		 * Handle the screen switch if the appropriate key is pressed.
		 * @param e the key event
		 * @return true if the key event was handled
		 */
		boolean handleScreenSwitch(KeyEvent e) {
			boolean result = false;
			if (e.isAltDown()) {
				if (e.getKeyCode() == KeyEvent.VK_F4) {
					exit();
				}
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					switchFullscreen();
				}
				e.consume();
				return true;
			}
			if (!commons.worldLoading && commons.world() != null && !movieVisible) {
				result = true;
				if (e.getKeyChar() == '+') {
					commons.world().player.moveNextPlanet();
					repaintInner();
				} else
				if (e.getKeyChar() == '-') {
					commons.world().player.movePrevPlanet();
					repaintInner();
				}
				switch (e.getKeyCode()) {
				case KeyEvent.VK_F1:
					displayPrimary(Screens.BRIDGE);
					break;
				case KeyEvent.VK_F2:
					displayPrimary(Screens.STARMAP);
					break;
				case KeyEvent.VK_F3:
					displayPrimary(Screens.COLONY);
					break;
				case KeyEvent.VK_F4:
					if (secondary != null) {
						if (secondary.screen() == Screens.EQUIPMENT_FLEET
								|| secondary.screen() == Screens.EQUIPMENT_PLANET) {
									hideSecondary();
						} else
						if (secondary.screen() == Screens.INFORMATION_COLONY
								|| secondary.screen() == Screens.INFORMATION_PLANETS) {
							displaySecondary(Screens.EQUIPMENT_PLANET);
						} else {
							displaySecondary(Screens.EQUIPMENT_FLEET);
						}
					} else {
						switch (primary.screen()) {
						case COLONY:
							displaySecondary(Screens.EQUIPMENT_PLANET);
							break;
						default:
							displaySecondary(Screens.EQUIPMENT_FLEET);
						}
					}
					break;
				case KeyEvent.VK_F5:
					if (secondary != null) {
						if (secondary.screen() == Screens.PRODUCTION) {
							hideSecondary();
						} else {
							displaySecondary(Screens.PRODUCTION);
						}
					} else {
						displaySecondary(Screens.PRODUCTION);
					}
					break;
				case KeyEvent.VK_F6:
					if (secondary != null) {
						if (secondary.screen() == Screens.RESEARCH) {
							hideSecondary();
						} else {
							displaySecondary(Screens.RESEARCH);
						}
					} else {
						displaySecondary(Screens.RESEARCH);
					}
					break;
				case KeyEvent.VK_F7:
					if (secondary != null) {
						switch (secondary.screen()) {
						case EQUIPMENT_FLEET:
							displaySecondary(Screens.INFORMATION_INVENTIONS);
							break;
						case EQUIPMENT_PLANET:
							displaySecondary(Screens.INFORMATION_COLONY);
							break;
						case DIPLOMACY:
							displaySecondary(Screens.INFORMATION_ALIENS);
							break;
						case INFORMATION_ALIENS:
						case INFORMATION_BUILDINGS:
						case INFORMATION_COLONY:
						case INFORMATION_FINANCIAL:
						case INFORMATION_FLEETS:
						case INFORMATION_INVENTIONS:
						case INFORMATION_MILITARY:
						case INFORMATION_PLANETS:
							hideSecondary();
							break;
						case RESEARCH:
						case PRODUCTION:
							displaySecondary(Screens.INFORMATION_INVENTIONS);
							break;
						default:
							displaySecondary(Screens.INFORMATION_PLANETS);
						}
					} else {
						switch (primary.screen()) {
						case STARMAP:
							displaySecondary(Screens.INFORMATION_PLANETS);
							break;
						case COLONY:
							displaySecondary(Screens.INFORMATION_COLONY);
							break;
						default:
							displaySecondary(Screens.INFORMATION_PLANETS);
						} 
					}
					break;
				case KeyEvent.VK_F8:
					if (secondary != null) {
						if (secondary.screen() == Screens.DATABASE) {
							hideSecondary();
						} else {
							displaySecondary(Screens.DATABASE);
						}
					} else {
						displaySecondary(Screens.DATABASE);
					}
					break;
				case KeyEvent.VK_F9:
					if (commons.world().level > 1) {
						if (secondary != null) {
							if (secondary.screen() == Screens.BAR) {
								hideSecondary();
							} else {
								displaySecondary(Screens.BAR);
							}
						} else {
							displaySecondary(Screens.BAR);
						}
					}
					break;
				case KeyEvent.VK_F10:
					if (commons.world().level > 3) {
						if (secondary != null) {
							if (secondary.screen() == Screens.DIPLOMACY) {
								hideSecondary();
							} else {
								displaySecondary(Screens.DIPLOMACY);
							}
						} else {
							displaySecondary(Screens.DIPLOMACY);
						}
					}
					break;
				case KeyEvent.VK_F11:
					if (secondary != null) {
						if (secondary.screen() == Screens.STATISTICS) {
							hideSecondary();
						} else {
							displaySecondary(Screens.STATISTICS);
						}
					} else {
						displaySecondary(Screens.STATISTICS);
					}
					break;
				case KeyEvent.VK_F12:
					if (secondary != null) {
						if (secondary.screen() == Screens.ACHIEVEMENTS) {
							hideSecondary();
						} else {
							displaySecondary(Screens.ACHIEVEMENTS);
						}
					} else {
						displaySecondary(Screens.ACHIEVEMENTS);
					}
					break;
				case KeyEvent.VK_1:
					if (e.isControlDown()) {
						commons.world().level = 1;
						if (primary != null) {
							primary.onLeave();
						}
						primary = null;
						displayPrimary(Screens.BRIDGE);
					} else {
						commons.world().simulator.setDelay(1000);
						commons.world().simulator.start();
					}
					break;
				case KeyEvent.VK_2:
					if (e.isControlDown()) {
						commons.world().level = 2;
						if (primary != null) {
							primary.onLeave();
						}
						primary = null;
						displayPrimary(Screens.BRIDGE);
					} else {
						commons.world().simulator.setDelay(500);
						commons.world().simulator.start();
					}
					break;
				case KeyEvent.VK_3:
					if (e.isControlDown()) {
						commons.world().level = 3;
						if (primary != null) {
							primary.onLeave();
						}
						primary = null;
						displayPrimary(Screens.BRIDGE);
					} else {
						commons.world().simulator.setDelay(250);
						commons.world().simulator.start();
					}
					break;
				case KeyEvent.VK_SPACE:
					if (commons.world().simulator.isRunning()) {
						commons.world().simulator.stop();
					} else {
						commons.world().simulator.start();
					}
					repaintInner();
					break;
				case KeyEvent.VK_4:
					if (e.isControlDown()) {
						commons.world().level = 4;
						if (primary != null) {
							primary.onLeave();
						}
						primary = null;
						displayPrimary(Screens.BRIDGE);
					}
					break;
				case KeyEvent.VK_5:
					if (e.isControlDown()) {
						commons.world().level = 5;
						if (primary != null) {
							primary.onLeave();
						}
						primary = null;
						displayPrimary(Screens.BRIDGE);
					}
					break;
				case KeyEvent.VK_O:
					if (e.isControlDown()) {
						Planet p = commons.world().player.currentPlanet; 
						if (p != null) {
							p.owner = commons.world().player;
							if (p.race == null || !p.race.isEmpty()) {
								p.race = p.owner.race;
							}
							repaintInner();
						}
					} else {
						result = false;
					}
					break;
//				case KeyEvent.VK_R:
//					if (e.isControlDown()) { // reload labels
//						commons.world().labels.load(commons.rl, commons.world().name);
//						repaintInner();
//					} else {
//						result = false;
//					}
//					break;
//				case KeyEvent.VK_T:
//					if (e.isControlDown()) { // reload labels
//						if (commons.world().player.currentResearch != null) {
//							ResearchType rt = commons.world().player.currentResearch;
//							if (commons.world().player.runningResearch == rt) {
//								commons.world().player.runningResearch = null;
//								commons.world().player.research.remove(rt);
//							}
//							commons.world().player.availableResearch.add(rt);
//							if (secondary != null 
//									&& (secondary.screen() == Screens.RESEARCH || secondary.screen() == Screens.PRODUCTION)) {
//								ResearchProductionScreen rps = ((ResearchProductionScreen)secondary);
//								rps.displayCategory(rt.category);
//							}
//						}
//						repaintInner();
//					} else {
//						result = false;
//					}
//					break;
				default:
					result = false;
				}
			}
			if (result) {
				e.consume();
			}
			return result;
		}
		/**
		 * Toggle between full screen mode.
		 */
		void switchFullscreen() {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			for (final GraphicsDevice gs : ge.getScreenDevices()) {
				if (gs.getDefaultConfiguration().getBounds().intersects(getBounds())) {
					GameWindow gw = new GameWindow(GameWindow.this, gs.getFullScreenWindow() == null);
					if (gs.getFullScreenWindow() == null) {
			    		dispose();
						gs.setFullScreenWindow(gw);
						setSize(gs.getDefaultConfiguration().getBounds().getSize());
					} else {
			    		dispose();
						gs.setFullScreenWindow(null);
						gw.setVisible(true);
					}
					gw.doMoveMouseAgain();
					break;
				}
			}
		}
		/** Create a second window with the same content. */
		void newWindow() {
			GameWindow gw = new GameWindow(GameWindow.this, false);
			gw.setVisible(true);
			gw.doMoveMouseAgain();
		}
	}
	/**
	 * The common mouse action manager.
	 * @author akarnokd, 2009.12.23.
	 */
	class MouseActions extends MouseAdapter {
		/** 
		 * Transform and invoke the mouse action on the current top screen. 
		 * @param e the mouse event
		 */
		void invoke(MouseEvent e) {
			ScreenBase pri = primary;
			ScreenBase sec = secondary;
			boolean rep = false;
			if (movieVisible) {
				rep = movie.mouse(UIMouse.from(e));
			} else
			if (statusbarVisible) {
				if (statusbar.mouse(UIMouse.from(e))) {
					repaintInner();
					return;
				}
			}
			if (sec != null) {
				rep = sec.mouse(UIMouse.from(e));
			} else
			if (pri != null) {
				rep = pri.mouse(UIMouse.from(e));
			}
			if (rep) {
				repaintInner();
			}
		}
		@Override
		public void mousePressed(MouseEvent e) {
			invoke(e);
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			invoke(e);
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			invoke(e);
		}
		@Override
		public void mouseMoved(MouseEvent e) {
			invoke(e);
		}
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			invoke(e);
		}
		@Override
		public void mouseClicked(MouseEvent e) {
			invoke(e);
		}
		@Override
		public void mouseEntered(MouseEvent e) {
			invoke(e);
		}
		@Override
		public void mouseExited(MouseEvent e) {
			invoke(e);
		}
	}
	@Override
	public void playVideos(final Act onComplete, String... videos) {
		for (String s : videos) {
			movie.mediaQueue.add(s);
		}
		movie.playbackFinished = new Act() {
			@Override
			public void act() {
				hideMovie();
				if (onComplete != null) {
					onComplete.act();
				}
			}
		};
		displayMovie();
	}
	/* (non-Javadoc)
	 * @see hu.openig.v1.screens.GameControls#playVideos(java.lang.String[])
	 */
	@Override
	public void playVideos(String... videos) {
		playVideos(null, videos);
	}
	@Override
	public int getInnerHeight() {
		return surface.getHeight();
	}
	@Override
	public int getInnerWidth() {
		return surface.getWidth();
	}
	@Override
	public void repaintInner() {
		// issue a single repaint, e.g., coalesce the repaints
		if (!repaintRequest) {
			repaintRequest = true;
			surface.repaint();
		}
	}
	@Override
	public void repaintInner(int x, int y, int w, int h) {
		repaintRequestPartial = true;
		surface.repaint(x, y, w, h);
	}
	@Override
	public FontMetrics fontMetrics(int size) {
		return getFontMetrics(getFont().deriveFont((float)size).deriveFont(Font.BOLD));
	}
}

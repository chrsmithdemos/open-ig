/*
 * Copyright 2008-2011, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.model;

import hu.openig.core.Difficulty;
import hu.openig.core.Func1;
import hu.openig.core.Labels;
import hu.openig.core.PlanetType;
import hu.openig.core.ResourceLocator;
import hu.openig.model.Bridge.Level;
import hu.openig.render.TextRenderer;
import hu.openig.utils.ImageUtils;
import hu.openig.utils.JavaUtils;
import hu.openig.utils.WipPort;
import hu.openig.utils.XElement;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The world object.
 * @author akarnokd, 2009.10.25.
 */
public class World {
	/** The name of the world. */
	public String name;
	/** The current world level. */
	public int level;
	/** The current player. */
	public Player player;
	/** The map of player-id to player object. */
	public final Map<String, Player> players = new HashMap<String, Player>();
	/** The time. */
	public final GregorianCalendar time = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
	{
		time.set(GregorianCalendar.YEAR, 3427);
		time.set(GregorianCalendar.MONTH, GregorianCalendar.AUGUST);
		time.set(GregorianCalendar.DATE, 13);
		time.set(GregorianCalendar.HOUR_OF_DAY, 8);
		time.set(GregorianCalendar.MINUTE, 50);
		time.set(GregorianCalendar.SECOND, 0);
		time.set(GregorianCalendar.MILLISECOND, 0);
	}
	/** All planets on the starmap. */
	public final Map<String, Planet> planets = new LinkedHashMap<String, Planet>();
	/** The list of available researches. */
	public final Map<String, ResearchType> researches = new HashMap<String, ResearchType>();
	/** The available crew-talks. */
	public Talks talks;
	/** The ship-walk definitions. */
	public Walks walks;
	/** The game definition. */
	public GameDefinition definition;
	/** The difficulty of the game. */
	public Difficulty difficulty;
	/** The bridge definition. */
	public Bridge bridge;
	/** The galaxy model. */
	public GalaxyModel galaxyModel;
	/** The buildings model. */
	public BuildingModel buildingModel;
	/** The game specific labels. */
	public Labels labels;
	/** The resource locator. */
	public ResourceLocator rl;
	/** Retrieve the auto build limit. */
	public Func1<Void, Integer> getAutoBuildLimit;
	/** Retrieve the auto repair settings. */
	public Func1<Void, Boolean> isAutoRepair;
	/** The global world statistics. */
	public final WorldStatistics statistics = new WorldStatistics();
	/** The date formatter. */
	public final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			sdf.setCalendar(new GregorianCalendar(TimeZone.getTimeZone("GMT")));
			return sdf;
		}
	};
	/**
	 * The random number generator for simulation/AI activities.
	 */
	public final ThreadLocal<Random> random = new ThreadLocal<Random>() {
		@Override
		public Random get() {
			return new Random();
		}
	};
	/** The sequence to assign unique ids to fleets. */
	public int fleetIdSequence;
	/** The test questions. */
	public Map<String, TestQuestion> test;
	/** The diplomacy definition. */
	public Map<String, Diplomacy> diplomacy;
	/** The battle object. */
	public Battle battle;
	/** The list of pending battles. */
	public Deque<BattleInfo> pendingBattles = new LinkedList<BattleInfo>();
	/** The callback function to initiate a battle. */
	public Func1<Void, Void> startBattle;
	/**
	 * Load the game world's resources.
	 * @param resLocator the resource locator
	 * @param game the game directory
	 */
	public void load(final ResourceLocator resLocator, final String game) {
		this.name = game;
		this.rl = resLocator;
		final ThreadPoolExecutor exec = 
			new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), 
					Integer.MAX_VALUE, 1, TimeUnit.SECONDS, 
					new LinkedBlockingQueue<Runnable>(),
					new ThreadFactory() {
				/** The thread count. */
				final AtomicInteger count = new AtomicInteger();
				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "World-Loader-" + count.incrementAndGet());
					t.setPriority(Thread.MIN_PRIORITY);
					return t;
				}
			});
		exec.allowCoreThreadTimeOut(true);
		final WipPort wip = new WipPort(8);
		try {
			level = definition.startingLevel;
			
			processResearches(rl.getXML(game + "/tech"));
			
			processPlayers(rl.getXML(game + "/players")); 
			
			talks = new Talks();
			walks = new Walks();
			buildingModel = new BuildingModel();
			galaxyModel = new GalaxyModel();
			test = JavaUtils.newLinkedHashMap();
			diplomacy = JavaUtils.newLinkedHashMap();
			battle = new Battle();
			
			exec.submit(new Runnable() {
				@Override
				public void run() {
					try {
						TestQuestion.parse(rl.getXML(game + "/test"), test);
					} catch (Throwable t) {
						t.printStackTrace();
					} finally {
						wip.dec();
					}
				}
			});

			exec.submit(new Runnable() {
				@Override
				public void run() {
					try {
						Diplomacy.parse(rl.getXML(game + "/diplomacy"), diplomacy);
					} catch (Throwable t) {
						t.printStackTrace();
					} finally {
						wip.dec();
					}
				}
			});

			exec.submit(new Runnable() {
				@Override
				public void run() {
					try {
						processBattle(rl.getXML(game + "/battle"));
					} catch (Throwable t) {
						t.printStackTrace();
					} finally {
						wip.dec();
					}
				}
			});
			
			exec.submit(new Runnable() {
				@Override
				public void run() {
					try {
						talks.load(rl, game + "/talks");
					} catch (Throwable t) {
						t.printStackTrace();
					} finally {
						wip.dec();
					}
				}
			});
			exec.submit(new Runnable() {
				@Override
				public void run() {
					try {
						walks.load(rl, game + "/walks");
						
						bridge = new Bridge();
						processBridge(rl, game + "/bridge");
					} catch (Throwable t) {
						t.printStackTrace();
					} finally {
						wip.dec();
					}
				}
			});
			exec.submit(new Runnable() {
				@Override
				public void run() {
					try {
						buildingModel.processBuildings(rl, game + "/buildings", researches, labels, exec, wip);
					} catch (Throwable t) {
						t.printStackTrace();
					} finally {
						wip.dec();
					}
				}
			});
			exec.submit(new Runnable() {
				@Override
				public void run() {
					try {
						galaxyModel.processGalaxy(rl, game + "/galaxy", exec, wip);
					} catch (Throwable t) {
						t.printStackTrace();
					} finally {
						wip.dec();
					}
				}
			});
	
		} finally {
			wip.dec();
		}
		await(wip);
		wip.inc();
		try {
			for (final PlanetType pt : galaxyModel.planetTypes.values()) {
				for (int i = pt.start; i <= pt.end; i++) {
					final int j = i;
					wip.inc();
					final String n = String.format(pt.pattern, i);
					exec.execute(new Runnable() {
						@Override
						public void run() {
							try {
								XElement map = rl.getXML(n);
								PlanetSurface ps = new PlanetSurface();
								ps.parseMap(map, galaxyModel, buildingModel);
								synchronized (pt.surfaces) {
									pt.surfaces.put(j, ps);
								}
							} catch (Throwable t) {
								System.err.println(n);
								t.printStackTrace();
							} finally {
								wip.dec();
							}
						}
					});
				}
			}
		} finally {
			wip.dec();
		}
		await(wip);
		
		processPlanets(rl.getXML(game + "/planets"));

		try {
			exec.shutdown();
		} finally {
			try {
				exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			} catch (InterruptedException ex) {
				
			}
		}
	}
	/**
	 * Await the port.
	 * @param wip the port
	 */
	void await(WipPort wip) {
		try {
			wip.await();
		} catch (InterruptedException ex) {
			
		}
	}
	/**
	 * Returns the current level graphics.
	 * @return the current level graphics
	 */
	public Level getCurrentLevel() {
		return bridge.levels.get(level);
	}
	/**
	 * Process the bridge definition resources.
	 * @param rl the resource locator
	 * @param data the data resource to load
	 */
	protected void processBridge(ResourceLocator rl, String data) {
		XElement root = rl.getXML(data);
		XElement graphics = root.childElement("graphics");
		for (XElement level : graphics.childrenWithName("level")) {
			Bridge.Level lvl = new Bridge.Level();
			lvl.id = Integer.parseInt(level.get("id"));
			lvl.image = rl.getImage(level.get("image"));
			lvl.ship = walks.ships.get(level.get("ship-id"));
			lvl.walk = lvl.ship.positions.get("*bridge");
			XElement mp = level.childElement("message-panel");
			
			XElement mpAppear = mp.childElement("appear");
			lvl.messageAppear.video = mpAppear.get("video");
			lvl.messageAppear.audio = mpAppear.get("audio");
			
			XElement mpOpen = mp.childElement("open");
			lvl.messageOpen.video = mpOpen.get("video");
			lvl.messageOpen.audio = mpOpen.get("audio");
			
			XElement mpClose = mp.childElement("close");
			lvl.messageClose.video = mpClose.get("video");
			lvl.messageClose.audio = mpClose.get("audio");
			
			XElement mpButtons = mp.childElement("buttons");
			String up = mpButtons.get("up");
			lvl.up[0] = rl.getImage(up);
			lvl.up[0] = rl.getImage(up + "_pressed");
			lvl.up[0] = rl.getImage(up + "_empty");
			String down = mpButtons.get("down");
			lvl.down[0] = rl.getImage(down);
			lvl.down[0] = rl.getImage(down + "_pressed");
			lvl.down[0] = rl.getImage(down + "_empty");
			String send = mpButtons.get("send");
			lvl.send[0] = rl.getImage(send);
			lvl.send[0] = rl.getImage(send + "_pressed");
			String receive = mpButtons.get("receive");
			lvl.receive[0] = rl.getImage(receive);
			lvl.receive[0] = rl.getImage(receive + "_pressed");
			
			XElement cp = level.childElement("comm-panel");
			XElement cpOpen = cp.childElement("open");
			lvl.projectorOpen.video = cpOpen.get("video");
			lvl.projectorOpen.audio = cpOpen.get("audio");
			
			XElement cpClose = cp.childElement("close");
			lvl.projectorClose.video = cpClose.get("video");
			lvl.projectorClose.audio = cpClose.get("audio");
			bridge.levels.put(lvl.id, lvl);
		}
		XElement messages = root.childElement("messages");
		XElement send = messages.childElement("send");
		for (XElement message : send.childrenWithName("message")) {
			Bridge.Message msg = new Bridge.Message();
			msg.id = message.get("id");
			msg.media = message.get("media");
			msg.title = message.get("title");
			msg.description = message.get("description");
			bridge.sendMessages.add(msg);
		}
		XElement receive = messages.childElement("receive");
		for (XElement message : receive.childrenWithName("message")) {
			Bridge.Message msg = new Bridge.Message();
			msg.id = message.get("id");
			msg.media = message.get("media");
			msg.title = message.get("title");
			msg.description = message.get("description");
			bridge.receiveMessages.add(msg);
		}
	}
	/**
	 * @return the ship for the current level
	 */
	public WalkShip getShip() {
		return getCurrentLevel().ship;
	}
	/**
	 * Process the players XML.
	 * @param xplayers the players node
	 */
	public void processPlayers(XElement xplayers) {
		Map<Fleet, Integer> deferredFleets = JavaUtils.newHashMap();
		
		for (XElement xplayer : xplayers.childrenWithName("player")) {
			Player p = new Player();
			p.id = xplayer.get("id");
			p.color = (int)Long.parseLong(xplayer.get("color"), 16);
			p.race = xplayer.get("race");
			p.name = labels.get(xplayer.get("name"));
			p.shortName = labels.get(xplayer.get("name") + ".short");
			
			p.money = xplayer.getLong("money");
			p.initialStance = xplayer.getInt("initial-stance");
			
			p.fleetIcon = rl.getImage(xplayer.get("icon"));
			String pic = xplayer.get("picture");
			if (pic != null) {
				p.picture = rl.getImage(pic);
			}
			
			if ("true".equals(xplayer.get("user", "false"))) {
				this.player = p;
			}
			
			if ("true".equals(xplayer.get("nodiplomacy", "false"))) {
				p.noDiplomacy = true;
			}
			if ("true".equals(xplayer.get("nodatabase", "false"))) {
				p.noDatabase = true;
			}
			
			String aim = xplayer.get("ai", "");
			if (aim.length() > 0) {
				p.aiMode = AIMode.valueOf(aim);
			}
			String rat = xplayer.get("ratios", "");
			if (rat.length() > 0) {
				String[] rts = rat.split("\\s*,\\s*");
				if (rts.length == 3) {
					double r1 = Double.parseDouble(rts[0]);
					double r2 = Double.parseDouble(rts[1]);
					double r3 = Double.parseDouble(rts[2]);
					double sum = r1 + r2 + r3;
					p.aiDefensiveRatio = r1 / sum;
					p.aiOffensiveRatio = r2 / sum;
				}
			}
			
			for (XElement xinventory : xplayer.childrenWithName("inventory")) {
				String rid = xinventory.get("id");
				ResearchType rt = researches.get(rid);
				if (rt == null) {
					System.err.printf("Missing research %s for player %s%n", rid, player.id);
				} else {
					p.setAvailable(rt);
					p.changeInventoryCount(rt, xinventory.getInt("count"));
				}
			}
			setTechAvailability(xplayer, p);
			
			setFleets(deferredFleets, xplayer, p);
			
			this.players.put(p.id, p);
			for (ResearchType rt : researches.values()) {
				if (rt.race.contains(p.race) && rt.level == 0) {
					p.setAvailable(rt);
				}
			}
		}
		linkDeferredFleetTargets(deferredFleets);
	}
	/**
	 * Process the planets listing XML.
	 * @param xplanets the planets node
	 */
	public void processPlanets(XElement xplanets) {
		for (XElement xplanet : xplanets.childrenWithName("planet")) {
			Planet p = new Planet();
			p.id = xplanet.get("id");
			p.name = xplanet.get("name");
			String nameLabel = xplanet.get("label", null);
			if (nameLabel != null) {
				p.name = labels.get(nameLabel); 
			}
			p.owner = players.get(xplanet.get("owner", null));
			p.race = xplanet.get("race");
			p.x = Integer.parseInt(xplanet.get("x"));
			p.y = Integer.parseInt(xplanet.get("y"));
			
			p.diameter = Integer.parseInt(xplanet.get("size"));
			p.population = Integer.parseInt(xplanet.get("population"));
			
			p.allocation = ResourceAllocationStrategy.valueOf(xplanet.get("allocation"));
			p.autoBuild = AutoBuild.valueOf(xplanet.get("autobuild"));
			p.tax = TaxLevel.valueOf(xplanet.get("tax"));
			p.rotationDirection = RotationDirection.valueOf(xplanet.get("rotate"));
			p.morale = Integer.parseInt(xplanet.get("morale"));
			p.taxIncome = Integer.parseInt(xplanet.get("tax-income"));
			p.tradeIncome = Integer.parseInt(xplanet.get("trade-income"));
			
			String populationDelta = xplanet.get("population-last", null);
			if (populationDelta != null && !populationDelta.isEmpty()) {
				p.lastPopulation = Integer.parseInt(populationDelta);
			} else {
				p.lastPopulation = p.population;
			}
			String lastMorale = xplanet.get("morale-last", null);
			if (lastMorale != null && !lastMorale.isEmpty()) {
				p.lastMorale = Integer.parseInt(lastMorale);
			} else {
				p.lastMorale = p.morale;
			}
			
			XElement surface = xplanet.childElement("surface");
			String si = surface.get("id");
			String st = surface.get("type");
			p.type = galaxyModel.planetTypes.get(st);
			p.surface = p.type.surfaces.get(Integer.parseInt(si)).copy();
			p.surface.parseMap(xplanet, null, buildingModel);
			
			for (XElement xinv : xplanet.childElement("inventory").childrenWithName("item")) {
				InventoryItem ii = new InventoryItem();
				ii.type = researches.get(xinv.get("id"));
				ii.count = xinv.getInt("count");
				ii.hp = xinv.getInt("hp", ii.type.productionCost);
				ii.shield = xinv.getInt("shield", ii.shieldMax());
				ii.owner = players.get(xinv.get("owner"));
				p.inventory.add(ii);
			}
			
			this.planets.put(p.id, p);

			if (p.owner != null) {
				p.owner.planets.put(p, PlanetKnowledge.BUILDING);
			}
		}
	}
	/**
	 * Process a tech XML.
	 * @param tech the root node of the tech XML
	 */
	public void processResearches(XElement tech) {
		for (XElement item : tech.childrenWithName("item")) {
			processResearch(item);
		}

	}
	/**
	 * Process a research/technology node.
	 * @param item the <code>item</code> node
	 */
	public void processResearch(XElement item) {
		ResearchType tech = getResearch(item.get("id"));
		
		tech.category = ResearchSubCategory.valueOf(item.get("category"));
		
		tech.name = labels.get(item.get("name"));
		tech.longName = labels.get(item.get("long-name"));
		tech.description = labels.get(item.get("description"));
		
		String image = item.get("image");
		
		tech.image = rl.getImage(image);
		tech.infoImage = rl.getImage(image + "_large", true);
		tech.infoImageWired = rl.getImage(image + "_wired_large", true);
		
		tech.factory = item.get("factory");
		tech.race.addAll(Arrays.asList(item.get("race").split("\\s*,\\s*")));
		tech.productionCost = Integer.parseInt(item.get("production-cost"));
		tech.researchCost = Integer.parseInt(item.get("research-cost"));
		tech.level = Integer.parseInt(item.get("level"));
		
		tech.civilLab = item.getInt("civil", 0);
		tech.mechLab = item.getInt("mech", 0);
		tech.compLab = item.getInt("comp", 0);
		tech.aiLab = item.getInt("ai", 0);
		tech.milLab = item.getInt("mil", 0);
		
		String prereqs = item.get("requires", null);
		if (prereqs != null) {
			for (String si : prereqs.split("\\s*,\\s*")) {
				tech.prerequisites.add(getResearch(si));
			}
		}
		
		for (XElement slot : item.childrenWithName("slot")) {
			EquipmentSlot s = new EquipmentSlot();
			s.id = slot.get("id");
			s.x = slot.getInt("x");
			s.y = slot.getInt("y");
			s.width = slot.getInt("width");
			s.height = slot.getInt("height");
			s.max = slot.getInt("max");
			
			for (String si : slot.get("items").split("\\s*,\\s*")) {
				s.items.add(getResearch(si));
			}
			
			tech.slots.put(s.id, s);
		}
		for (XElement slotFixed : item.childrenWithName("slot-fixed")) {
			tech.fixedSlots.put(getResearch(slotFixed.get("item")), slotFixed.getInt("count"));
		}
		for (XElement prop : item.childrenWithName("property")) {
			tech.properties.put(prop.get("name"), prop.get("value"));
		}
		
		tech.equipmentImage = rl.getImage(image + "_tiny", true);
		tech.equipmentCustomizeImage = rl.getImage(image + "_small", true);
		if (tech.equipmentCustomizeImage == null) {
			tech.equipmentCustomizeImage = rl.getImage(image + "_huge", true);
		}
		tech.index = item.getInt("index");
		tech.video = item.get("video", null);
	}
	/**
	 * Retrieve or create a research type.
	 * @param id the id
	 * @return the research type
	 */
	ResearchType getResearch(String id) {
		ResearchType tech = researches.get(id);
		if (tech == null) {
			tech = new ResearchType();
			tech.id = id;
			researches.put(id, tech);
		}
		return tech;
	}
	/**
	 * List the available building types for the given player.
	 * @param player the player
	 * @param planet the target planet
	 * @return the list of available building types
	 */
	public List<BuildingType> listBuildings(Player player, Planet planet) {
		List<BuildingType> result = new ArrayList<BuildingType>();
		
		for (BuildingType bt : buildingModel.buildings.values()) {
			if (bt.tileset.containsKey(planet.isPopulated() ? planet.race : player.race)) {
				if (bt.research == null || (planet.owner != player)
						|| (player.isAvailable(bt.research) || bt.research.level == 0)) {
					result.add(bt);
				}
			}
		}
		
		return result;
	}
	/**
	 * List the available building types for the current player for the current planet.
	 * @return the list of available building types
	 */
	public List<BuildingType> listBuildings() {
		return listBuildings(player, 
				player.currentPlanet);
	}
	/**
	 * Returns true if all prerequisites of the given research type have been met.
	 * If a research is available, it will result as false
	 * @param rt the research type
	 * @return true
	 */
	public boolean canResearch(ResearchType rt) {
		if (!player.isAvailable(rt)) {
			if (rt.level <= level) {
				for (ResearchType rt0 : rt.prerequisites) {
					if (!player.isAvailable(rt0)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
	/**
	 * Can the research be shown in listings?
	 * @param rt the research type
	 * @return true if display
	 */
	public boolean canDisplayResearch(ResearchType rt) {
		return rt.race.contains(player.race) && (player.isAvailable(rt) || rt.level <= level);
	}
	/**
	 * Get the research color for the given research type.
	 * @param rt the research type
	 * @return the color
	 */
	public int getResearchColor(ResearchType rt) {
		int c = TextRenderer.GRAY;
		if (player.isAvailable(rt)) {
			c = TextRenderer.ORANGE;
		} else
		if (player.research.containsKey(rt)) {
			c = TextRenderer.YELLOW;
		} else
		if (canResearch(rt)) {
			c = TextRenderer.GREEN;
		}
		return c;
	}
	/**
	 * @return Returns an ordered list of the research types.
	 */
	public List<ResearchType> getResearch() {
		List<ResearchType> res = new ArrayList<ResearchType>();
		for (ResearchType rt0 : researches.values()) {
			if (canDisplayResearch(rt0)) {
				res.add(rt0);
			}
		}
		Collections.sort(res, new Comparator<ResearchType>() {
			@Override
			public int compare(ResearchType o1, ResearchType o2) {
				int c = o1.category.main.ordinal() - o2.category.main.ordinal();
				if (c == 0) {
					c = o1.category.ordinal() - o2.category.ordinal();
					if (c == 0) {
						c = o1.index - o2.index;
					}
				}
				return c;
			}
		});
		return res;
	}
	/** 
	 * Select the given research and its building type if any.
	 * @param rt the non-null research type
	 */
	public void selectResearch(ResearchType rt) {
		player.currentResearch(rt);
		if (rt.category.main == ResearchMainCategory.BUILDINS) {
			// select the appropriate building type
			for (BuildingType bt : buildingModel.buildings.values()) {
				if (bt.research == rt) {
					player.currentBuilding = bt;
					break;
				}
			}
		}
	}
	/**
	 * Derive a short state for the loading screen.
	 * @param worldSave the full world state.
	 * @return the short state
	 */
	public static XElement deriveShortWorldState(XElement worldSave) {
		XElement sstate = new XElement("world-short");
		sstate.set("level", worldSave.get("level"));
		sstate.set("difficulty", worldSave.get("difficulty"));
		sstate.set("time", worldSave.get("time"));
		String pid = worldSave.get("player");
		for (XElement pl : worldSave.childrenWithName("player")) {
			if (pid.equals(pl.get("id"))) {
				sstate.set("money", pl.get("money"));
				break;
			}
		}

		return sstate;
	}
	/**
	 * Save the world state.
	 * @return the world state as XElement tree.
	 */
	public XElement saveState() {
		XElement world = new XElement("world");

		world.set("level", level);
		world.set("game", name);
		world.set("player", player.id);
		world.set("difficulty", difficulty);
		world.set("time", dateFormat.get().format(time.getTime()));
		
		statistics.save(world.add("statistics"));
		
		XElement test = world.add("test");
		for (TestQuestion tq : this.test.values()) {
			for (TestAnswer ta : tq.answers) {
				if (ta.selected) {
					XElement testentry = test.add("q-a");
					testentry.set("question", tq.id);
					testentry.set("answer", ta.id);
				}
			}
		}
		
		for (Player p : players.values()) {
			XElement xp = world.add("player");
			xp.set("id", p.id);
			xp.set("money", p.money);
			xp.set("planet", p.currentPlanet != null ? p.currentPlanet.id : null);
			xp.set("fleet", p.currentFleet != null ? p.currentFleet.id : null);
			xp.set("building", p.currentBuilding != null ? p.currentBuilding.id : null);
			xp.set("research", p.currentResearch() != null ? p.currentResearch().id : null);
			xp.set("running", p.runningResearch != null ? p.runningResearch.id : null);
			xp.set("mode", p.selectionMode);
			
			p.statistics.save(xp.add("statistics"));

			if (p.knownPlayers.size() > 0) {
				XElement stances = xp.add("stance");
				for (Map.Entry<Player, Integer> se : p.knownPlayers.entrySet()) {
					XElement st1 = stances.add("with");
					st1.set("player", se.getKey().id);
					st1.set("value", se.getValue());
				}
			}
			if (p.messageQueue.size() > 0) {
				XElement xqueue = xp.add("message-queue");
				for (Message msg : p.messageQueue) {
					XElement xmessage = xqueue.add("message");
					msg.save(xmessage, dateFormat.get());
				}
			}
			if (p.messageHistory.size() > 0) {
				XElement xqueue = xp.add("message-history");
				for (Message msg : p.messageHistory) {
					XElement xmessage = xqueue.add("message");
					msg.save(xmessage, dateFormat.get());
				}
			}
			
			XElement xyesterday = xp.add("yesterday");
			xyesterday.set("build", p.yesterday.buildCost);
			xyesterday.set("repair", p.yesterday.repairCost);
			xyesterday.set("research", p.yesterday.researchCost);
			xyesterday.set("production", p.yesterday.productionCost);
			xyesterday.set("tax", p.yesterday.taxIncome);
			xyesterday.set("trade", p.yesterday.tradeIncome);
			xyesterday.set("morale", p.yesterday.taxMorale);
			xyesterday.set("count", p.yesterday.taxMoraleCount);
			
			XElement xtoday = xp.add("today");
			xtoday.set("build", p.today.buildCost);
			xtoday.set("repair", p.today.repairCost);
			xtoday.set("research", p.today.researchCost);
			xtoday.set("production", p.today.productionCost);
			
			for (Map.Entry<ResearchMainCategory, Map<ResearchType, Production>> prods : p.production.entrySet()) {
				if (prods.getValue().size() > 0) {
					XElement xprod = xp.add("production");
					xprod.set("category", prods.getKey());
					for (Map.Entry<ResearchType, Production> pe : prods.getValue().entrySet()) {
						XElement xproditem = xprod.add("line");
						xproditem.set("id", pe.getKey().id);
						xproditem.set("count", pe.getValue().count);
						xproditem.set("priority", pe.getValue().priority);
						xproditem.set("progress", pe.getValue().progress);
					}
				}
			}
			for (Map.Entry<ResearchType, Research> res : p.research.entrySet()) {
				XElement xres = xp.add("research");
				xres.set("id", res.getKey().id);
				xres.set("assigned", res.getValue().assignedMoney);
				xres.set("remaining", res.getValue().remainingMoney);
			}
			
			XElement res = xp.add("available");
			for (Map.Entry<ResearchType, List<ResearchType>> ae : p.available().entrySet()) {
				XElement av = res.add("type");
				av.set("id", ae.getKey().id);
				if (ae.getValue().size() > 0) {
					StringBuilder sb = new StringBuilder();
					for (ResearchType aert : ae.getValue()) {
						if (sb.length() > 0) {
							sb.append(", ");
						}
						sb.append(aert.id);
					}
					
					av.set("list", sb.toString());
				}
			}
			for (Map.Entry<Fleet, FleetKnowledge> fl : p.fleets.entrySet()) {
				Fleet f = fl.getKey();
				if (f.owner == p && !f.inventory.isEmpty()) {
					XElement xfleet = xp.add("fleet");
					xfleet.set("id", f.id);
					xfleet.set("x", f.x);
					xfleet.set("y", f.y);
					xfleet.set("name", f.name);
					if (f.targetFleet != null) {
						xfleet.set("target-fleet", f.targetFleet.id);
					} else
					if (f.targetPlanet != null) {
						xfleet.set("target-planet", f.targetPlanet.id);
					}
					xfleet.set("mode", f.mode);
					if (f.waypoints.size() > 0) {
						StringBuilder wp = new StringBuilder();
						for (Point2D.Float pt : f.waypoints) {
							if (wp.length() > 0) {
								wp.append(" ");
							}
							wp.append(pt.x).append(";").append(pt.y);
						}
						xfleet.set("waypoints", wp.toString());
					}
					
					for (InventoryItem fii : f.inventory) {
						XElement xfii = xfleet.add("item");
						xfii.set("id", fii.type.id);
						xfii.set("count", fii.count);
						xfii.set("hp", fii.hp);
						xfii.set("shield", fii.shield);
						for (InventorySlot fis : fii.slots) {
							XElement xfs = xfii.add("slot");
							xfs.set("id", fis.slot.id);
							if (fis.type != null) {
								xfs.set("type", fis.type.id);
								xfs.set("count", fis.count);
								xfs.set("hp", fis.hp);
							}
						}
					}
				}
			}
			// save discovered planets only
			StringBuilder sb1 = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			for (Map.Entry<Planet, PlanetKnowledge> pk : p.planets.entrySet()) {
				if (pk.getKey().owner != p) {
					if (pk.getValue().ordinal() >= PlanetKnowledge.NAME.ordinal()) {
						if (sb2.length() > 0) {
							sb2.append(", ");
						}
						sb2.append(pk.getKey().id);
					} else {
						if (sb1.length() > 0) {
							sb1.append(", ");
						}
						sb1.append(pk.getKey().id);
					}
				}
			}
			xp.set("discovered", sb1.toString());
			xp.set("discovered-named", sb2.toString());
			
			for (Map.Entry<ResearchType, Integer> inv : p.inventory.entrySet()) {
				XElement xinv = xp.add("inventory");
				xinv.set("id", inv.getKey().id);
				xinv.set("count", inv.getValue());
			}
		}
		
		for (Planet p : planets.values()) {
			if (p.owner != null) {
				XElement xp = world.add("planet");
				xp.set("id", p.id);
				xp.set("owner", p.owner.id);
				xp.set("race", p.race);
				xp.set("quarantine", p.quarantine);
				xp.set("allocation", p.allocation);
				xp.set("tax", p.tax);
				xp.set("morale", p.morale);
				xp.set("morale-last", p.lastMorale);
				xp.set("population", p.population);
				xp.set("population-last", p.lastPopulation);
				xp.set("autobuild", p.autoBuild);
				xp.set("tax-income", p.taxIncome);
				xp.set("trade-income", p.tradeIncome);
				xp.set("earthquake-ttl", p.earthQuakeTTL);
				
				for (InventoryItem pii : p.inventory) {
					XElement xpii = xp.add("item");
					xpii.set("id", pii.type.id);
					xpii.set("owner", pii.owner.id);
					xpii.set("count", pii.count);
					xpii.set("hp", pii.hp);
					xpii.set("shield", pii.shield);
					Integer ttl = p.timeToLive.get(pii); 
					if (ttl != null && ttl > 0) {
						xpii.set("ttl", ttl);
					}
				}
				for (Building b : p.surface.buildings) {
					XElement xb = xp.add("building");
					xb.set("x", b.location.x);
					xb.set("y", b.location.y);
					xb.set("id", b.type.id);
					xb.set("tech", b.techId);
					xb.set("enabled", b.enabled);
					xb.set("repairing", b.repairing);
					xb.set("hp", b.hitpoints);
					xb.set("build", b.buildProgress);
					xb.set("level", b.upgradeLevel);
					xb.set("energy", b.assignedEnergy);
					xb.set("worker", b.assignedWorker);
				}
			}
		}
		
		return world;
	}
	/**
	 * Load the world state.
	 * @param xworld the world XElement
	 */
	public void loadState(XElement xworld) {
		difficulty = Difficulty.valueOf(xworld.get("difficulty"));
		level = xworld.getInt("level");
		fleetIdSequence = 0;
		
		try {
			time.setTime(dateFormat.get().parse(xworld.get("time")));
			time.set(GregorianCalendar.MINUTE, (time.get(GregorianCalendar.MINUTE) / 10) * 10);
			time.set(GregorianCalendar.SECOND, 0);
			time.set(GregorianCalendar.MILLISECOND, 0);
		} catch (ParseException ex) {
			ex.printStackTrace();
		}
		
		player = players.get(xworld.get("player"));
		
		XElement stats = xworld.childElement("statistics");
		if (stats != null) {
			statistics.load(stats);
		}

		XElement test = xworld.childElement("test");
		if (test != null) {
			for (XElement qa : test.childrenWithName("q-a")) {
				TestQuestion tq = this.test.get(qa.get("question"));
				tq.choose(qa.get("answer"));
			}
		}
		
		Map<Player, XElement[]> deferredMessages = new HashMap<Player, XElement[]>();
		/** The deferred fleet-to-fleet targeting. */
		Map<Fleet, Integer> deferredTargets = new HashMap<Fleet, Integer>();
		
		for (XElement xplayer : xworld.childrenWithName("player")) {
			Player p = players.get(xplayer.get("id"));
			
			p.money = xplayer.getLong("money");
			p.currentPlanet = planets.get(xplayer.get("planet", null));
			
			p.currentBuilding = buildingModel.buildings.get(xplayer.get("building", null));
			p.currentResearch(researches.get(xplayer.get("research", null)));
			p.runningResearch = researches.get(xplayer.get("running", null));
			p.selectionMode = SelectionMode.valueOf(xplayer.get("mode", SelectionMode.PLANET.toString()));
			
			XElement xyesterday = xplayer.childElement("yesterday");
			
			p.yesterday.buildCost = xyesterday.getInt("build");
			p.yesterday.repairCost = xyesterday.getInt("repair");
			p.yesterday.researchCost = xyesterday.getInt("research");
			p.yesterday.productionCost = xyesterday.getInt("production");
			p.yesterday.taxIncome = xyesterday.getInt("tax");
			p.yesterday.tradeIncome = xyesterday.getInt("trade");
			p.yesterday.taxMorale = xyesterday.getInt("morale");
			p.yesterday.taxMoraleCount = xyesterday.getInt("count");
			
			XElement xtoday = xplayer.childElement("today");
			p.today.buildCost = xtoday.getInt("build");
			p.today.repairCost = xtoday.getInt("repair");
			p.today.researchCost = xtoday.getInt("research");
			p.today.productionCost = xtoday.getInt("production");

			for (Map<ResearchType, Production> prod : p.production.values()) {
				prod.clear();
			}
			
			XElement pstats = xplayer.childElement("statistics");
			if (pstats != null) {
				p.statistics.load(pstats);
			}
			
			XElement xstance = xplayer.childElement("stance");
			if (xstance != null) {
				for (XElement xwith : xstance.childrenWithName("with")) {
					Player pl = players.get(xwith.get("player"));
					if (pl != null) {
						p.setStance(pl, xwith.getInt("value"));
					} else {
						throw new AssertionError("Missing player for stance " + p.name + " vs. " + xwith.get("player"));
					}
				}
			}

			for (XElement xprod : xplayer.childrenWithName("production")) {
				ResearchMainCategory cat = ResearchMainCategory.valueOf(xprod.get("category"));
				Map<ResearchType, Production> prod = new LinkedHashMap<ResearchType, Production>();
				for (XElement xline : xprod.childrenWithName("line")) {
					ResearchType rt = researches.get(xline.get("id"));
					Production pr = new Production();
					pr.type = rt;
					pr.count = xline.getInt("count");
					pr.priority = xline.getInt("priority");
					pr.progress = xline.getInt("progress");
					prod.put(rt, pr);
				}
				p.production.put(cat, prod);
			}
			p.research.clear();
			for (XElement xres : xplayer.childrenWithName("research")) {
				ResearchType rt = researches.get(xres.get("id"));
				if (rt == null) {
					throw new IllegalArgumentException("research technology not found: " + xres.get("id"));
				}
				Research rs = new Research();
				rs.type = rt;
				rs.state = rt == p.currentResearch() ? ResearchState.RUNNING : ResearchState.STOPPED; 
				rs.assignedMoney = xres.getInt("assigned");
				rs.remainingMoney = xres.getInt("remaining");
				p.research.put(rt, rs);
			}
			
			// add free technologies
			p.available().clear();
			for (ResearchType rt : researches.values()) {
				if (rt.level == 0 && rt.race.equals(p.race)) {
					p.add(rt);
				}
			}
			
			setTechAvailability(xplayer, p);
			
			setFleets(deferredTargets, xplayer, p);
			
			p.planets.clear();
			for (String pl : xplayer.get("discovered").split("\\s*,\\s*")) {
				if (pl.length() > 0) {
					Planet p0 = planets.get(pl);
					if (p0 == null) {
						throw new IllegalArgumentException("discovered planet not found: " + pl);
					}
					p.planets.put(p0, PlanetKnowledge.VISIBLE);
				}
			}
			for (String pl : xplayer.get("discovered-named", "").split("\\s*,\\s*")) {
				if (pl.length() > 0) {
					Planet p0 = planets.get(pl);
					if (p0 == null) {
						throw new IllegalArgumentException("discovered-named planet not found: " + pl);
					}
					p.planets.put(p0, PlanetKnowledge.NAME);
				}
			}
			p.inventory.clear();
			for (XElement xinv : xplayer.childrenWithName("inventory")) {
				p.inventory.put(researches.get(xinv.get("id")), xinv.getInt("count"));
			}
			p.currentFleet = null;
			int currentFleet = xplayer.getInt("fleet", -1);
			if (currentFleet >= 0) {
				for (Fleet f : p.fleets.keySet()) {
					if (f.id == currentFleet) {
						p.currentFleet = f;
						break;
					}
				}
			}
			XElement xqueue = xplayer.childElement("message-queue");
			XElement xhistory = xplayer.childElement("message-history");
			deferredMessages.put(p, new XElement[] { xqueue, xhistory });
		}
		Set<String> allPlanets = new HashSet<String>(planets.keySet());
		for (XElement xplanet : xworld.childrenWithName("planet")) {
			Planet p = planets.get(xplanet.get("id"));

			p.owner = players.get(xplanet.get("owner"));
			p.race = xplanet.get("race");
			p.quarantine = "true".equals(xplanet.get("quarantine"));
			p.allocation = ResourceAllocationStrategy.valueOf(xplanet.get("allocation"));
			p.tax = TaxLevel.valueOf(xplanet.get("tax"));
			p.morale = xplanet.getInt("morale");
			p.lastMorale = xplanet.getInt("morale-last", p.morale);
			p.population = xplanet.getInt("population");
			p.lastPopulation = xplanet.getInt("population-last", p.population);
			p.autoBuild = AutoBuild.valueOf(xplanet.get("autobuild"));
			p.taxIncome = xplanet.getInt("tax-income");
			p.tradeIncome = xplanet.getInt("trade-income");
			p.earthQuakeTTL = xplanet.getInt("earthquake-ttl", 0);

			p.inventory.clear();
			p.surface.buildings.clear();
			p.surface.buildingmap.clear();

			for (XElement xpii : xplanet.childrenWithName("item")) {
				InventoryItem pii = new InventoryItem();
				pii.owner = players.get(xpii.get("owner"));
				pii.type = researches.get(xpii.get("id"));
				pii.count = xpii.getInt("count");
				pii.hp = xpii.getInt("hp", pii.type.productionCost);
				pii.shield = xpii.getInt("shield", pii.shieldMax());
				
				int ttl = xpii.getInt("ttl", 0);
				if (ttl > 0) {
					p.timeToLive.put(pii, ttl);
				} else
				// set TTL to satellites which have been deployed prior the TTL introduction.
				if (pii.owner != p.owner) {
					if (pii.type.id.equals("Satellite")) {
						p.timeToLive.put(pii, 12 * 6);
					} else
					if (pii.type.id.equals("SpySatellite1")) {
						p.timeToLive.put(pii, 24 * 6);
					} else
					if (pii.type.id.equals("SpySatellite2")) {
						p.timeToLive.put(pii, 96 * 6);
					}
				}
				
				p.inventory.add(pii);
			}

			p.surface.setBuildings(buildingModel, xplanet);
			
			if (p.owner != null) {
				p.owner.planets.put(p, PlanetKnowledge.BUILDING);
			}
			
			allPlanets.remove(p.id);
		}
		for (String rest : allPlanets) {
			Planet p = planets.get(rest);
			p.die();
		}
		fleetIdSequence++;
		for (Map.Entry<Player, XElement[]> e : deferredMessages.entrySet()) {
			if (e.getValue()[0] != null) {
				e.getKey().messageQueue.clear();
				for (XElement xmessage : e.getValue()[0].childrenWithName("message")) {
					Message msg = new Message();
					msg.load(xmessage, this);
					e.getKey().messageQueue.add(msg);
				}
			}
			if (e.getValue()[1] != null) {
				e.getKey().messageHistory.clear();
				for (XElement xmessage : e.getValue()[1].childrenWithName("message")) {
					Message msg = new Message();
					msg.load(xmessage, this);
					e.getKey().messageHistory.add(msg);
				}
			}
		}
		linkDeferredFleetTargets(deferredTargets);
	}
	/**
	 * Link the fleet's targetFleet value with the fleet given by the ID.
	 * @param deferredTargets the map from source fleet to target fleet ID
	 */
	private void linkDeferredFleetTargets(Map<Fleet, Integer> deferredTargets) {
		for (Map.Entry<Fleet, Integer> ft : deferredTargets.entrySet()) {
			outer:
			for (Player p : players.values()) {
				for (Fleet f : p.ownFleets()) {
					if (f.id == ft.getValue().intValue()) {
						ft.getKey().targetFleet = f;
						break outer;
					}
				}
			}
		}
	}
	/**
	 * Set the fleets from the given player definition into the player object.
	 * @param deferredTargets the deferred targets if a fleet targets another
	 * @param xplayer the source definition
	 * @param p the target player object
	 */
	private void setFleets(Map<Fleet, Integer> deferredTargets,
			XElement xplayer, Player p) {
		p.fleets.clear();
		for (XElement xfleet : xplayer.childrenWithName("fleet")) {
			Fleet f = new Fleet();
			f.owner = p;
			f.id = xfleet.getInt("id", -1);
			// if no id automatically assign a new sequence
			boolean noTargetFleet = false;
			if (f.id < 0) {
				f.id = fleetIdSequence++;
				noTargetFleet = true; // ignore target fleet in this case
			}
			fleetIdSequence = Math.max(fleetIdSequence, f.id);
			
			f.x = xfleet.getFloat("x");
			f.y = xfleet.getFloat("y");
			f.name = xfleet.get("name");
			
			String s0 = xfleet.get("target-fleet", null);
			if (s0 != null && !noTargetFleet) {
				deferredTargets.put(f, Integer.parseInt(s0));
			}
			s0 = xfleet.get("target-planet", null);
			if (s0 != null) {
				f.targetPlanet = planets.get(s0);
			}
			s0 = xfleet.get("mode", null);
			if (s0 != null) {
				f.mode = FleetMode.valueOf(s0);
			}
			s0 = xfleet.get("waypoints", null);
			if (s0 != null) {
				for (String wp : s0.split("\\s+")) {
					String[] xy = wp.split(";");
					f.waypoints.add(new Point2D.Float(Float.parseFloat(xy[0]), Float.parseFloat(xy[1])));
				}
			}
			
			for (XElement xfii : xfleet.childrenWithName("item")) {
				InventoryItem fii = new InventoryItem();
				fii.type = researches.get(xfii.get("id"));
				fii.count = xfii.getInt("count");
				fii.shield = xfii.getInt("shield");
				fii.hp = xfii.getInt("hp");
				for (XElement xfis : xfii.childrenWithName("slot")) {
					InventorySlot fis = new InventorySlot();
					fis.slot = fii.type.slots.get(xfis.get("id"));
					fis.type = researches.get(xfis.get("type", null));
					if (fis.type != null) {
						fis.count = xfis.getInt("count");
						fis.hp = xfis.getInt("hp");
					}
					fii.slots.add(fis);
				}
				f.inventory.add(fii);
			}
			if (!f.inventory.isEmpty()) {
				p.fleets.put(f, FleetKnowledge.FULL);
			}
		}
	}
	/**
	 * Set the available technologies for the given player.
	 * @param xplayer the player definition
	 * @param p the player object to load
	 */
	private void setTechAvailability(XElement xplayer, Player p) {
		XElement xavail0 = xplayer.childElement("available");
		if (xavail0 != null) {
			for (XElement xavail : xavail0.childrenWithName("type")) {
				ResearchType rt = researches.get(xavail.get("id"));
				if (rt == null) {
					throw new IllegalArgumentException("available technology not found: " + xavail);
				}
				p.add(rt);
				
				for (String liste : xavail.get("list", "").split("\\s*,\\s*")) {
					if (liste.length() > 0) {
						ResearchType rt0 = researches.get(liste);
						if (rt0 == null) {
							throw new IllegalArgumentException("available technology not found: " + liste + " in " + xavail);
						}
						p.availableLevel(rt).add(rt0);
					}
				}
			}
		}
	}
	/** @return Return the list of other important items. */
	public String getOtherItems() {
		StringBuilder os = new StringBuilder();
		for (InventoryItem pii : player.currentPlanet.inventory) {
			if (pii.owner == player && pii.type.category == ResearchSubCategory.SPACESHIPS_SATELLITES) {
				if (os.length() > 0) {
					os.append(", ");
				}
				os.append(pii.type.name);
			} else
			if (pii.type.category == ResearchSubCategory.SPACESHIPS_STATIONS
			&& (pii.owner == player || player.knowledge(player.currentPlanet, PlanetKnowledge.BUILDING) >= 0)) {
				if (os.length() > 0) {
					os.append(", ");
				}
				os.append(pii.type.name);
			}
		}
		return os.toString();
	}
	/**
	 * Locate a fleet with the given ID.
	 * @param id the fleet unique id
	 * @return the fleet or null if not found
	 */
	public Fleet findFleet(int id) {
		for (Player p : players.values()) {
			for (Fleet f : p.fleets.keySet()) {
				if (f.id == id) {
					return f;
				}
			}
		}
		return null;
	}
	/**
	 * Construct a new message without target and only the label.
	 * @param text the label identifier
	 * @return the message
	 */
	public Message newMessage(String text) {
		Message message = new Message();
		message.gametime = time.getTimeInMillis();
		message.timestamp = System.currentTimeMillis();
		message.text = text;
		return message;
	}
	/** @return Compute the maximum test points. */
	public int testMax() {
		int sum = 0;
		for (TestQuestion tq : test.values()) {
			int max = 0;
			for (TestAnswer ta : tq.answers) {
				max = Math.max(max, ta.points);
			}
			sum += max;
		}
		return sum;
	}
	/** @return compute the test score based on the user selections. */
	public int testScore() {
		int sum = 0;
		for (TestQuestion tq : test.values()) {
			for (TestAnswer ta : tq.answers) {
				if (ta.selected) {
					sum += ta.points;
				}
			}
		}
		return sum;
		
	}
	/**
	 * Process the battle definition XML.
	 * @param xbattle the battle definition
	 */
	void processBattle(XElement xbattle) {
		for (XElement xturret : xbattle.childElement("buildings").childrenWithName("building-turret")) {

			int nx = xturret.getInt("width");
			int ny = xturret.getInt("height");
			String id = xturret.get("id");
			
			BufferedImage m = rl.getImage(xturret.get("matrix"));
			BufferedImage[][] matrix = ImageUtils.split(m, m.getWidth() / nx, m.getHeight() / ny);

			for (XElement xrace : xturret.childrenWithName("race")) {
				String rid = xrace.get("id");
				for (XElement xport : xrace.childrenWithName("port")) {
					BuildingTurret tr = new BuildingTurret();
					tr.strip = xport.getInt("strip");
					tr.dx = xport.getInt("dx");
					tr.dy = xport.getInt("dy");
					tr.matrix = matrix;
					battle.addTurret(id, rid, tr);
				}
			}
			
		}
		for (XElement xproj : xbattle.childElement("projectiles").childrenWithName("projectile")) {
			String id = xproj.get("id");
			int nx = xproj.getInt("width");
			int ny = xproj.getInt("height");
			BattleProjectile bp = new BattleProjectile();
			
			BufferedImage m = rl.getImage(xproj.get("matrix"));
			bp.matrix = ImageUtils.split(m, m.getWidth() / nx, m.getHeight() / ny);
			if (xproj.has("alternative")) {
				m = rl.getImage(xproj.get("alternative"));
				bp.alternative = ImageUtils.split(m, m.getWidth() / nx, m.getHeight() / ny);
			}
			if (xproj.has("sound")) {
				bp.sound = SoundType.valueOf(xproj.get("sound"));
			}
			bp.damage = xproj.getInt("damage");
			bp.range = xproj.getInt("range");
			bp.delay = xproj.getInt("delay");
			if (xproj.has("area")) {
				bp.area = xproj.getInt("area");
			} else {
				bp.area = 1;
			}
			bp.mode = BattleProjectile.Mode.valueOf(xproj.get("mode"));
			
			battle.projectiles.put(id, bp);
			
		}
		for (XElement xspace : xbattle.childElement("space-entities").childrenWithName("tech")) {
			String id = xspace.get("id");
			int nx = xspace.getInt("width");
			
			BattleSpaceEntity se = new BattleSpaceEntity();
			
			BufferedImage ni = rl.getImage(xspace.get("normal"));
			se.normal = ImageUtils.splitByWidth(ni, ni.getWidth() / nx);
			
			if (xspace.has("alternative")) {
				BufferedImage ai = rl.getImage(xspace.get("alternative"));
				se.alternative = ImageUtils.splitByWidth(ai, ai.getWidth() / nx);
			} else {
				se.alternative = se.normal;
			}
			se.infoImage = rl.getImage(xspace.get("image"));
			se.destruction = SoundType.valueOf(xspace.get("sound"));
			if (se.destruction == null) {
				System.err.println("Missing sound " + xspace.get("sound") + " for " + id);
			}
			if (xspace.has("movement-speed")) {
				se.movementSpeed = xspace.getInt("movement-speed");
			}
			if (xspace.has("rotation-speed")) {
				se.rotationSpeed = xspace.getInt("rotation-speed");
			}
			
			battle.spaceEntities.put(id, se);
		}
		for (XElement xdefense : xbattle.childElement("ground-projectors").childrenWithName("tech")) {
			String id = xdefense.get("id");
			int nx = xdefense.getInt("width");

			BattleGroundProjector se = new BattleGroundProjector();
			
			BufferedImage ni = rl.getImage(xdefense.get("normal"));
			se.normal = ImageUtils.splitByWidth(ni, ni.getWidth() / nx);
			
			if (xdefense.has("alternative")) {
				BufferedImage ai = rl.getImage(xdefense.get("alternative"));
				se.alternative = ImageUtils.splitByWidth(ai, ai.getWidth() / nx);
			} else {
				se.alternative = se.normal;
			}
			se.image = rl.getImage(xdefense.get("image"));
			if (xdefense.has("sound")) {
				se.destruction = SoundType.valueOf(xdefense.get("sound"));
			} else {
				System.err.println("Missing sound for " + id);
			}
			se.projectile = xdefense.get("projectile");
			
			battle.groundProjectors.put(id, se);
		}
		for (XElement xdefense : xbattle.childElement("ground-shields").childrenWithName("tech")) {
			String id = xdefense.get("id");

			BattleGroundShield se = new BattleGroundShield();
			
			BufferedImage ni = rl.getImage(xdefense.get("normal"));
			se.normal = ni;
			
			if (xdefense.has("alternative")) {
				BufferedImage ai = rl.getImage(xdefense.get("alternative"));
				se.alternative = ai;
			} else {
				se.alternative = se.normal;
			}
			se.infoImage = rl.getImage(xdefense.get("image"));
			if (xdefense.has("sound")) {
				se.destruction = SoundType.valueOf(xdefense.get("sound"));
			} else {
				System.err.println("Missing sound for " + id);
			}
			se.shields = xdefense.getInt("shield");
			
			battle.groundShields.put(id, se);
		}
		for (XElement xground : xbattle.childElement("ground-vehicles").childrenWithName("tech")) {
			String id = xground.get("id");
			int nx = xground.getInt("width");
			int ny = xground.getInt("height");
			BattleGroundEntity ge = new BattleGroundEntity();

			BufferedImage ni = rl.getImage(xground.get("normal"));
			ge.normal = ImageUtils.split(ni, ni.getWidth() / nx, ni.getHeight() / ny);
			
			if (xground.has("alternative")) {
				BufferedImage ai = rl.getImage(xground.get("alternative"));
				ge.alternative = ImageUtils.split(ai, ai.getWidth() / nx, ai.getWidth() / ny);
			} else {
				ge.alternative = ge.normal;
			}
			ge.destroy = SoundType.valueOf(xground.get("destroy"));
			if (xground.has("fire")) {
				ge.fire = SoundType.valueOf(xground.get("fire"));
			}
			
			battle.groundEntities.put(id, ge);
		}
	}
	/**
	 * Compute the distance square between two points.
	 * @param x1 the first X
	 * @param y1 the first Y
	 * @param x2 the second X
	 * @param y2 the second Y
	 * @return the distance square
	 */
	public static double dist(double x1, double y1, double x2, double y2) {
		return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
	}
}

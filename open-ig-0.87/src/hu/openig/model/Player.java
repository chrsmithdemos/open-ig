/*
 * Copyright 2008-2011, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.model;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The object describing the player's status and associated
 * objects.
 * @author akarnokd, 2009.10.25.
 */
public class Player {
	/** The player id. */
	public String id;
	/** The player's name. */
	public String name;
	/** The coloring used for this player. */
	public int color;
	/** The fleet icon. */
	public BufferedImage fleetIcon;
	/** The picture used in the database screen. */
	public BufferedImage picture;
	/** The race of the player. Determines the technology tree to be used. */
	public String race;
	/** The in-progress production list. */
	public final Map<ResearchMainCategory, Map<ResearchType, Production>> production = new HashMap<ResearchMainCategory, Map<ResearchType, Production>>();
	{
		for (ResearchMainCategory cat : ResearchMainCategory.values()) {
			production.put(cat, new LinkedHashMap<ResearchType, Production>());
		}
	}
	/** The in-progress research. */
	public final Map<ResearchType, Research> research = new HashMap<ResearchType, Research>();
	/** The completed research. */
	public final Set<ResearchType> availableResearch = new HashSet<ResearchType>();
	/** The discovered players. */
	public final List<Player> discoveredPlayers = new ArrayList<Player>();
	/** The fleets owned. */
	public final Map<Fleet, FleetKnowledge> fleets = new HashMap<Fleet, FleetKnowledge>();
	/** The planets owned. */
	public final Map<Planet, PlanetKnowledge> planets = new HashMap<Planet, PlanetKnowledge>();
	/** The inventory counts. */
	public final Map<ResearchType, Integer> inventory = new HashMap<ResearchType, Integer>();
	/** The current running research. */
	public ResearchType runningResearch;
	/** The actual planet. */
	public Planet currentPlanet;
	/** The actual fleet. */
	public Fleet currentFleet;
	/** The actual research. */
	public ResearchType currentResearch;
	/** The actual building. */
	public BuildingType currentBuilding;
	/** The type of the last selected thing: planet or fleet. */
	public SelectionMode selectionMode;
	/** The current money amount. */
	public int money;
	/** The player's finance status at a particular day. */
	public class PlayerFinances {
		/** The production cost. */
		public int productionCost;
		/** The research cost. */
		public int researchCost;
		/** The build cost. */
		public int buildCost;
		/** The repair cost. */
		public int repairCost;
		/** The tax income. */
		public int taxIncome;
		/** The trade income. */
		public int tradeIncome;
		/** The average tax morale. */
		public int taxMorale;
		/** The tax morale count. */
		public int taxMoraleCount;
		/**
		 * Assign a different instance values.
		 * @param other the other instance
		 */
		public void assign(PlayerFinances other) {
			productionCost = other.productionCost;
			researchCost = other.researchCost;
			buildCost = other.buildCost;
			repairCost = other.repairCost;
			taxIncome = other.taxIncome;
			tradeIncome = other.tradeIncome;
			taxMorale = other.taxMorale;
		}
		/** Clear values. */
		public void clear() {
			productionCost = 0;
			researchCost = 0;
			buildCost = 0;
			repairCost = 0;
			taxIncome = 0;
			tradeIncome = 0;
		}
	}
	/** The global financial information yesterday. */
	public final PlayerFinances yesterday = new PlayerFinances();
	/** The global finalcial information today. */
	public final PlayerFinances today = new PlayerFinances();
	/**
	 * @return returns the next planet by goind top-bottom relative to the current planet
	 */
	public Planet moveNextPlanet() {
		List<Planet> playerPlanets = getPlayerPlanets();
		if (playerPlanets.size() > 0) {
			Collections.sort(playerPlanets, PLANET_ORDER);
			int idx = playerPlanets.indexOf(currentPlanet);
			Planet p = playerPlanets.get((idx + 1) % playerPlanets.size());
			currentPlanet = p;
			return p;
		}
		return null;
	}
	/**
	 * @return returns the previous planet by goind top-bottom relative to the current planet
	 */
	public Planet movePrevPlanet() {
		List<Planet> playerPlanets = getPlayerPlanets();
		if (playerPlanets.size() > 0) {
			Collections.sort(playerPlanets, PLANET_ORDER);
			int idx = playerPlanets.indexOf(currentPlanet);
			if (idx == 0) {
				idx = playerPlanets.size(); 
			}
			Planet p = playerPlanets.get((idx - 1) % playerPlanets.size());
			currentPlanet = p;
			return p;
		}
		return null;
	}
	/**
	 * @return a list of the player-owned planets
	 */
	public List<Planet> getPlayerPlanets() {
		List<Planet> result = new ArrayList<Planet>();
		for (Planet p : planets.keySet()) {
			if (p.owner == this) {
				result.add(p);
			}
		}
		return result;
	}
	/** The planet orderer by coordinates. */
	public static final Comparator<Planet> PLANET_ORDER = new Comparator<Planet>() {
		@Override
		public int compare(Planet o1, Planet o2) {
			int c = o1.y < o2.y ? -1 : (o1.y > o2.y ? 1 : 0);
			if (c == 0) {
				c = o1.x < o2.x ? -1 : (o1.x > o2.x ? 1 : 0);
			}
			return c;
		}
	};
	/** The planet order by name. */
	public static final Comparator<Planet> NAME_ORDER = new Comparator<Planet>() {
		@Override
		public int compare(Planet o1, Planet o2) {
			return o1.name.compareTo(o2.name);
		}
	};
	/**
	 * @return the number of built buildings per type
	 */
	public Map<BuildingType, Integer> countBuildings() {
		Map<BuildingType, Integer> result = new HashMap<BuildingType, Integer>();
		for (Planet p : planets.keySet()) {
			if (p.owner == this) {
				for (Building b : p.surface.buildings) {
					Integer cnt = result.get(b.type);
					result.put(b.type, cnt != null ? cnt + 1 : 1);
				}
			}
		}
		return result;
	}
	/** @return the global planet statistics. */
	public PlanetStatistics getPlanetStatistics() {
		PlanetStatistics ps = new PlanetStatistics();
		for (Planet p : planets.keySet()) {
			if (p.owner == this) {
				ps.add(p.getStatistics());
			}
		}
		return ps;
	}
	/**
	 * Test if the given research is available.
	 * @param rt the research
	 * @return true if available
	 */
	public boolean isAvailable(ResearchType rt) {
		return availableResearch.contains(rt);
	}
	/**
	 * Is there enough labs to research the technology? Does
	 * not consider the operational state of the labs.
	 * @param rt the technology
	 * @return true if there are at least the required lab
	 */
	public boolean hasEnoughLabs(ResearchType rt) {
		PlanetStatistics ps = getPlanetStatistics();
		if (ps.civilLab < rt.civilLab) {
			return false;
		}
		if (ps.mechLab < rt.mechLab) {
			return false;
		}
		if (ps.compLab < rt.compLab) {
			return false;
		}
		if (ps.aiLab < rt.aiLab) {
			return false;
		}
		if (ps.milLab < rt.milLab) {
			return false;
		}
		
		return true;
	}
	/**
	 * Is there enough active labs to research the technology?
	 * @param rt the technology
	 * @return true if there are at least the required lab
	 */
	public boolean hasEnoughActiveLabs(ResearchType rt) {
		PlanetStatistics ps = getPlanetStatistics();
		if (ps.civilLabActive < rt.civilLab) {
			return false;
		}
		if (ps.mechLabActive < rt.mechLab) {
			return false;
		}
		if (ps.compLabActive < rt.compLab) {
			return false;
		}
		if (ps.aiLabActive < rt.aiLab) {
			return false;
		}
		if (ps.milLabActive < rt.milLab) {
			return false;
		}
		
		return true;
	}
}

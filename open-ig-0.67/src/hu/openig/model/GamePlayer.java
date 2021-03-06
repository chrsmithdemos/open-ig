/*
 * Copyright 2008-2009, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.model;

import java.util.HashSet;
import java.util.Set;

/**
 * The players object representing all its knowledge.
 * @author karnokd, 2009.05.11.
 * @version $Revision 1.0$
 */
public class GamePlayer {
	/** The player's name. */
	public String name;
	/** The player type. */
	public PlayerType playerType;
	/** The race of the player. */
	public GameRace race;
	/** The player's money amount. */
	public long money;
	/** The icon index used to display the player's fleets. */
	public int fleetIcon;
	/** The currently selected planet. */
	public GamePlanet selectedPlanet;
	/** The currently selected fleet. */
	public GameFleet selectedFleet;
	/** The type currently selected game object on the starmap. */
	public StarmapSelection selectionType;
	/**
	 * Set of known planets, which can be displayed on the starmap for the player.
	 * (Should) contain all entries of knownPlanetsByName and ownPlanets
	 */
	public final Set<GamePlanet> knownPlanets = new HashSet<GamePlanet>();
	/**
	 * Known planets by name.
	 * (Should) contain all entries of ownPlanets.
	 */
	public final Set<GamePlanet> knownPlanetsByName = new HashSet<GamePlanet>();
	/**
	 * Set of own planets.
	 */
	public final Set<GamePlanet> ownPlanets = new HashSet<GamePlanet>();
	/** Set of own fleets. */
	public final Set<GameFleet> ownFleets = new HashSet<GameFleet>();
	/** Set of known fleets. (Should) contain the ownFleets too. */
	public final Set<GameFleet> knownFleets = new HashSet<GameFleet>();
	/**
	 * Adds the planet to the own planets set and subsequently
	 * calls knowPlanet. Players can lose posession of a planet
	 * with the losePlanet() method, but the planet will be still
	 * known and known by name.
	 * @param planet the planet to posess, cannot be null
	 */
	public void possessPlanet(GamePlanet planet) {
		ownPlanets.add(planet);
		knowPlanetByName(planet);
	}
	/**
	 * Adds the planet to the known planets by name set and subsequently calls
	 * the knowPlanet method.
	 * @param planet the planet to know by name, cannot be null
	 */
	public void knowPlanetByName(GamePlanet planet) {
		if (planet == null) {
			throw new NullPointerException("planet");
		}
		knownPlanetsByName.add(planet);
		knowPlanet(planet);
	}
	/**
	 * Adds the planet to the known set of planets.
	 * @param planet the planet to know, cannot be null
	 */
	public void knowPlanet(GamePlanet planet) {
		if (planet == null) {
			throw new NullPointerException("planet");
		}
		knownPlanets.add(planet);
	}
	/**
	 * Loose a planet ownership by removing it
	 * from the ownPlanets set.
	 * @param planet the planet to loose, cannot be null
	 */
	public void loosePlanet(GamePlanet planet) {
		if (planet == null) {
			throw new NullPointerException("planet");
		}
		if (!ownPlanets.remove(planet)) {
			throw new IllegalStateException("Planet " + planet.id + " was not own by player ");
		}
	}
	/**
	 * Acquire a new fleet.
	 * @param fleet the fleet to aquire, cannot be null
	 */
	public void possessFleet(GameFleet fleet) {
		if (fleet == null) {
			throw new NullPointerException("fleet");
		}
		ownFleets.add(fleet);
		knowFleet(fleet);
	}
	/**
	 * Know a new fleet.
	 * @param fleet the fleet to aquire, cannot be null
	 */
	public void knowFleet(GameFleet fleet) {
		if (fleet == null) {
			throw new NullPointerException("fleet");
		}
		knownFleets.add(fleet);
	}
	/**
	 * Loose a fleet.
	 * @param fleet the fleet to aquire, cannot be null
	 */
	public void looseFleet(GameFleet fleet) {
		if (fleet == null) {
			throw new NullPointerException("fleet");
		}
		ownFleets.remove(fleet);
		unknowFleet(fleet);
	}
	/**
	 * Unknow a fleet, e.g. loose sight of a fleet.
	 * @param fleet the fleet to aquire, cannot be null
	 */
	public void unknowFleet(GameFleet fleet) {
		if (fleet == null) {
			throw new NullPointerException("fleet");
		}
		knownFleets.remove(fleet);
	}
}

/*
 * Copyright 2008-2012, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Record to store the planet wide statistics used by the information screens.
 * @author akarnokd
 */
public class PlanetStatistics {
	/** The available houses. */
	public int houseAvailable;
	/** The worker demand. */
	public int workerDemand;
	/** The available hospitals. */
	public int hospitalAvailable;
	/** The available food. */
	public int foodAvailable;
	/** The available police. */
	public int policeAvailable;
	/** The energy demand. */
	public int energyDemand;
	/** The available energy. */
	public int energyAvailable;
	/** The civil engineering laboratory active count. */
	public int civilLabActive;
	/** The civil engineering laboratory. */
	public int civilLab;
	/** The mechanical engineering laboratory active count. */
	public int mechLabActive;
	/** The mechanical engineering laboratory. */
	public int mechLab;
	/** The computer laboratory active count. */
	public int compLabActive;
	/** The computer laboratory. */
	public int compLab;
	/** The AI laboratory active count. */
	public int aiLabActive;
	/** The AI laboratory. */
	public int aiLab;
	/** The Military laboratory active count. */
	public int milLabActive;
	/** The Military laboratory active count. */
	public int milLab;
	/** The equipment factory capacity active. */
	public int equipmentActive;
	/** The equipment factory capacity. */
	public int equipment;
	/** The weapons factory capacity active. */
	public int weaponsActive;
	/** The weapons factory capacity. */
	public int weapons;
	/** The spaceship factory capacity active. */
	public int spaceshipActive;
	/** The spaceship factory capacity. */
	public int spaceship;
	/** The current list of problems. */
	public final Map<PlanetProblems, PlanetProblems> problems = new LinkedHashMap<PlanetProblems, PlanetProblems>();
	/** The current list of warnings. */
	public final Map<PlanetProblems, PlanetProblems> warnings = new LinkedHashMap<PlanetProblems, PlanetProblems>();
	/** Free repair percent. */
	public float freeRepair = 0;
	/** Free repair efficiency. */
	public float freeRepairEff = 0;
	/** Has a functioning military spaceport. */
	public boolean hasMilitarySpaceport;
	/** Has a functioning trader's spaceport. */
	public boolean hasTradersSpaceport;
	/** Number of orbital factories. */
	public int orbitalFactory;
	/** The figher count. */
	public int fighterCount;
	/** The vehicle count. */
	public int vehicleCount;
	/** The maximum vehicles. */
	public int vehicleMax;
	/** A space station is deployed. */
	public boolean hasSpaceStation;
	/** Building in progress or complete but not yet resource-allocated. */
	public boolean constructing;
	/** The total planet count. */
	public int planetCount;
	/** The total military spaceport count. */
	public int militarySpaceportCount;
	/** The worker demand of all built buildings, whether they are enabled or not. */
	public int nativeWorkerDemand;
	/**
	 * Add the other planet statistics to this one.
	 * @param other the other statistics
	 */
	public void add(PlanetStatistics other) {
		planetCount++;
		houseAvailable += other.houseAvailable;
		workerDemand += other.workerDemand;
		hospitalAvailable += other.hospitalAvailable;
		foodAvailable += other.foodAvailable;
		policeAvailable += other.policeAvailable;
		energyDemand += other.energyDemand;
		energyAvailable += other.energyAvailable;
		civilLabActive += other.civilLabActive;
		civilLab += other.civilLab;
		mechLabActive += other.mechLabActive;
		mechLab += other.mechLab;
		compLabActive += other.compLabActive;
		compLab += other.compLab;
		aiLabActive += other.aiLabActive;
		aiLab += other.aiLab;
		milLabActive += other.milLabActive;
		milLab += other.milLab;
		equipmentActive += other.equipmentActive;
		equipment += other.equipment;
		weaponsActive += other.weaponsActive;
		weapons += other.weapons;
		spaceshipActive += other.spaceshipActive;
		spaceship += other.spaceship;
		orbitalFactory += other.orbitalFactory;
		problems.putAll(other.problems);
		warnings.putAll(other.warnings);
		constructing |= other.constructing;
		hasMilitarySpaceport |= other.hasMilitarySpaceport;
		hasSpaceStation |= other.hasSpaceStation;
		hasTradersSpaceport |= other.hasTradersSpaceport;
		militarySpaceportCount += other.militarySpaceportCount;
		nativeWorkerDemand += other.nativeWorkerDemand;
	}
	/**
	 * The planet has the specified problem?
	 * @param probl the problem
	 * @return present
	 */
	public boolean hasProblem(PlanetProblems probl) {
		return problems.containsKey(probl);
	}
	/** 
	 * Add the given planet problem to the map.
	 * @param probl the planet problem
	 */
	public void addProblem(PlanetProblems probl) {
		problems.put(probl, probl);
	}
	/**
	 * The planet has the specified problem?
	 * @param probl the problem
	 * @return present
	 */
	public boolean hasWarning(PlanetProblems probl) {
		return warnings.containsKey(probl);
	}
	/** 
	 * Add the given planet problem to the map.
	 * @param probl the planet problem
	 */
	public void addWarning(PlanetProblems probl) {
		warnings.put(probl, probl);
	}
	/**
	 * @return the total number of built labs.
	 */
	public int labCount() {
		return aiLab + civilLab + compLab + mechLab + milLab;
	}
	/**
	 * @return the total number of active labs.
	 */
	public int activeLabCount() {
		return aiLabActive + civilLabActive + compLabActive + mechLabActive + milLabActive;
	}
	/**
	 * Check if building is possible on this planet.
	 * @return true if building is possible
	 */
	public boolean canBuildAnything() {
		return !hasProblem(PlanetProblems.COLONY_HUB) && !hasWarning(PlanetProblems.COLONY_HUB);
	}
	/**
	 * Check if any level of the problem exists.
	 * @param prob the problem to check
	 * @return true if problem present
	 */
	public boolean hasAnyProblem(PlanetProblems prob) {
		return hasProblem(prob) || hasWarning(prob);
	}
}

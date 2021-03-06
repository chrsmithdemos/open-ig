/*
 * Copyright 2008-2011, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.model;

import java.lang.reflect.Field;

import hu.openig.utils.XElement;

/**
 * The global world statistics.
 * @author akarnokd, Apr 9, 2011
 */
public class WorldStatistics {
	/** The seconds spent in game. */
	public long playTime;
	/** The seconds spent running the game simulation. */ 
	public long simulationTime;
	/** Total positive money income. */
	public long moneyIncome;
	/** The total trade income. */
	public long moneyTradeIncome;
	/** The total tax income. */
	public long moneyTaxIncome;
	/** The total demolish income. */
	public long moneyDemolishIncome;
	/** The income from selling equipment. */
	public long moneySellIncome;
	/** Total money spent. */
	public long moneySpent;
	/** The money spent on building. */
	public long moneyBuilding;
	/** The money spent on repairing. */
	public long moneyRepair;
	/** The money spent on research. */
	public long moneyResearch;
	/** The money spent on production. */
	public long moneyProduction;
	/** The money spent on upgrading. */
	public long moneyUpgrade;
	/** The total build count. */
	public long buildCount;
	/** The demolish count. */
	public long demolishCount;
	/** The sell count. */
	public long sellCount;
	/** The research count. */
	public long researchCount;
	/** The production count. */
	public long productionCount;
	/** The upgrade count. */
	public long upgradeCount;
	/** Total buildings. */
	public long totalBuilding;
	/** Total working buildings. */
	public long totalAvailableBuilding;
	/** The total population. */
	public long totalPopulation;
	/** Total available house. */
	public long totalAvailableHouse;
	/** Total energy demand. */
	public long totalEnergyDemand;
	/** Total worker demand. */
	public long totalWorkerDemand;
	/** Total available energy. */
	public long totalAvailableEnergy;
	/** Total available food. */
	public long totalAvailableFood;
	/** Total available hospital. */
	public long totalAvailableHospital;
	/** Total available police. */
	public long totalAvailablePolice;
	/**
	 * Save the statistics.
	 * @param target the target XElement
	 */
	public void save(XElement target) {
		for (Field f : getClass().getFields()) {
			try {
				target.set(f.getName(), f.get(this));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * Load the statistics.
	 * @param source the source
	 */
	public void load(XElement source) {
		for (Field f : getClass().getFields()) {
			try {
				String s = source.get(f.getName(), null);
				if (f.getType() == Integer.TYPE) {
					if (s != null) {
						f.set(this, Integer.parseInt(s));
					} else {
						f.set(this, 0);
					}
				} else
				if (f.getType() == Long.TYPE) {
					if (s != null) {
						f.set(this, Long.parseLong(s));
					} else {
						f.set(this, 0L);
					}
				} else
				if (f.getType() == Float.TYPE) {
					if (s != null) {
						f.set(this, Float.parseFloat(s));
					} else {
						f.set(this, 0f);
					}
				} else
				if (f.getType() == Double.TYPE) {
					if (s != null) {
						f.set(this, Double.parseDouble(s));
					} else {
						f.set(this, 0d);
					}
				} else
				if (f.getType() == Boolean.TYPE) {
					if (s != null) {
						f.set(this, "true".equals(s));
					} else {
						f.set(this, false);
					}
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
}

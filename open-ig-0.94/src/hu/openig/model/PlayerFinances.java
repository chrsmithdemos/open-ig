/*
 * Copyright 2008-2012, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.model;

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
		taxMoraleCount = 0;
	}
}

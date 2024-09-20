package com.advancedraidtracker.ui.dpsanalysis;

import lombok.Value;

@Value
public class SetupInfo
{
	int attackStab;
	int attackSlash;
	int attackCrush;
	int attackMagic;
	int attackRanged;
	int defenseStab;
	int defenseSlash;
	int defenseCrush;
	int defenseMagic;
	int defenseRanged;
	int strength;
	int rangeStrength;
	int magicDamagePercent;
	int prayerBonus;
	int attackSpeed;
	String style;
	int undead;
	int slayer;
}

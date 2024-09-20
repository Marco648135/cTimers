package com.advancedraidtracker.ui.dpsanalysis;

import lombok.Value;

@Value
public class EquipmentData
{
	String name;
	int id;
	String version;
	String slot;
	String image;
	int speed;
	String category;
	EquipmentBonuses bonuses;
	EquipmentStats offensive;
	EquipmentStats defensive;
	boolean isTwoHanded;
}

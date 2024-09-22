package com.advancedraidtracker.ui.dpsanalysis;

import lombok.Setter;
import lombok.Value;

@Value
@Setter
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

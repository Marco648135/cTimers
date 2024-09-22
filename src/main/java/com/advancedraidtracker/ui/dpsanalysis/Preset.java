package com.advancedraidtracker.ui.dpsanalysis;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Preset {
	private String name;
	private Map<String, EquipmentData> equipment;
	private Map<Prayers, Boolean> prayers;
	private String selectedStyle;

	// New fields
	private Map<String, Integer> baseLevels; // Attack, Strength, Ranged, Magic
	private Map<String, String> selectedPotions; // Potion selected for each skill
	private String selectedAttackType;
	private boolean visible;

	// Constructor
	public Preset(
		String name,
		Map<String, EquipmentData> equipment,
		Map<Prayers, Boolean> prayers,
		String selectedStyle,
		Map<String, Integer> baseLevels,
		Map<String, String> selectedPotions,
		String selectedAttackType
	) {
		this.name = name;
		this.equipment = equipment;
		this.prayers = prayers;
		this.selectedStyle = selectedStyle;
		this.baseLevels = baseLevels;
		this.selectedPotions = selectedPotions;
		this.selectedAttackType = selectedAttackType;
	}

	public boolean isMeleeVoid()
	{
		return(equipment.get("head").getName().contains("Void ranger helm")
			&& (equipment.get("body").getName().contains("Elite void top") || equipment.get("body").getName().contains("Void knight top"))
			&& (equipment.get("legs").getName().contains("Elite void robe") || equipment.get("legs").getName().contains("Void knight robe"))
			&& equipment.get("hands").getName().contains("Void knight gloves"));
	}

	public boolean isRangeVoid()
	{
		return(equipment.get("head").getName().contains("Void ranger helm")
			&& equipment.get("body").getName().contains("Void knight top")
			&& equipment.get("legs").getName().contains("Void knight robe")
			&& equipment.get("hands").getName().contains("Void knight gloves"));
	}

	public boolean isRangeEVoid()
	{
		return(equipment.get("head").getName().contains("Void ranger helm")
		&& equipment.get("body").getName().contains("Elite void top")
		&& equipment.get("legs").getName().contains("Elite void robe")
		&& equipment.get("hands").getName().contains("Void knight gloves"));
	}

	public boolean isSalve()
	{
		return false;
	}

	public boolean isSalveE()
	{
		return false;
	}

	public boolean isSlayer()
	{
		return false;
	}
}
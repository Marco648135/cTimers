package com.advancedraidtracker.ui.dpsanalysis;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Preset
{
	private String name;
	private Map<String, EquipmentData> equipment;
	private Map<Prayers, Boolean> prayers;
	private String selectedStyle;

	public Preset(String name, Map<String, EquipmentData> equipment, Map<Prayers, Boolean> prayers, String selectedStyle) {
		this.name = name;
		this.equipment = equipment;
		this.prayers = prayers;
		this.selectedStyle = selectedStyle;
	}
}
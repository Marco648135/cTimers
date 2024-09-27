package com.advancedraidtracker.utility;

import java.util.EnumMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Prayer;

@Setter
@Getter
public class PlayerData
{
	private int ringId;
	private int attackLevel;
	private int strengthLevel;
	private final Map<Prayer, Boolean> prayers;

	public PlayerData()
	{
		this.ringId = -1; // Default value indicating no ring equipped
		this.attackLevel = -1;
		this.strengthLevel = -1;
		this.prayers = new EnumMap<>(Prayer.class);
	}

	public void setPrayerState(Prayer prayer, boolean isActive)
	{
		prayers.put(prayer, isActive);
	}
}
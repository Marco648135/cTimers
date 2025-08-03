package com.advancedraidtracker.ui.charts.chartelements;

import java.awt.*;

import static com.advancedraidtracker.constants.TobIDs.*;

public class SoulflameOutlineBox
{
    public int spawnTick;
    public String owner;
	public int duration;

    public SoulflameOutlineBox(String owner, int spawnTick)
    {
		this(owner, spawnTick,  6);
    }

	public SoulflameOutlineBox(String owner, int spawnTick, int duration)
	{
		this.spawnTick = spawnTick;
		this.owner = owner;
		this.duration = duration;
	}

    public Color getColor()
    {
		//return new Color(200, 240, 0);
		return new Color(111, 4, 249);
    }

	@Override
	public String toString()
	{
		String thrallString;
		thrallString = "Soulflame Buff (";
		thrallString += owner;
		thrallString += ", " + spawnTick + "->" + (spawnTick + duration) + ")";
		return thrallString;
	}
}

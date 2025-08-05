package com.advancedraidtracker.ui.charts.chartelements;

import java.awt.*;

import static com.advancedraidtracker.constants.TobIDs.*;

public class SoulflameOutlineBox
{
    public int spawnTick;
    public String owner;
	public int duration;
	public String room;

    public SoulflameOutlineBox(String owner, int spawnTick, String room)
    {
		this(owner, spawnTick, 6, room);
    }

	public SoulflameOutlineBox(String owner, int spawnTick, int duration, String room)
	{
		this.spawnTick = spawnTick;
		this.owner = owner;
		this.duration = duration;
		this.room = room;
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
		thrallString += ", " + room + ", " + spawnTick + "->" + (spawnTick + duration) + ")";
		return thrallString;
	}
}

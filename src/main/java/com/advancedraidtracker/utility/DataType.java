package com.advancedraidtracker.utility;

public enum DataType
{
	RING("Ring"),
	ATTACK("Attack"),
	STRENGTH("Strength"),
	PRAYER("Prayer");

	public final String name;
	DataType(String name)
	{
		this.name = name;
	}
}

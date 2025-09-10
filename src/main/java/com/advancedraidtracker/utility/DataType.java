package com.advancedraidtracker.utility;

public enum DataType
{
	RING("Ring"),
	ATTACK("Attack"),
	STRENGTH("Strength"),
	PRAYER("Prayer"),
	MAGIC("Magic");

	public final String name;
	DataType(String name)
	{
		this.name = name;
	}

	public static DataType fromName(String s)
	{
		for(DataType dataType : values())
		{
			if(dataType.name.equals(s))
			{
				return dataType;
			}
		}
		return null;
	}
}

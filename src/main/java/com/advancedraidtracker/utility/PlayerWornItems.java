package com.advancedraidtracker.utility;

import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;

public class PlayerWornItems
{
	public int helmet = 0;
	public int cape = 0;
	public int amulet = 0;
	public int weapon = 0;
	public int torso = 0;
	public int shield = 0;
	public int legs = 0;
	public int gloves = 0;
	public int boots = 0;
	private final ItemManager itemManager;

	public PlayerWornItems(String s, ItemManager itemManager)
	{
		this.itemManager = itemManager;
		String[] items = s.split("~");
		if (items.length == 9)
		{
			helmet = parseOrDefault(items[0]);
			cape = parseOrDefault(items[1]);
			amulet = parseOrDefault(items[2]);
			weapon = parseOrDefault(items[3]);
			torso = parseOrDefault(items[4]);
			shield = parseOrDefault(items[5]);
			legs = parseOrDefault(items[6]);
			gloves = parseOrDefault(items[7]);
			boots = parseOrDefault(items[8]);
		}
	}

	private static int parseOrDefault(String s)
	{
		if (s == null || s.isEmpty())
		{
			return -1;
		}
		else
		{
			return Integer.parseInt(s);
		}
	}

	public static String getStringFromComposition(PlayerComposition pc)
	{
		return pc.getEquipmentId(KitType.HEAD) + "~"
			+ pc.getEquipmentId(KitType.CAPE) + "~"
			+ pc.getEquipmentId(KitType.AMULET) + "~"
			+ pc.getEquipmentId(KitType.WEAPON) + "~"
			+ pc.getEquipmentId(KitType.TORSO) + "~"
			+ pc.getEquipmentId(KitType.SHIELD) + "~"
			+ pc.getEquipmentId(KitType.LEGS) + "~"
			+ pc.getEquipmentId(KitType.HANDS) + "~"
			+ pc.getEquipmentId(KitType.BOOTS);
	}

	public String[] getAll()
	{
		if (helmet + cape + amulet + weapon + torso + shield + legs + gloves + boots == 0)
		{
			return new String[]{};
		}
		else
		{
			return new String[]
				{
					"Helmet: " + getItemName(helmet),
					"Cape: " + getItemName(cape),
					"Amulet: " + getItemName(amulet),
					"Weapon: " + getItemName(weapon),
					"Torso: " + getItemName(torso),
					"Shield: " + getItemName(shield),
					"Legs: " + getItemName(legs),
					"Gloves: " + getItemName(gloves),
					"Boots: " + getItemName(boots),
				};
		}
	}

	public String getItemName(int id)
	{
		if (id == -1)
		{
			return "None";
		}
		else
		{
			return itemManager.getItemComposition(id).getName();
		}
	}
}

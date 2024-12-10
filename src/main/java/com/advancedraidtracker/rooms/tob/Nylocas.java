package com.advancedraidtracker.rooms.tob;

import java.util.List;
import lombok.Value;
import net.runelite.api.NPC;

@Value
public class Nylocas
{
	NPC nylo;
	int originTick;
	String description;

	public static boolean isMatch(Nylocas nylocas, NPC npc)
	{
		if(nylocas.nylo == npc)
		{
			return true;
		}
		return false;
	}

	public static Nylocas getNylocas(List<Nylocas> nylocasList, NPC npc)
	{
		for(Nylocas nylocas : nylocasList)
		{
			if(isMatch(nylocas, npc))
			{
				return nylocas;
			}
		}
		return null;
	}
}

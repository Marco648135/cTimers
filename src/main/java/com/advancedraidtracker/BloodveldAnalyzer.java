package com.advancedraidtracker;

import java.util.List;
import java.util.Objects;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcSpawned;

public class BloodveldAnalyzer
{
	private Client client;
	public BloodveldAnalyzer(Client client)
	{
		this.client = client;
	}

	public void npcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		int index = npc.getIndex();
	}

	public void animationChanged(AnimationChanged event)
	{
		if(event.getActor() instanceof Player && event.getActor().getName().equals(client.getLocalPlayer()))
		{
			if(event.getActor().getAnimation() == 8056) //player attacked with a scythe
			{
				List<NPC> npcs = client.getNpcs();
				Actor targetActor = event.getActor().getInteracting();
				if(targetActor instanceof NPC)
				{
					NPC target = (NPC) targetActor;
					WorldPoint targetLocation = target.getWorldLocation();
					int x = targetLocation.getRegionX();
					int y = targetLocation.getRegionY();
					for(NPC npc : npcs)
					{
						if(Objects.requireNonNull(npc.getName()).contains("Bloodveld") && !npc.isDead() && npc.getWorldLocation().distanceTo(targetLocation) < 10)
						{

						}
					}
				}
			}
		}
	}

	public void hitsplatApplied(HitsplatApplied event)
	{
		int gameTick = client.getTickCount();
		if(Objects.requireNonNull(event.getActor().getName()).contains("Bloodveld"))
		{

		}
	}
}

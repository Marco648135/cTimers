package com.advancedraidtracker.ui.advancedstatistics;

import com.advancedraidtracker.constants.RaidRoom;
import com.advancedraidtracker.utility.PlayerAttack;
import com.advancedraidtracker.utility.weapons.AnimationDecider;
import com.advancedraidtracker.utility.weapons.PlayerAnimation;
import com.advancedraidtracker.utility.wrappers.PlayerDidAttack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Value;

public class AdvancedData
{
	@Getter
	List<PlayerAttack> playerAttacks = new ArrayList<>();
	public void addAttack(RaidRoom currentRoom, PlayerDidAttack attack)
	{
		PlayerAnimation playerAnimation = AnimationDecider.getWeapon(attack.animation, attack.spotAnims, attack.projectile, attack.weapon);
		playerAttacks.add(new PlayerAttack(playerAnimation, attack.player, attack.tick, currentRoom));
		refresh();
	}

	public List<PlayerAttack> getRoomAttacks(RaidRoom room)
	{
		List<PlayerAttack> attacks = new ArrayList<>();
		for(PlayerAttack playerAttack : playerAttacks)
		{
			if(playerAttack.getRoom().equals(room))
			{
				attacks.add(playerAttack);
			}
		}
		return attacks;
	}

	class StallEvent
	{
		private int waveStalled;
		private int roomTick;
		private Map<String, Integer> ageMap;

		public StallEvent(int waveStalled, int roomTick)
		{
			this.waveStalled = waveStalled;
			this.roomTick = roomTick;
			this.ageMap = new HashMap<>();
		}

		public int getWaveStalled() { return waveStalled; }
		public int getRoomTick() { return roomTick; }
		public Map<String, Integer> getAgeMap() { return ageMap; }
	}

	@Value
	class KilledNylo
	{
		int roomTick;
		int age;
		String description;
		public int getDeathTick()
		{
			return roomTick + 52 - age;
		}
	}



	@Getter
	private List<StallEvent> stalls;
	@Getter
	private List<KilledNylo> killedNylos;
	private final List<Runnable> refreshListeners;

	public AdvancedData()
	{
		stalls = new ArrayList<>();
		killedNylos = new ArrayList<>();
		refreshListeners = new ArrayList<>();
	}

	public void addNyloKilled(int roomTick, int age, String description)
	{
		killedNylos.add(new KilledNylo(roomTick, age, description));
		refresh();
	}

	public void addNyloAliveAtStall(int stalledWave, int roomTick, String description, int age)
	{
		StallEvent stallEvent = null;
		for (StallEvent s : stalls)
		{
			if (s.getRoomTick() == roomTick)
			{
				stallEvent = s;
				break;
			}
		}

		if (stallEvent == null)
		{
			stallEvent = new StallEvent(stalledWave, roomTick);
			stalls.add(stallEvent);
		}

		stallEvent.getAgeMap().put(description, age);

		refresh();
	}

	public void addRefreshListener(Runnable listener)
	{
		refreshListeners.add(listener);
	}

	public void clearRefreshListeners()
	{
		refreshListeners.clear();
	}

	public void resetData()
	{
		stalls.clear();
		killedNylos.clear();
		refresh();
	}

	public void refresh()
	{
		for(Runnable listener : refreshListeners)
		{
			listener.run();
		}
	}
}

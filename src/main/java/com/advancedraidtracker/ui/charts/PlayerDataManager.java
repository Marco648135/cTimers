package com.advancedraidtracker.ui.charts;

import com.advancedraidtracker.utility.PlayerData;
import com.advancedraidtracker.utility.PlayerDataChanged;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import net.runelite.api.Prayer;

public class PlayerDataManager
{

	private final Map<String, NavigableMap<Integer, List<PlayerDataChanged>>> changesByPlayer = new HashMap<>();

	public void addPlayerDataChanged(PlayerDataChanged playerDataChanged)
	{
		String playerName = playerDataChanged.getUsername();
		int tick = playerDataChanged.getRoomTick();

		changesByPlayer
			.computeIfAbsent(playerName, k -> new TreeMap<>())
			.computeIfAbsent(tick, k -> new ArrayList<>())
			.add(playerDataChanged);
	}

	public PlayerData getPlayerData(String player, int tick)
	{
		NavigableMap<Integer, List<PlayerDataChanged>> changesByTick = changesByPlayer.get(player);

		if (changesByTick == null)
		{
			return new PlayerData();
		}

		PlayerData playerData = new PlayerData();

		NavigableMap<Integer, List<PlayerDataChanged>> changesUpToTick = changesByTick.headMap(tick, true);

		for (List<PlayerDataChanged> changesAtTick : changesUpToTick.values())
		{
			for (PlayerDataChanged change : changesAtTick)
			{
				applyChangeToPlayerData(playerData, change);
			}
		}

		return playerData;
	}

	private void applyChangeToPlayerData(PlayerData playerData, PlayerDataChanged change)
	{
		switch (change.getChangeType())
		{
			case RING:
				playerData.setRingId(change.getNewValue());
				break;
			case ATTACK:
				playerData.setAttackLevel(change.getNewValue());
				break;
			case STRENGTH:
				playerData.setStrengthLevel(change.getNewValue());
				break;
			case PRAYER:
				int prayerIdWithState = change.getNewValue();
				int stateDigit = prayerIdWithState % 10;
				int prayerId = prayerIdWithState / 10;

				Prayer prayer = Prayer.values()[prayerId];
				boolean isActive = (stateDigit == 1);

				playerData.setPrayerState(prayer, isActive);
				break;
			default:
				break;
		}
	}
}
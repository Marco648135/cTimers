package com.advancedraidtracker.ui.setups;

import java.util.List;
import net.runelite.api.ItemID;
import net.runelite.client.game.ItemManager;

public class RuneDepot extends IconGridPanel
{
	private static final List<Integer> RUNES = List.of(
		ItemID.AIR_RUNE, ItemID.MIND_RUNE, ItemID.WATER_RUNE, ItemID.EARTH_RUNE, ItemID.FIRE_RUNE,
		ItemID.BODY_RUNE, ItemID.COSMIC_RUNE, ItemID.CHAOS_RUNE, ItemID.NATURE_RUNE, ItemID.LAW_RUNE,
		ItemID.DEATH_RUNE, ItemID.SUNFIRE_RUNE, ItemID.ASTRAL_RUNE, ItemID.BLOOD_RUNE, ItemID.SOUL_RUNE,
		ItemID.WRATH_RUNE, ItemID.MIST_RUNE, ItemID.DUST_RUNE, ItemID.MUD_RUNE, ItemID.SMOKE_RUNE,
		ItemID.STEAM_RUNE, ItemID.LAVA_RUNE
	);

	public RuneDepot(ItemManager itemManager, SetupsWindow setupsWindow)
	{
		super(itemManager, setupsWindow);
		addItems(RUNES);
	}
}

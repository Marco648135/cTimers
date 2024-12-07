package com.advancedraidtracker.ui.setups;

import net.runelite.client.game.ItemManager;

public class RecentItems extends IconGridPanel
{

	public RecentItems(ItemManager itemManager, SetupsWindow setupsWindow)
	{
		super(itemManager, setupsWindow);
	}

	public void addRecentItem(int id)
	{
		if (id == -1)
		{
			return;
		}
		for (ItemEntry item : items)
		{
			if (item.id == id)
			{
				return;
			}
		}
		addItemAt(0, id);
	}

	public void clearRecentItems()
	{
		clearItems();
	}
}

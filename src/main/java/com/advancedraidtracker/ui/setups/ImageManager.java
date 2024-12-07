package com.advancedraidtracker.ui.setups;

import java.util.HashMap;
import java.util.Map;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

public class ImageManager
{
	static Map<Integer, AsyncBufferedImage> imageMap = new HashMap<>();

	public static AsyncBufferedImage getImage(ItemManager itemManager, int id)
	{
		if (imageMap.containsKey(id))
		{
			return imageMap.get(id);
		}
		else
		{
			AsyncBufferedImage image = itemManager.getImage(id);
			imageMap.put(id, image);
			return image;
		}
	}
}

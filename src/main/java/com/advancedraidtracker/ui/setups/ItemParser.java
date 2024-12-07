package com.advancedraidtracker.ui.setups;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemParser
{
	private final static Set<Integer> BYPASSED_IDS = Set.of(28307, 28310, 28313, 28316); //Rings use variants, original ID is beta world
	private static Trie itemTrie = new Trie();

	public static Trie getItemTrie()
	{
		if (itemTrie == null)
		{
			return new Trie();
		}
		else
		{
			return itemTrie;
		}
	}

	public static void processItemID()
	{
		CompletableFuture.runAsync(() -> {
			String fileUrl = "https://raw.githubusercontent.com/runelite/runelite/refs/heads/master/runelite-api/src/main/java/net/runelite/api/ItemID.java";
			List<String> lines = new ArrayList<>();
			try
			{
				URL url = new URL(fileUrl);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				int status = conn.getResponseCode();
				if (status != 200)
				{
					System.err.println("Failed to download file. HTTP status: " + status);
					return;
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String inputLine;
				while ((inputLine = in.readLine()) != null)
				{
					lines.add(inputLine);
				}
				in.close();
				conn.disconnect();

				Pattern pattern = Pattern.compile("public static final int ([A-Z0-9_]+)\\s*=\\s*(\\d+);");
				Trie trie = new Trie();
				for (String lineContent : lines)
				{
					Matcher matcher = pattern.matcher(lineContent);
					if (matcher.find())
					{
						String name = matcher.group(1);
						String idStr = matcher.group(2);
						int id = Integer.parseInt(idStr);

						String[] words = name.split("_");
						boolean discard = false;
						for (String word : words)
						{
							if (word.equals(idStr))
							{
								discard = true;
								break;
							}
						}
						if (discard && !BYPASSED_IDS.contains(id))
						{
							continue;
						}

						StringBuilder formattedName = new StringBuilder();
						for (int i = 0; i < words.length; i++)
						{
							String word = words[i].toLowerCase();
							if (!word.isEmpty())
							{
								formattedName.append(Character.toUpperCase(word.charAt(0)));
								if (word.length() > 1)
								{
									formattedName.append(word.substring(1));
								}
							}
							if (i < words.length - 1)
							{
								formattedName.append(" ");
							}
						}
						String finalName = formattedName.toString();

						trie.insert(finalName, id);
					}
				}

				itemTrie = trie;

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		});
	}
}

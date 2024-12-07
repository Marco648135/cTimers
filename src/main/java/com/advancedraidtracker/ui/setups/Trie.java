package com.advancedraidtracker.ui.setups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Trie
{
	private final TrieNode root;
	private int size;
	private final List<Entry> entries;

	public Trie()
	{
		this.root = new TrieNode();
		this.size = 0;
		this.entries = new ArrayList<>();
	}

	public void insert(String name, int id)
	{
		String lowerName = name.toLowerCase();
		String[] words = lowerName.split("\\s+");
		for (String word : words)
		{
			if (word.isEmpty())
			{
				continue;
			}
			insertWord(word, name, id);
		}
		insertWord(lowerName, name, id);
		size++;
		entries.add(new Entry(name, id));
	}

	private void insertWord(String word, String fullName, int id)
	{
		TrieNode current = root;
		for (char ch : word.toCharArray())
		{
			current = current.getChildren().computeIfAbsent(ch, c -> new TrieNode());
		}
		current.addEntry(new Entry(fullName, id));
	}


	public int getSize()
	{
		return size;
	}



	public static class TrieNode
	{
		private final Map<Character, TrieNode> children = new HashMap<>();
		private List<Entry> entries = new ArrayList<>();

		public Map<Character, TrieNode> getChildren()
		{
			return children;
		}

		public void addEntry(Entry entry)
		{
			entries.add(entry);
		}

		public List<Entry> getEntries()
		{
			return entries;
		}

		private boolean endOfWord;
		private int id;
		private String fullName;

		public String getFullName()
		{
			return fullName;
		}

		public void setFullName(String name)
		{
			this.fullName = name;
		}


		public boolean isEndOfWord()
		{
			return endOfWord;
		}

		public void setEndOfWord(boolean endOfWord)
		{
			this.endOfWord = endOfWord;
		}

		public int getId()
		{
			return id;
		}

		public void setId(int id)
		{
			this.id = id;
		}
	}

	public static class Entry
	{
		private final String name;
		private final int id;

		public Entry(String name, int id)
		{
			this.name = name;
			this.id = id;
		}

		public String getName()
		{
			return name;
		}

		public int getId()
		{
			return id;
		}

		public String toString()
		{
			return name;
		}
	}

	public List<Entry> getSuggestions(String prefix, int limit)
	{
		List<Entry> suggestions = new ArrayList<>();
		String lowerPrefix = prefix.toLowerCase();

		TrieNode current = root;
		for (char ch : lowerPrefix.toCharArray())
		{
			current = current.getChildren().get(ch);
			if (current == null)
			{
				return suggestions;
			}
		}
		collectSuggestions(current, suggestions, limit, new HashSet<>());
		return suggestions;
	}

	private void collectSuggestions(TrieNode node, List<Entry> suggestions, int limit, Set<String> seenNames)
	{
		for (Entry entry : node.getEntries())
		{
			if (seenNames.contains(entry.getName()))
			{
				continue;
			}
			suggestions.add(entry);
			seenNames.add(entry.getName());
			if (suggestions.size() >= limit)
			{
				return;
			}
		}
		for (TrieNode child : node.getChildren().values())
		{
			collectSuggestions(child, suggestions, limit, seenNames);
			if (suggestions.size() >= limit)
			{
				return;
			}
		}
	}


}
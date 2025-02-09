package com.advancedraidtracker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.GameTick;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class BloodveldAnalyzer
{
	private Client client;

	// Map to store pending attacks, keyed by the tick they occurred
	private Map<Integer, PendingAttack> pendingAttacks = new HashMap<>();

	// Map to track NPCs' last known chunks
	private Map<Integer, Chunk> npcLastChunkMap = new HashMap<>();

	// Map to maintain per-chunk NPC lists
	private Map<Chunk, LinkedHashSet<Integer>> chunkNPCMap = new HashMap<>();

	private Gson gson = new GsonBuilder().create();

	public BloodveldAnalyzer(Client client)
	{
		this.client = client;

		// Ensure the directory and file exist
		File dir = new File("C:\\bloodvelds");
		if (!dir.exists())
		{
			dir.mkdirs();
		}

		File dataFile = new File(dir, "data.log");
		if (!dataFile.exists())
		{
			try
			{
				dataFile.createNewFile();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void npcSpawned(NpcSpawned event)
	{

	}

	NPC queuedTarget = null;

	public void animationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();

		if (actor == client.getLocalPlayer())
		{
			if (actor.getAnimation() == 8056) // scythe attack
			{
				if(queuedTarget == null)
				{
					if(actor.getInteracting() instanceof NPC)
					{
						queuedTarget = (NPC) actor.getInteracting();
					}
				}
			}
		}
	}

	List<HitsplatApplied> hitsplatApplieds = new ArrayList<>();

	String hitsplatString = "";
	public void hitsplatApplied(HitsplatApplied event)
	{
		hitsplatApplieds.add(event);
	}

	public void onGameTick(GameTick e)
	{
		if(queuedTarget != null)
		{
			int currentTick = client.getTickCount();
			Player player = client.getLocalPlayer();
			int playerX = player.getWorldLocation().getRegionX();
			int playerY = player.getWorldLocation().getRegionY();
			int plane = player.getWorldLocation().getPlane();

			// Get the target NPC
			Actor targetActor = queuedTarget;
			if (targetActor != null)
			{
				NPC target = (NPC) targetActor;
				if (target.getName() != null && target.getName().contains("Bloodveld"))
				{
					// Get the target's SW tile position
					int targetX = target.getWorldLocation().getRegionX();
					int targetY = target.getWorldLocation().getRegionY();
					int targetPlane = target.getWorldLocation().getPlane();

					// Determine if arc is horizontal or vertical
					boolean isHorizontalArc = !(playerY == targetY);

					// Compute the arc tiles
					List<int[]> arcTiles = computeArcTiles(targetX, targetY, targetPlane, isHorizontalArc);

					// Now, for each bloodveld NPC, check if its SW tile or its area intersects the arc
					List<NPC> bloodveldNPCs = getBloodveldNPCsInArea(targetX, targetY, 10);

					Map<NPC, int[]> npcsWithSWTileIntersectingArc = new HashMap<>();
					Map<NPC, int[]> npcsWithAreaIntersectingArc = new HashMap<>();

					for (NPC npc : bloodveldNPCs)
					{
						int npcX = npc.getWorldLocation().getRegionX();
						int npcY = npc.getWorldLocation().getRegionY();
						int npcPlane = npc.getWorldLocation().getPlane();
						int[] npcPosition = new int[]{npcX, npcY, npcPlane};

						// Check if npc's SW tile intersects arc
						if (arcTilesContains(arcTiles, npcX, npcY))
						{
							npcsWithSWTileIntersectingArc.put(npc, npcPosition);
						}

						// Check if npc's 2x2 area intersects arc
						if (npcIntersectsArc(npcX, npcY, npcPlane, arcTiles))
						{
							npcsWithAreaIntersectingArc.put(npc, npcPosition);
						}
					}

					// Create PendingAttack instance
					PendingAttack attack = new PendingAttack();
					attack.tick = currentTick;
					attack.playerX = playerX;
					attack.playerY = playerY;
					attack.playerPlane = plane;
					attack.targetNPC = target;
					attack.arcTiles = arcTiles;
					attack.npcsWithSWTileIntersectingArc = npcsWithSWTileIntersectingArc;
					attack.npcsWithAreaIntersectingArc = npcsWithAreaIntersectingArc;

					// Store the PendingAttack, keyed by tick
					pendingAttacks.put(currentTick, attack);
				}
			}
			queuedTarget = null;
		}

		String appliedHitsplats = "";
		for(HitsplatApplied event : hitsplatApplieds)
		{
			int currentTick = client.getTickCount();
			Actor actor = event.getActor();
			if (actor instanceof NPC)
			{
				NPC npc = (NPC) actor;
				if (npc.getName() != null && npc.getName().contains("Bloodveld"))
				{
					// See if there's a pending attack from the previous tick
					PendingAttack pendingAttack = pendingAttacks.get(currentTick - 1);
					if (pendingAttack != null)
					{
						// Record the hitsplat
						pendingAttack.addHitsplatReceiver(new HitsplatData(event.getHitsplat().getAmount(), npc.getIndex()));

					}
				}
			}
			else if(actor instanceof Player)
			{
				appliedHitsplats += actor.getName() +",";
			}
		}
		hitsplatApplieds.clear();
		int currentTick = client.getTickCount();

		// Process pending attacks from previous ticks
		Iterator<Map.Entry<Integer, PendingAttack>> iterator = pendingAttacks.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<Integer, PendingAttack> entry = iterator.next();
			int attackTick = entry.getKey();
			PendingAttack attack = entry.getValue();
			// If the attack is older than one tick ago or hitsplats are complete
			if (currentTick - attackTick > 1 || attack.isHitsplatsComplete())
			{
				int index100 = -1;
				int index50 = -1;
				int index25 = -1;
				for(HitsplatData hitsplatData : attack.getHitsplatReceivers())
				{
					if(hitsplatData.damage > 12)
					{
						index100 = hitsplatData.index;
					}
					else if(hitsplatData.damage > 6)
					{
						index50 = hitsplatData.index;
					}
					else
					{
						index25 = hitsplatData.index;
					}
				}
				if(index100 != -1 && index50 != -1)
				{
					System.out.println("Target: " + attack.targetNPC.getIndex() + ", by damage: " + index100 + ","+index50+","+index25+", by order: " + attack.primaryHitsplatReceiverIndex+","+attack.secondaryHitsplatReceiverIndex+","+attack.tertiaryHitsplatReceiverIndex);
				}
				// Write attack data to the file
				//writeAttackDataToFile(attack);

				// Remove from pending attacks
				iterator.remove();
			}
		}

		// Update chunk NPC lists
		updateChunkNPCLists();

		// Write chunk data to the file
		writeChunkDataToFile(currentTick);
	}

	private void updateChunkNPCLists()
	{
		List<NPC> bloodvelds = getBloodveldNPCs();

		Set<Integer> currentNPCIndexes = new HashSet<>();
		for (NPC npc : bloodvelds)
		{
			int npcIndex = npc.getIndex();
			currentNPCIndexes.add(npcIndex);

			int npcX = npc.getWorldLocation().getRegionX();
			int npcY = npc.getWorldLocation().getRegionY();
			Chunk currentChunk = getChunkForCoordinates(npcX, npcY);
			Chunk lastChunk = npcLastChunkMap.get(npcIndex);

			if (!currentChunk.equals(lastChunk))
			{
				if (lastChunk != null)
				{
					LinkedHashSet<Integer> lastChunkNPCs = chunkNPCMap.get(lastChunk);
					if (lastChunkNPCs != null)
					{
						lastChunkNPCs.remove(npcIndex);
					}
				}
				chunkNPCMap.computeIfAbsent(currentChunk, k -> new LinkedHashSet<>()).add(npcIndex);

				// Update last known chunk
				npcLastChunkMap.put(npcIndex, currentChunk);
			}
		}

		// Remove dead or despawned NPCs from chunks
		Iterator<Map.Entry<Chunk, LinkedHashSet<Integer>>> chunkIterator = chunkNPCMap.entrySet().iterator();
		while (chunkIterator.hasNext())
		{
			Map.Entry<Chunk, LinkedHashSet<Integer>> entry = chunkIterator.next();
			LinkedHashSet<Integer> npcSet = entry.getValue();
			npcSet.removeIf(npcIndex -> !currentNPCIndexes.contains(npcIndex));

			if (npcSet.isEmpty())
			{
				chunkIterator.remove();
			}
		}
	}

	private void writeAttackDataToFile(PendingAttack attack)
	{
		// Build JSON data
		JsonObject json = new JsonObject();

		json.addProperty("tick", attack.tick);

		JsonObject playerData = new JsonObject();
		playerData.addProperty("x", attack.playerX);
		playerData.addProperty("y", attack.playerY);
		playerData.addProperty("plane", attack.playerPlane);
		json.add("playerPosition", playerData);

		json.addProperty("targetNPCIndex", attack.targetNPC.getIndex());

		JsonArray arcTilesArray = new JsonArray();
		for (int[] tile : attack.arcTiles)
		{
			JsonObject tileObj = new JsonObject();
			tileObj.addProperty("x", tile[0]);
			tileObj.addProperty("y", tile[1]);
			arcTilesArray.add(tileObj);
		}
		json.add("arcTiles", arcTilesArray);

		JsonArray npcsSWIntersectArray = new JsonArray();
		for (Map.Entry<NPC, int[]> entry : attack.npcsWithSWTileIntersectingArc.entrySet())
		{
			JsonObject npcObj = new JsonObject();
			npcObj.addProperty("index", entry.getKey().getIndex());
			npcObj.addProperty("x", entry.getValue()[0]);
			npcObj.addProperty("y", entry.getValue()[1]);
			npcsSWIntersectArray.add(npcObj);
		}
		json.add("npcsSWTileIntersectingArc", npcsSWIntersectArray);

		JsonArray npcsAreaIntersectArray = new JsonArray();
		for (Map.Entry<NPC, int[]> entry : attack.npcsWithAreaIntersectingArc.entrySet())
		{
			JsonObject npcObj = new JsonObject();
			npcObj.addProperty("index", entry.getKey().getIndex());
			npcObj.addProperty("x", entry.getValue()[0]);
			npcObj.addProperty("y", entry.getValue()[1]);
			npcsAreaIntersectArray.add(npcObj);
		}
		json.add("npcsAreaIntersectingArc", npcsAreaIntersectArray);

		json.addProperty("primaryHitsplatReceiverIndex", attack.primaryHitsplatReceiverIndex);
		json.addProperty("secondaryHitsplatReceiverIndex", attack.secondaryHitsplatReceiverIndex);
		json.addProperty("tertiaryHitsplatReceiverIndex", attack.tertiaryHitsplatReceiverIndex);

		try (FileWriter file = new FileWriter("C:\\bloodvelds\\data.log", true))
		{
			gson.toJson(json, file);
			file.write(System.lineSeparator());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void writeChunkDataToFile(int currentTick)
	{
		JsonObject json = new JsonObject();
		json.addProperty("tick", currentTick);

		JsonArray chunksArray = new JsonArray();
		for (Map.Entry<Chunk, LinkedHashSet<Integer>> entry : chunkNPCMap.entrySet())
		{
			Chunk chunk = entry.getKey();
			LinkedHashSet<Integer> npcSet = entry.getValue();

			JsonObject chunkObj = new JsonObject();
			chunkObj.addProperty("chunkX", chunk.x);
			chunkObj.addProperty("chunkY", chunk.y);

			JsonArray npcIndices = new JsonArray();
			for (Integer npcIndex : npcSet)
			{
				npcIndices.add(npcIndex);
			}
			chunkObj.add("npcIndices", npcIndices);

			chunksArray.add(chunkObj);
		}
		json.add("chunks", chunksArray);

		try (FileWriter file = new FileWriter("C:\\bloodvelds\\data.log", true))
		{
			gson.toJson(json, file);
			file.write(System.lineSeparator());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private List<int[]> computeArcTiles(int targetX, int targetY, int plane, boolean isHorizontalArc)
	{
		List<int[]> arcTiles = new ArrayList<>();

		// Center tile is target SW tile
		arcTiles.add(new int[]{targetX, targetY, plane});

		if (isHorizontalArc)
		{
			// Add tiles to the east and west
			arcTiles.add(new int[]{targetX - 1, targetY, plane}); // West tile
			arcTiles.add(new int[]{targetX + 1, targetY, plane}); // East tile
		}
		else
		{
			// Vertical arc, add tiles to the north and south
			arcTiles.add(new int[]{targetX, targetY - 1, plane}); // South tile
			arcTiles.add(new int[]{targetX, targetY + 1, plane}); // North tile
		}

		return arcTiles;
	}

	private List<NPC> getBloodveldNPCsInArea(int centerX, int centerY, int radius)
	{
		List<NPC> result = new ArrayList<>();
		for (NPC npc : client.getNpcs())
		{
			if (npc.getName() != null && npc.getName().contains("Bloodveld") && !npc.isDead())
			{
				int npcX = npc.getWorldLocation().getRegionX();
				int npcY = npc.getWorldLocation().getRegionY();
				int dx = npcX - centerX;
				int dy = npcY - centerY;
				if (Math.abs(dx) <= radius && Math.abs(dy) <= radius)
				{
					result.add(npc);
				}
			}
		}
		return result;
	}

	private List<NPC> getBloodveldNPCs()
	{
		List<NPC> result = new ArrayList<>();
		for (NPC npc : client.getNpcs())
		{
			if (npc.getName() != null && npc.getName().contains("Bloodveld") && !npc.isDead())
			{
				result.add(npc);
			}
		}
		return result;
	}

	private boolean npcIntersectsArc(int npcX, int npcY, int plane, List<int[]> arcTiles)
	{
		// NPC occupies a 2x2 area from SW to NE
		Set<String> npcTiles = new HashSet<>();
		npcTiles.add(npcX + "," + npcY);
		npcTiles.add((npcX + 1) + "," + npcY);
		npcTiles.add(npcX + "," + (npcY + 1));
		npcTiles.add((npcX + 1) + "," + (npcY + 1));

		for (int[] arcTile : arcTiles)
		{
			String tileKey = arcTile[0] + "," + arcTile[1];
			if (npcTiles.contains(tileKey))
			{
				return true;
			}
		}
		return false;
	}

	private boolean arcTilesContains(List<int[]> arcTiles, int x, int y)
	{
		for (int[] tile : arcTiles)
		{
			if (tile[0] == x && tile[1] == y)
			{
				return true;
			}
		}
		return false;
	}

	private Chunk getChunkForCoordinates(int x, int y)
	{
		int chunkX = x / 8;
		int chunkY = y / 8;
		return new Chunk(chunkX, chunkY);
	}
	@Value
	private class HitsplatData
	{
		private int damage;
		private int index;
	}

	// Internal classes
	private class PendingAttack
	{
		int tick;
		int playerX;
		int playerY;
		int playerPlane;
		NPC targetNPC;
		List<int[]> arcTiles;
		Map<NPC, int[]> npcsWithSWTileIntersectingArc;
		Map<NPC, int[]> npcsWithAreaIntersectingArc;
		Integer primaryHitsplatReceiverIndex;
		Integer secondaryHitsplatReceiverIndex;
		Integer tertiaryHitsplatReceiverIndex;
		@Getter
		private List<HitsplatData> hitsplatReceivers = new ArrayList<>();

		public void addHitsplatReceiver(HitsplatData npcIndex)
		{
			hitsplatReceivers.add(npcIndex);
			if (hitsplatReceivers.size() == 1)
			{
				primaryHitsplatReceiverIndex = npcIndex.index;
			}
			else if (hitsplatReceivers.size() == 2)
			{
				secondaryHitsplatReceiverIndex = npcIndex.index;
			}
			else if (hitsplatReceivers.size() == 3)
			{
				tertiaryHitsplatReceiverIndex = npcIndex.index;
			}
		}

		public boolean isHitsplatsComplete()
		{
			// Assuming up to 3 hitsplats
			return hitsplatReceivers.size() >= 3;
		}
	}

	private class Chunk
	{
		int x; // chunk X coordinate
		int y; // chunk Y coordinate

		public Chunk(int x, int y)
		{
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (!(obj instanceof Chunk)) return false;
			Chunk other = (Chunk) obj;
			return this.x == other.x && this.y == other.y;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(x, y);
		}
	}
}

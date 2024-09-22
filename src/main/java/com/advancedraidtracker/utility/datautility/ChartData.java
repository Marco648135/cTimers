package com.advancedraidtracker.utility.datautility;

import com.advancedraidtracker.constants.RaidRoom;
import com.advancedraidtracker.utility.weapons.AnimationDecider;
import com.advancedraidtracker.utility.weapons.PlayerAnimation;
import com.advancedraidtracker.utility.wrappers.DawnSpec;
import com.advancedraidtracker.utility.wrappers.PlayerDidAttack;
import com.advancedraidtracker.ui.charts.chartelements.ThrallOutlineBox;
import com.advancedraidtracker.utility.wrappers.StringInt;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;

import java.util.*;

import static com.advancedraidtracker.constants.RaidRoom.ALL;

@Slf4j
public class ChartData
{
    private final Map<RaidRoom, Map<Integer, Integer>> hpMapping;
    private final Map<RaidRoom, Map<Integer, String>> npcMapping;
    private final Multimap<RaidRoom, PlayerDidAttack> attacks;

    private final Map<RaidRoom, List<ThrallOutlineBox>> thrallBoxes;
    public final List<String> maidenCrabs = new ArrayList<>();
	public final List<StringInt> playerStoodInBlood = new ArrayList<>();
	public final List<StringInt> playerChancedDrain = new ArrayList<>();
    public List<DawnSpec> dawnSpecs = new ArrayList<>();


    public int getIdleTicks(String player, int scale)
    {
        return getIdleTicks(player, ALL, scale);
    }

    public int getWeaponHits(RaidRoom room, PlayerAnimation ... playerAnimations)
    {
        int count = 0;
        for(PlayerDidAttack attack : getAttacks(room))
        {
            if(Arrays.stream(playerAnimations).anyMatch(playerAnimation -> playerAnimation.equals(attack.getPlayerAnimation())))
            {
                count++;
            }
        }
        return count;
    }

    public int getIdleTicks(String player, RaidRoom room, int scale)
    {
        int idleTicks = 0;
        int lastAttack = 0;
        int firstAttack = Integer.MAX_VALUE;
        int ticksOnCD = 0;
        if (!room.equals(ALL))
        {
            for (PlayerDidAttack attack : getAttacks(room))
            {
				PlayerAnimation playerAnimation = AnimationDecider.getWeapon(attack.animation, attack.spotAnims, attack.projectile, attack.weapon);
				int cd = playerAnimation.attackTicks;
                if (attack.tick + cd > lastAttack && cd > 0)
                {
                    lastAttack = attack.tick + cd;
                }
                if (attack.tick < firstAttack && cd > 0)
                {
                    firstAttack = attack.tick;
                }
                if (attack.player.equals(player))
                {
                    if (cd > 0)
                    {
                        ticksOnCD += cd;
                    }
                }
            }
            idleTicks = (lastAttack - firstAttack) - ticksOnCD;
            return idleTicks;
        } else
        {
            for (RaidRoom r : attacks.keySet())
            {
                lastAttack = 0;
                firstAttack = Integer.MAX_VALUE;
                ticksOnCD = 0;
                for (PlayerDidAttack attack : getAttacks(r))
                {
                    int cd = AnimationDecider.getWeapon(attack.animation, attack.spotAnims, attack.projectile, attack.weapon).attackTicks;
                    if (attack.tick + cd - 1 > lastAttack && cd > 0)
                    {
                        lastAttack = attack.tick + cd - 1;
                    }
                    if (attack.tick < firstAttack && cd > 0)
                    {
                        firstAttack = attack.tick;
                    }
                    if (attack.player.equals(player))
                    {
                        if (cd > 0)
                        {
                            ticksOnCD += cd;
                        }
                    }
                }
                idleTicks += (lastAttack - firstAttack) - ticksOnCD;
            }
            return idleTicks;
        }
    }

    public ChartData()
    {
        this.hpMapping = new HashMap<>();
        this.npcMapping = new HashMap<>();
        this.attacks = ArrayListMultimap.create();
        this.thrallBoxes = new HashMap<>();
    }

    public void addHPMapping(RaidRoom room, Integer tick, Integer hp)
    {
        hpMapping.computeIfAbsent(room, k -> new HashMap<>()).put(tick, hp);
    }

    public void addNPCMapping(RaidRoom room, Integer index, String npcDescription)
    {
        npcMapping.computeIfAbsent(room, k -> new HashMap<>()).put(index, npcDescription);
    }

    public void addAttack(RaidRoom room, PlayerDidAttack attack)
    {
		for(PlayerDidAttack checkedAttack : attacks.get(room))
		{
			if(checkedAttack.tick == attack.tick && checkedAttack.player.equals(attack.player) && checkedAttack.animation.equals(attack.animation))
			{
				return;
			}
		}
        attacks.put(room, attack);
    }

    public void addThrallOutlineBox(RaidRoom room, String owner, int spawnTick, int id)
    {
        thrallBoxes.computeIfAbsent(room, k -> new ArrayList<>()).add(new ThrallOutlineBox(owner, spawnTick, id));
    }

    public Map<Integer, Integer> getHPMapping(RaidRoom room)
    {
        return hpMapping.getOrDefault(room, new HashMap<>());
    }

    public Map<Integer, String> getNPCMapping(RaidRoom room)
    {
        return npcMapping.getOrDefault(room, new HashMap<>());
    }

    public Collection<PlayerDidAttack> getAttacks(RaidRoom room)
    {
        return attacks.get(room);
    }

    public List<ThrallOutlineBox> getThralls(RaidRoom room)
    {
        return thrallBoxes.getOrDefault(room, new ArrayList<>());
    }

    public void addMaidenCrab(String crab)
    {
        maidenCrabs.add(crab);
    }

	public void addMaidenStoodInBlood(String player, int tick)
	{
		playerStoodInBlood.add(new StringInt(player, tick));
	}

	public void addPlayerChancedDrain(String player, int tick)
	{
		playerChancedDrain.add(new StringInt(player, tick));
	}

    public void addDawnSpec(DawnSpec dawnSpec)
    {
        dawnSpecs.add(dawnSpec);
    }

    public void addDawnSpecs(List<DawnSpec> dawnSpecs)
    {
        this.dawnSpecs = dawnSpecs;
    }


    public static PlayerDidAttack getPlayerDidAttack(String[] subData, ItemManager itemManager)
    {
        String player = subData[4].split(":")[0];
        int tick = Integer.parseInt(subData[4].split(":")[1]);
        String wornItems = "";
        String[] animationAndWorn = subData[5].split(":");
        String animation = animationAndWorn[0];
        if (animationAndWorn.length == 2)
        {
            wornItems = animationAndWorn[1];
        }
        String spotAnims = subData[6];
        String[] subsubData = subData[7].split(":");
        int weapon = Integer.parseInt(subsubData[0]);
        int interactedIndex = -1;
        int interactedID = -1;
        if (subsubData.length > 2)
        {
            interactedIndex = Integer.parseInt(subsubData[1]);
            interactedID = Integer.parseInt(subsubData[2]);
        }
        String[] projectileAndTargetData = subData[8].split(":");
        String projectile = projectileAndTargetData[0];
        String targetName = "";
        if (projectileAndTargetData.length > 1)
        {
            targetName = projectileAndTargetData[1];
        }
        return (new PlayerDidAttack(itemManager, player, animation, tick, weapon, projectile, spotAnims, interactedIndex, interactedID, targetName, wornItems));
    }


}

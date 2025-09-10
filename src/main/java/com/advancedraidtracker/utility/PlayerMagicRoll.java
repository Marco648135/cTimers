package com.advancedraidtracker.utility;

import com.advancedraidtracker.AdvancedRaidTrackerPlugin;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.ItemVariationMapping;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

public class PlayerMagicRoll {

    private Player player;
    private int bonus;
    @Getter
    private int roll;
    private int level;
    private double prayerBoost = 0;
    private ItemManager itemManager;

    private final AdvancedRaidTrackerPlugin plugin;
    private final Client client;

    private static final int GUARANTEED_FREEZE_ROLL = 22032;
    private static final Set<Integer> VOID_PIECES = ImmutableSet.<Integer>builder()
            .addAll(ItemVariationMapping.getVariations(ItemID.ELITE_VOID_KNIGHT_ROBES))
            .addAll(ItemVariationMapping.getVariations(ItemID.ELITE_VOID_KNIGHT_TOP))
            .addAll(ItemVariationMapping.getVariations(ItemID.PEST_VOID_KNIGHT_GLOVES))
            .addAll(ItemVariationMapping.getVariations(ItemID.GAME_PEST_MAGE_HELM))
            .build();

    // todo(levex): remove the itemManager and inject it
    public PlayerMagicRoll(Player p, ItemManager itemManager, AdvancedRaidTrackerPlugin plugin, Client client)
    {
        this.plugin = plugin;
        this.client = client;

        PlayerData pd = plugin.getPlayerDataFor(p.getName());

        if (pd != null) {
            this.level = pd.getMagicLevel();
            this.prayerBoost = getPrayerBoost(pd.getPrayers());
        } else {
            if (p.getName().equalsIgnoreCase(client.getLocalPlayer().getName())) {
                // Local player, can easily check.
                this.level = client.getBoostedSkillLevel(Skill.MAGIC);
                this.prayerBoost = client.isPrayerActive(Prayer.AUGURY) ? 1.25d : 1.0d;
            } else {
                // assume not stupid.
                this.level = 112;
                this.prayerBoost = 1.25d;
            }
        }

        this.player = p;
        this.itemManager = itemManager;

        this.bonus = 0;
        calculateMagicRoll();
    }

    private double getPrayerBoost(Map<Prayer, Boolean> pd)
    {
        return pd.get(Prayer.AUGURY) ? 1.25d :
                pd.get(Prayer.MYSTIC_WILL) ? 1.05d :
                pd.get(Prayer.MYSTIC_LORE) ? 1.10d :
                pd.get(Prayer.MYSTIC_MIGHT) ? 1.15d : 0;
    }

    private void calculateMagicRoll() {
        PlayerComposition pc = player.getPlayerComposition();
        int[] wornItems = {
                pc.getEquipmentId(KitType.HEAD),
                pc.getEquipmentId(KitType.CAPE),
                pc.getEquipmentId(KitType.AMULET),
                pc.getEquipmentId(KitType.WEAPON),
                pc.getEquipmentId(KitType.TORSO),
                pc.getEquipmentId(KitType.SHIELD),
                pc.getEquipmentId(KitType.LEGS),
                pc.getEquipmentId(KitType.HAIR),
                pc.getEquipmentId(KitType.HANDS),
                pc.getEquipmentId(KitType.BOOTS)
        };

        int voidScore = 0;
        boolean sceptre = false;
        for (int item : wornItems)
        {
            if (VOID_PIECES.contains(item))
                voidScore ++;

            if (item == ItemID.ANCIENT_SCEPTRE_ICE)
                sceptre = true;

            ItemStats itemStats = itemManager.getItemStats(item);
            if (itemStats != null)
            {
                ItemEquipmentStats itemEquipmentStats = itemStats.getEquipment();
                bonus += itemEquipmentStats.getAmagic();
            }
        }

        // calculate roll first
        double effectiveLvl = Math.floor(level * prayerBoost);
        if (voidScore == 4)
            effectiveLvl *= 1.45d;
        // +1 for long range, +9 from wiki
        effectiveLvl += 1 + 9;

        effectiveLvl = Math.floor(effectiveLvl);

        roll = (int) Math.floor(effectiveLvl * (bonus + 64));

        if (sceptre) {
            roll *= 1.10d;
            roll = (int) Math.floor(roll);

            // not sure if this is correct at all
        }
    }

    public boolean isOk() {
        return roll >= GUARANTEED_FREEZE_ROLL;
    }

    public String getRollAsPercent() {
        double accuracy = (double) roll / GUARANTEED_FREEZE_ROLL;
        return String.format("%.2f%%", accuracy * 100);
    }
}

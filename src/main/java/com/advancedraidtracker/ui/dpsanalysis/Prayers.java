package com.advancedraidtracker.ui.dpsanalysis;

import java.awt.image.BufferedImage;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;

public enum Prayers
{
	THICK_SKIN("Thick Skin", SpriteID.PRAYER_THICK_SKIN_DISABLED, SpriteID.PRAYER_THICK_SKIN, 5, 0, 0, 0, 0, 0, 0),
	BURST_OF_STRENGTH("Burst of Strength", SpriteID.PRAYER_BURST_OF_STRENGTH_DISABLED, SpriteID.PRAYER_BURST_OF_STRENGTH, 0, 5, 0, 0, 0, 0, 0),
	CLARITY_OF_THOUGHT("Clarity of Thought", SpriteID.PRAYER_CLARITY_OF_THOUGHT_DISABLED, SpriteID.PRAYER_CLARITY_OF_THOUGHT, 0, 0, 5, 0, 0, 0, 0),
	SHARP_EYE("Sharp Eye", SpriteID.PRAYER_SHARP_EYE_DISABLED, SpriteID.PRAYER_SHARP_EYE, 0, 0, 0, 5, 0, 0, 0),
	MYSTIC_WILL("Mystic Will", SpriteID.PRAYER_MYSTIC_WILL_DISABLED, SpriteID.PRAYER_MYSTIC_WILL, 0, 0, 0, 0, 0, 5, 0),
	ROCK_SKIN("Rock Skin", SpriteID.PRAYER_ROCK_SKIN_DISABLED, SpriteID.PRAYER_ROCK_SKIN, 10, 0, 0, 0, 0, 0, 0),
	SUPERHUMAN_STRENGTH("Superhuman Strength", SpriteID.PRAYER_SUPERHUMAN_STRENGTH_DISABLED, SpriteID.PRAYER_SUPERHUMAN_STRENGTH, 0, 10, 0, 0, 0, 0, 0),
	IMPROVED_REFLEXES("Improved Reflexes", SpriteID.PRAYER_IMPROVED_REFLEXES_DISABLED, SpriteID.PRAYER_IMPROVED_REFLEXES, 0, 0, 10, 0, 0, 0, 0),
	RAPID_RESTORE("Rapid Restore", SpriteID.PRAYER_RAPID_RESTORE_DISABLED, SpriteID.PRAYER_RAPID_RESTORE, 0, 0, 0, 0, 0, 0, 0),
	RAPID_HEAL("Rapid Heal", SpriteID.PRAYER_RAPID_HEAL_DISABLED, SpriteID.PRAYER_RAPID_HEAL, 0, 0, 0, 0, 0, 0, 0),
	PROTECT_ITEM("Protect Item", SpriteID.PRAYER_PROTECT_ITEM_DISABLED, SpriteID.PRAYER_PROTECT_ITEM, 0, 0, 0, 0, 0, 0, 0),
	HAWK_EYE("Hawk Eye", SpriteID.PRAYER_HAWK_EYE_DISABLED, SpriteID.PRAYER_HAWK_EYE, 0, 0, 0, 10, 0, 0, 0),
	MYSTIC_LORE("Mystic Lore", SpriteID.PRAYER_MYSTIC_LORE_DISABLED, SpriteID.PRAYER_MYSTIC_LORE, 0, 0, 0, 0, 0, 10, 1),
	STEEL_SKIN("Steel Skin", SpriteID.PRAYER_STEEL_SKIN_DISABLED, SpriteID.PRAYER_STEEL_SKIN, 15, 0, 0, 0, 0, 0, 0),
	ULTIMATE_STRENGTH("Ultimate Strength", SpriteID.PRAYER_ULTIMATE_STRENGTH_DISABLED, SpriteID.PRAYER_ULTIMATE_STRENGTH, 0, 15, 0, 0, 0, 0, 0),
	INCREDIBLE_REFLEXES("Incredible Reflexes", SpriteID.PRAYER_INCREDIBLE_REFLEXES_DISABLED, SpriteID.PRAYER_INCREDIBLE_REFLEXES, 0, 0, 15, 0, 0, 0, 0),
	PROTECT_FROM_MAGIC("Protect from Magic", SpriteID.PRAYER_PROTECT_FROM_MAGIC_DISABLED, SpriteID.PRAYER_PROTECT_FROM_MAGIC, 0, 0, 0, 0, 0, 0, 0),
	PROTECT_FROM_MISSILES("Protect from Missiles", SpriteID.PRAYER_PROTECT_FROM_MISSILES_DISABLED, SpriteID.PRAYER_PROTECT_FROM_MISSILES, 0, 0, 0, 0, 0, 0, 0),
	PROTECT_FROM_MELEE("Protect from Melee", SpriteID.PRAYER_PROTECT_FROM_MELEE_DISABLED, SpriteID.PRAYER_PROTECT_FROM_MELEE, 0, 0, 0, 0, 0, 0, 0),
	EAGLE_EYE("Eagle Eye", SpriteID.PRAYER_EAGLE_EYE_DISABLED, SpriteID.PRAYER_EAGLE_EYE, 0, 0, 0, 15, 0, 0, 0),
	MYSTIC_MIGHT("Mystic Might", SpriteID.PRAYER_MYSTIC_MIGHT_DISABLED, SpriteID.PRAYER_MYSTIC_MIGHT, 0, 0, 0, 0, 0, 15, 2),
	RETRIBUTION("Retribution", SpriteID.PRAYER_RETRIBUTION_DISABLED, SpriteID.PRAYER_RETRIBUTION, 0, 0, 0, 0, 0, 0, 0),
	REDEMPTION("Redemption", SpriteID.PRAYER_REDEMPTION_DISABLED, SpriteID.PRAYER_REDEMPTION, 0, 0, 0, 0, 0, 0, 0),
	SMITE("Smite", SpriteID.PRAYER_SMITE_DISABLED, SpriteID.PRAYER_SMITE, 0, 0, 0, 0, 0, 0, 0),
	PRESERVE("Preserve", SpriteID.PRAYER_PRESERVE_DISABLED, SpriteID.PRAYER_PRESERVE, 0, 0, 0, 0, 0, 0, 0),
	CHIVALRY("Chivalry", SpriteID.PRAYER_CHIVALRY_DISABLED, SpriteID.PRAYER_CHIVALRY, 20, 18, 15, 0, 0, 0, 0),
	PIETY("Piety", SpriteID.PRAYER_PIETY_DISABLED, SpriteID.PRAYER_PIETY, 25, 23, 20, 0, 0, 0, 0),
	RIGOUR("Rigour", SpriteID.PRAYER_RIGOUR_DISABLED, SpriteID.PRAYER_RIGOUR, 25, 0, 0, 20, 23, 0, 0),
	AUGURY("Augury", SpriteID.PRAYER_AUGURY_DISABLED, SpriteID.PRAYER_AUGURY, 25, 0, 0, 0, 0, 25, 4);

	public final String name;
	public final int disabledID;
	public final int enabledID;
	public final int def;    // Defence boost (%)
	public final int str;    // Strength boost (%)
	public final int att;    // Attack boost (%)
	public final int ran;    // Ranged attack boost (%)
	public final int ranD;   // Ranged strength boost (%)
	public final int mag;    // Magic attack and defence boost (%)
	public final int magD;   // Magic damage boost (%)

	Prayers(String name, int disabledID, int enabledID, int def, int str, int att, int ran, int ranD, int mag, int magD)
	{
		this.name = name;
		this.disabledID = disabledID;
		this.enabledID = enabledID;
		this.def = def;
		this.str = str;
		this.att = att;
		this.ran = ran;
		this.ranD = ranD;
		this.mag = mag;
		this.magD = magD;
	}
}
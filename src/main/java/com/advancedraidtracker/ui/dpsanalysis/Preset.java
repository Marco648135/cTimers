package com.advancedraidtracker.ui.dpsanalysis;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Preset
{
	private String name;
	private Map<String, EquipmentData> equipment;
	private Map<Prayers, Boolean> prayers;
	private String selectedStyle;

	// New fields
	private Map<String, Integer> baseLevels; // Attack, Strength, Ranged, Magic
	private Map<String, String> selectedPotions; // Potion selected for each skill
	private String selectedAttackType;
	private boolean visible;

	// Constructor
	public Preset(
		String name,
		Map<String, EquipmentData> equipment,
		Map<Prayers, Boolean> prayers,
		String selectedStyle,
		Map<String, Integer> baseLevels,
		Map<String, String> selectedPotions,
		String selectedAttackType
	)
	{
		this.name = name;
		this.equipment = equipment;
		this.prayers = prayers;
		this.selectedStyle = selectedStyle;
		this.baseLevels = baseLevels;
		this.selectedPotions = selectedPotions;
		this.selectedAttackType = selectedAttackType;
	}

	private String getEquipmentName(String slot)
	{
		EquipmentData item = equipment.get(slot);
		return (item != null) ? item.getName() : "";
	}

	public boolean isMeleeVoid()
	{
		return getEquipmentName("head").contains("Void ranger helm")
			&& (getEquipmentName("body").contains("Elite void top") || getEquipmentName("body").contains("Void knight top"))
			&& (getEquipmentName("legs").contains("Elite void robe") || getEquipmentName("legs").contains("Void knight robe"))
			&& getEquipmentName("hands").contains("Void knight gloves");
	}

	public boolean isRangeVoid()
	{
		return getEquipmentName("head").contains("Void ranger helm")
			&& getEquipmentName("body").contains("Void knight top")
			&& getEquipmentName("legs").contains("Void knight robe")
			&& getEquipmentName("hands").contains("Void knight gloves");
	}

	public boolean isRangeEVoid()
	{
		return getEquipmentName("head").contains("Void ranger helm")
			&& getEquipmentName("body").contains("Elite void top")
			&& getEquipmentName("legs").contains("Elite void robe")
			&& getEquipmentName("hands").contains("Void knight gloves");
	}

	public boolean isSalve()
	{
		return false;
	}

	public boolean isSalveE()
	{
		return false;
	}

	public boolean isSlayer()
	{
		return false;
	}

	public Map<String, Integer> getVirtualLevels()
	{
		Map<String, Integer> virtualLevels = new HashMap<>();

		// Get total prayer boosts
		Map<String, Integer> prayerBoosts = getPrayerBoostForPreset(prayers);

		// For Melee Attack
		int baseAttackLevel = baseLevels.getOrDefault("Attack", 1);
		int attackPotionBoost = getPotionBoost("Attack", baseAttackLevel, selectedPotions.getOrDefault("Attack", "None"));
		int meleeAttackPrayerBoost = (int) Math.floor((baseAttackLevel + attackPotionBoost) * prayerBoosts.get("meleeAttack") / 100.0);
		int virtualAttackLevel = baseAttackLevel + attackPotionBoost + meleeAttackPrayerBoost;
		virtualLevels.put("Attack", virtualAttackLevel);

		// For Melee Strength
		int baseStrengthLevel = baseLevels.getOrDefault("Strength", 1);
		int strengthPotionBoost = getPotionBoost("Strength", baseStrengthLevel, selectedPotions.getOrDefault("Strength", "None"));
		int meleeStrengthPrayerBoost = (int) Math.floor((baseStrengthLevel + strengthPotionBoost) * prayerBoosts.get("meleeStrength") / 100.0);
		int virtualStrengthLevel = baseStrengthLevel + strengthPotionBoost + meleeStrengthPrayerBoost;
		virtualLevels.put("Strength", virtualStrengthLevel);

		// For Ranged
		int baseRangedLevel = baseLevels.getOrDefault("Ranged", 1);
		int rangedPotionBoost = getPotionBoost("Ranged", baseRangedLevel, selectedPotions.getOrDefault("Ranged", "None"));
		int rangedAttackPrayerBoost = (int) Math.floor((baseRangedLevel + rangedPotionBoost) * prayerBoosts.get("rangedAttack") / 100.0);
		int virtualRangedAttackLevel = baseRangedLevel + rangedPotionBoost + rangedAttackPrayerBoost;
		virtualLevels.put("RangedAttack", virtualRangedAttackLevel);


		int rangedStrengthPrayerBoost = (int) Math.floor((baseRangedLevel + rangedPotionBoost) * prayerBoosts.get("rangedStrength") / 100.0);
		int virtualRangedStrengthLevel = baseRangedLevel + rangedPotionBoost + rangedStrengthPrayerBoost;
		virtualLevels.put("RangedStrength", virtualRangedStrengthLevel);

		// For Magic (if necessary)
		// ...

		// Note: Ranged Strength bonuses from prayer are applied separately in damage calculations, not to levels.

		return virtualLevels;
	}

	public EquipmentData getWeapon()
	{
		return getEquipment().get("weapon");
	}

	private Map<String, Integer> getPrayerBoostForPreset(Map<Prayers, Boolean> prayers)
	{
		Map<String, Integer> boosts = new HashMap<>();
		double meleeAttackBoostPercentage = 0.0;
		double meleeStrengthBoostPercentage = 0.0;
		double rangedAttackBoostPercentage = 0.0;
		double rangedStrengthBoostPercentage = 0.0;
		double magicAttackBoostPercentage = 0.0;
		double magicDamageBoostPercentage = 0.0;
		// Add more if needed

		for (Map.Entry<Prayers, Boolean> entry : prayers.entrySet())
		{
			if (entry.getValue())
			{
				Prayers prayer = entry.getKey();
				meleeAttackBoostPercentage += prayer.att;
				meleeStrengthBoostPercentage += prayer.str;
				rangedAttackBoostPercentage += prayer.ranA;
				rangedStrengthBoostPercentage += prayer.ranD;
				magicAttackBoostPercentage += prayer.mag;
				magicDamageBoostPercentage += prayer.magD;
				// Continue for other bonuses
			}
		}

		boosts.put("meleeAttack", (int) meleeAttackBoostPercentage);
		boosts.put("meleeStrength", (int) meleeStrengthBoostPercentage);
		boosts.put("rangedAttack", (int) rangedAttackBoostPercentage);
		boosts.put("rangedStrength", (int) rangedStrengthBoostPercentage);
		boosts.put("magicAttack", (int) magicAttackBoostPercentage);
		boosts.put("magicDamage", (int) magicDamageBoostPercentage);

		return boosts;
	}

	private int getPotionBoost(String skill, int baseLevel, String potion)
	{
		switch (potion)
		{
			case "None":
				return 0;
			case "Attack":
				return (int) Math.floor(baseLevel * 0.10 + 3);
			case "Super Attack":
				return (int) Math.floor(baseLevel * 0.15 + 5);
			case "Zamorak Brew":
				return (int) Math.floor(baseLevel * 0.20 + 2);
			case "Strength":
				return (int) Math.floor(baseLevel * 0.10 + 3);
			case "Super Strength":
				return (int) Math.floor(baseLevel * 0.15 + 5);
			case "Ranging":
				return (int) Math.floor(baseLevel * 0.10 + 4);
			case "Super Ranging":
				return (int) Math.floor(baseLevel * 0.15 + 5);
			case "Magic":
				return 4;
			case "Super Magic":
				return (int) Math.floor(baseLevel * 0.15 + 5);
			case "Overload (Raids)":
				return (int) Math.floor(baseLevel * 0.16 + 6);
			case "Smelling Salts":
				return (int) Math.floor(baseLevel * 0.16 + 11);
			case "Ancient Brew":
				return (int) Math.floor(baseLevel * 0.05 + 2);
			case "Magister's Brew":
				return (int) Math.floor(baseLevel * 0.08 + 3);
			case "Imbued Heart":
				return (int) Math.floor(baseLevel * 0.10 + 1);
			case "Saturated Heart":
				return (int) Math.floor(baseLevel * 0.10 + 4);
			default:
				return 0;
		}
	}

	private int getAttackSpeed()
	{
		try
		{
			return getEquipment().get("weapon").getSpeed();
		}
		catch (NullPointerException e)
		{
			return 5; //barehanded? todo idk this sucks
		}
	}

	public AttackData computeAttackData(NPCData target)
	{
		int effectiveAttackLevel = 0;
		int effectiveStrengthLevel = 0;
		int maxHit = 0;
		int attackRoll = 0;
		String selectedStyle = getSelectedStyle();

		Map<String, Integer> virtualLevels = getVirtualLevels();

		int attackSpeed = getAttackSpeed();

		String attackType = "";
		// Use the virtual levels in calculations
		if (isMeleeStyle(selectedStyle))
		{
			attackType = getSelectedAttackType();
			effectiveAttackLevel = virtualLevels.get("Attack");
			effectiveStrengthLevel = virtualLevels.get("Strength");

			// Adjust for attack style
			if (selectedStyle.equals("Accurate"))
			{
				effectiveAttackLevel += 3;
			}
			else if (selectedStyle.equals("Aggressive"))
			{
				effectiveStrengthLevel += 3;
			}
			else if (selectedStyle.equals("Controlled"))
			{
				effectiveAttackLevel += 1;
				effectiveStrengthLevel += 1;
			}
			else if (selectedStyle.equals("Defensive"))
			{
				// Defensive style, no attack/strength bonus
			}

			effectiveStrengthLevel += 8;
			effectiveAttackLevel += 8;

			if (isMeleeVoid())
			{
				effectiveStrengthLevel = (int) (effectiveStrengthLevel * 1.1);
				effectiveAttackLevel = (int) (effectiveAttackLevel * 1.1);
			}

			// Equipment bonuses
			int attackBonus = 0;
			int strengthBonus = 0;
			for (EquipmentData data : getEquipment().values())
			{
				if (getSelectedAttackType().equals("Stab"))
				{
					attackBonus += data.getOffensive().getStab();
				}
				else if (getSelectedAttackType().equals("Slash"))
				{
					attackBonus += data.getOffensive().getSlash();
				}
				else if (getSelectedAttackType().equals("Crush"))
				{
					attackBonus += data.getOffensive().getCrush();
				}
				strengthBonus += data.getBonuses().getStr();
			}

			maxHit = (int) ((effectiveStrengthLevel * (strengthBonus + 64) + 320) / 640.0);
			attackRoll = effectiveAttackLevel * (attackBonus + 64);
			if (isSalveE())
			{
				maxHit = (int) (maxHit * 1.20);
				attackRoll = (int) (attackRoll * 1.20);
			}
			else if (isSalve() || isSlayer())
			{
				maxHit = (int) (maxHit * (7.0 / 6.0));
				attackRoll = (int) (attackRoll * (7.0 / 6.0));
			}
		}
		else if (isRangedStyle(selectedStyle))
		{
			effectiveAttackLevel = virtualLevels.get("RangedAttack");
			effectiveStrengthLevel = virtualLevels.get("RangedStrength");

			// Adjust for attack style
			if (selectedStyle.equals("Accurate (Ranged)"))
			{
				effectiveAttackLevel += 3;
				effectiveStrengthLevel += 3;
			}
			else if (selectedStyle.equals("Rapid"))
			{
				attackSpeed--;
			}

			effectiveStrengthLevel += 8;
			effectiveAttackLevel += 8;

			if (isRangeEVoid())
			{
				effectiveStrengthLevel = (int) (effectiveStrengthLevel * 1.125);
				effectiveAttackLevel = (int) (effectiveAttackLevel * 1.1);
			}
			else if (isRangeVoid())
			{
				effectiveStrengthLevel = (int) (effectiveStrengthLevel * 1.1);
				effectiveAttackLevel = (int) (effectiveAttackLevel * 1.1);
			}

			// Equipment bonuses
			int attackBonus = 0;
			int strengthBonus = 0; // Ranged strength bonus

			for (EquipmentData data : getEquipment().values())
			{
				attackBonus += data.getOffensive().getRanged();
				strengthBonus += data.getBonuses().getRanged_str();
				if (data.getName().toLowerCase().contains("quiver"))
				{
					attackBonus += 10;
					strengthBonus += 1;
				}
			}

			attackRoll = effectiveAttackLevel * (attackBonus + 64);

			maxHit = (int) (.5 + effectiveStrengthLevel * (strengthBonus + 64) / 640.0);

			if (isSalveE())
			{
				maxHit = (int) (maxHit * 1.2);
			}
			else if (isSalve())
			{
				maxHit = (int) (maxHit * (7.0 / 6.0));
			}
			else if (isSlayer())
			{
				maxHit = (int) (maxHit * 1.15); //todo craws/webweaver
			}

			if (getEquipment().get("weapon").getName().contains("Twisted bow"))
			{
				int magicValue = Math.max(target.getSkills().getMagic(), target.getOffensive().getMagic());
				//outside of chambers:
				magicValue = Math.min(250, magicValue);
				double tbowAccuracyMultiplier = 140 + (((((30 * magicValue) / 10.0) - 10) / 100.0) - (Math.pow((3 * magicValue / 10.0) - 100, 2) / 100.0));
				tbowAccuracyMultiplier = Math.min(tbowAccuracyMultiplier, 140);
				double tbowDamageMultiplier = 250 + (((((30 * magicValue) / 10.0) - 14) / 100.0) - (Math.pow((3 * magicValue / 10.0) - 140, 2) / 100.0));
				tbowDamageMultiplier = Math.min(tbowDamageMultiplier, 250);

				tbowAccuracyMultiplier /= 100;
				tbowDamageMultiplier /= 100;

				maxHit = (int) (maxHit * tbowDamageMultiplier);
				attackRoll = (int) (attackRoll * tbowAccuracyMultiplier);
			}

		}
		else if (isMagicStyle(selectedStyle))
		{
			effectiveAttackLevel = virtualLevels.get("Magic");

			// Adjust for attack style
			if (selectedStyle.equals("Standard"))
			{
				// No bonus
			}
			else if (selectedStyle.equals("Defensive (Magic)"))
			{
				effectiveAttackLevel += 3;
			}

			// Equipment bonuses
			int attackBonus = 0;
			for (EquipmentData data : getEquipment().values())
			{
				attackBonus += data.getOffensive().getMagic();
			}

			attackRoll = effectiveAttackLevel * (attackBonus + 64);

			maxHit = calculateMagicMaxHit(); // Depends on spell selected

		}

		int defenseRoll = calculateDefenseRoll(target, target.getSkills().getDef(), selectedStyle, attackType);
		return new AttackData(attackRoll, maxHit, attackSpeed, defenseRoll);
	}

	private int calculateMagicMaxHit()
	{
		// Placeholder: Return a default max hit, or calculate based on magic level and equipment
		// You might need to add spell selection to the UI and Preset class for accurate calculations

		// Example calculation assuming standard spellbook
		int baseMaxHit = 20; // Adjust this value based on the spell

		// Calculate magic damage bonus from equipment
		int magicDamageBonus = 0;
		for (EquipmentData data : getEquipment().values())
		{
			magicDamageBonus += data.getBonuses().getMagic_str(); // Assuming magic_str is the magic damage bonus in %
		}

		// Apply magic damage bonus
		int maxHit = (int) Math.floor(baseMaxHit * (1 + magicDamageBonus / 100.0));

		return maxHit;
	}

	private int calculateDefenseRoll(NPCData npcData, int defenseValue, String selectedStyle, String attackType)
	{
		int npcDefenseBonus = 0;

		if (isMeleeStyle(selectedStyle))
		{
			switch (attackType)
			{
				case "Stab":
					npcDefenseBonus = npcData.getDefensive().getStab();
					break;
				case "Slash":
					npcDefenseBonus = npcData.getDefensive().getSlash();
					break;
				case "Crush":
					npcDefenseBonus = npcData.getDefensive().getCrush();
					break;
			}
		}
		else if (isRangedStyle(selectedStyle))
		{
			npcDefenseBonus = npcData.getDefensive().getStandard(); //TODO :angry:
		}
		else if (isMagicStyle(selectedStyle))
		{
			npcDefenseBonus = npcData.getDefensive().getMagic();
		}

		return (defenseValue + 9) * (npcDefenseBonus + 64);
	}

	private boolean isMeleeStyle(String selectedStyle)
	{
		return selectedStyle.equals("Accurate") ||
			selectedStyle.equals("Aggressive") ||
			selectedStyle.equals("Defensive") ||
			selectedStyle.equals("Controlled");
	}

	private boolean isRangedStyle(String selectedStyle)
	{
		return selectedStyle.equals("Accurate (Ranged)") ||
			selectedStyle.equals("Rapid") ||
			selectedStyle.equals("Longrange");
	}

	private boolean isMagicStyle(String selectedStyle)
	{
		return selectedStyle.equals("Standard") ||
			selectedStyle.equals("Defensive (Magic)");
	}

}
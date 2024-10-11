package com.advancedraidtracker.ui.charts;

import com.advancedraidtracker.constants.RaidRoom;
import com.advancedraidtracker.constants.TobIDs;
import com.advancedraidtracker.ui.charts.chartelements.OutlineBox;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.clientThread;
import com.advancedraidtracker.utility.RoomUtil;
import com.advancedraidtracker.utility.wrappers.PlayerDidAttack;
import com.advancedraidtracker.utility.weapons.AnimationDecider;
import com.advancedraidtracker.utility.weapons.PlayerAnimation;
import java.util.Map;

public class ChartUtility
{


	public static OutlineBox convertToOutlineBox(PlayerDidAttack attack, PlayerAnimation playerAnimation, String room, Map<Integer, Integer> roomHP, Map<Integer, String> NPCMap)
	{
		if (clientThread != null)
		{
			clientThread.invoke(attack::setWornNames);
		}

		if (playerAnimation.equals(PlayerAnimation.NOT_SET))
		{
			playerAnimation = AnimationDecider.getWeapon(attack.animation, attack.spotAnims, attack.projectile, attack.weapon);
		}

		if (playerAnimation != PlayerAnimation.EXCLUDED_ANIMATION && playerAnimation != PlayerAnimation.UNDECIDED)
		{
			boolean isTarget = RoomUtil.isPrimaryBoss(attack.targetedID) && attack.targetedID != -1;
			String targetString = playerAnimation.name + ": ";
			String targetName = getBossName(attack.targetedID, attack.targetedIndex, attack.tick, roomHP, NPCMap);
			if (targetName.equals("?"))
			{
				targetString += attack.targetName;
			}
			else
			{
				targetString += targetName;
			}
			if (playerAnimation == PlayerAnimation.VENG_APPLIED)
			{
				targetString = playerAnimation.name + ": " + attack.damage;
			}

			String additionalText = getAdditionalText(playerAnimation, targetString);
			OutlineBox outlineBox = new OutlineBox(
				playerAnimation.shorthand,
				playerAnimation.color,
				isTarget,
				additionalText,
				playerAnimation,
				playerAnimation.attackTicks,
				attack.tick,
				attack.player,
				RaidRoom.getRoom(room),
				attack.weapon
			);
			outlineBox.setWornItems(attack.wornItems);
			outlineBox.setTooltip(targetString);
			if (attack.getPreset() != null)
			{
				outlineBox.setPreset(attack.getPreset());
			}
			if (attack.damage > -1)
			{
				outlineBox.setDamage(attack.damage);
			}
			if (clientThread != null)
			{
				clientThread.invoke(outlineBox::setWornNames);
			}
			outlineBox.setSpotAnims(attack.spotAnims);
			return outlineBox;
		}
		return null;
	}

	private static String getAdditionalText(PlayerAnimation playerAnimation, String targetString)
	{
		String additionalText = "";
		if (targetString.contains("(on w"))
		{
			additionalText = targetString.substring(targetString.indexOf("(on w") + 5);
			additionalText = "s" + additionalText.substring(0, additionalText.indexOf(")"));
		}
		else if (targetString.contains("small") || targetString.contains("big"))
		{
			additionalText = getShortenedString(targetString, playerAnimation.name.length());
		}
		else if (targetString.contains("70s") || targetString.contains("50s") || targetString.contains("30s"))
		{
			String shortenedString = targetString.substring(playerAnimation.name.length() + 2);
			shortenedString = shortenedString.substring(0, 2);
			String proc = targetString.substring(targetString.indexOf("0s") - 1, targetString.indexOf("0s") + 1);

			additionalText = proc + shortenedString;
		}
		return additionalText;
	}

	public static String getBossName(int id, int index, int tick, Map<Integer, Integer> roomHP, Map<Integer, String> NPCMap)
	{
		try
		{
			switch (id)
			{
				case TobIDs.MAIDEN_P0:
				case TobIDs.MAIDEN_P1:
				case TobIDs.MAIDEN_P2:
				case TobIDs.MAIDEN_P3:
				case TobIDs.MAIDEN_PRE_DEAD:
				case TobIDs.MAIDEN_P0_HM:
				case TobIDs.MAIDEN_P1_HM:
				case TobIDs.MAIDEN_P2_HM:
				case TobIDs.MAIDEN_P3_HM:
				case TobIDs.MAIDEN_PRE_DEAD_HM:
				case TobIDs.MAIDEN_P0_SM:
				case TobIDs.MAIDEN_P1_SM:
				case TobIDs.MAIDEN_P2_SM:
				case TobIDs.MAIDEN_P3_SM:
				case TobIDs.MAIDEN_PRE_DEAD_SM:
					return "Maiden (" + RoomUtil.varbitHPtoReadable(roomHP.get(tick + 1)) + ")";
				case TobIDs.BLOAT:
				case TobIDs.BLOAT_HM:
				case TobIDs.BLOAT_SM:
					return "Bloat (" + RoomUtil.varbitHPtoReadable(roomHP.get(tick)) + ")";
				case TobIDs.NYLO_BOSS_MELEE:
				case TobIDs.NYLO_BOSS_RANGE:
				case TobIDs.NYLO_BOSS_MAGE:
				case TobIDs.NYLO_BOSS_MELEE_HM:
				case TobIDs.NYLO_BOSS_RANGE_HM:
				case TobIDs.NYLO_BOSS_MAGE_HM:
				case TobIDs.NYLO_BOSS_MELEE_SM:
				case TobIDs.NYLO_BOSS_RANGE_SM:
				case TobIDs.NYLO_BOSS_MAGE_SM:
					return "Nylo Boss (" + RoomUtil.varbitHPtoReadable(roomHP.get(tick)) + ")";
				case TobIDs.XARPUS_P23:
				case TobIDs.XARPUS_P23_HM:
				case TobIDs.XARPUS_P23_SM:
					return "Xarpus (" + RoomUtil.varbitHPtoReadable(roomHP.get(tick)) + ")";
				case TobIDs.VERZIK_P1:
				case TobIDs.VERZIK_P2:
				case TobIDs.VERZIK_P3:
				case TobIDs.VERZIK_P1_HM:
				case TobIDs.VERZIK_P2_HM:
				case TobIDs.VERZIK_P3_HM:
				case TobIDs.VERZIK_P1_SM:
				case TobIDs.VERZIK_P2_SM:
				case TobIDs.VERZIK_P3_SM:
					return "Verzik (" + RoomUtil.varbitHPtoReadable(roomHP.get(tick)) + ")";
			}
			for (Integer i : NPCMap.keySet())
			{
				if (i == index)
				{
					return NPCMap.get(i);
				}
			}
			return "?";
		}
		catch (Exception e)
		{
			return "?";
		}
	}

	private static String getShortenedString(String targetString, int index)
	{
		String shortenedString = targetString.substring(index + 3);
		shortenedString = shortenedString.substring(0, shortenedString.indexOf(" "));
		if (targetString.contains("east small"))
		{
			shortenedString += "e";
		}
		else if (targetString.contains("south small"))
		{
			shortenedString += "s";
		}
		else if (targetString.contains("west small"))
		{
			shortenedString += "w";
		}
		else if (targetString.contains("east big"))
		{
			shortenedString += "E";
		}
		else if (targetString.contains("south big"))
		{
			shortenedString += "S";
		}
		else if (targetString.contains("west big"))
		{
			shortenedString += "W";
		}
		return shortenedString;
	}
}

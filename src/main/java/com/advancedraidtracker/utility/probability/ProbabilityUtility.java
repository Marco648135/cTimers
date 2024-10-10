package com.advancedraidtracker.utility.probability;

import com.advancedraidtracker.ui.dpsanalysis.AttackData;
import com.advancedraidtracker.ui.dpsanalysis.EquipmentData;
import com.advancedraidtracker.ui.dpsanalysis.NPCData;
import com.advancedraidtracker.ui.dpsanalysis.Preset;
import com.advancedraidtracker.utility.probability.ProbabilityCalculator.PMFGroup;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProbabilityUtility
{
	public static List<PMFGroup> convertPreset(Preset preset, NPCData target)
	{
		return convertPreset(preset, target, 1);
	}

	public static List<PMFGroup> convertPreset(Preset preset, NPCData target, int count)
	{
		List<PMFGroup> list = new ArrayList<>();
		if(target == null)
		{
			return list;
		}
		AttackData attackData = preset.computeAttackData(target);
		if (target.getName().contains("Verzik") || target.getName().contains("Phase 1"))
		{
			EquipmentData weapon = preset.getWeapon();
			if (weapon.getName().contains("Dawnbringer"))
			{
				list.add(new ProbabilityCalculator.RandomRollGroup(75, 150, count));
			}
			else if (weapon.getName().contains("Scythe"))
			{
				int max2 = attackData.getMaxHit() / 2;
				int max3 = attackData.getMaxHit() / 2 / 2;
				list.add(new ProbabilityCalculator.RandomAccuracyCheckedComparedRollGroup(0, attackData.getAttackRoll(), 0, attackData.getDefenceRoll(), 0, attackData.getMaxHit(), 0, 10, count));
				list.add(new ProbabilityCalculator.RandomAccuracyCheckedComparedRollGroup(0, attackData.getAttackRoll(), 0, attackData.getDefenceRoll(), 0, max2, 0, 10, count));
				list.add(new ProbabilityCalculator.RandomAccuracyCheckedComparedRollGroup(0, attackData.getAttackRoll(), 0, attackData.getDefenceRoll(), 0, max3, 0, 10, count));
			}
			else
			{
				list.add(new ProbabilityCalculator.RandomAccuracyCheckedComparedRollGroup(0, attackData.getAttackRoll(), 0, attackData.getDefenceRoll(), 0, attackData.getMaxHit(), 0, 10, count));
			}
		}
		return list;
	}
}

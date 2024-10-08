package com.advancedraidtracker.utility.probability;

import java.util.*;

public class ProbabilityCalculator
{

	// Enum for comparison operators
	public enum ComparisonOperator
	{
		LESS_THAN,
		LESS_THAN_OR_EQUAL,
		EQUAL,
		GREATER_THAN_OR_EQUAL,
		GREATER_THAN
	}

	// Interface for PMF groups
	public interface PMFGroup
	{
		Map<Integer, Double> getPMF();
	}

	// Class to represent a group of random rolls
	private static class RandomRollGroup implements PMFGroup
	{
		int minValue;
		int maxValue;
		int count;

		public RandomRollGroup(int minValue, int maxValue, int count)
		{
			this.minValue = minValue;
			this.maxValue = maxValue;
			this.count = count;
		}

		@Override
		public Map<Integer, Double> getPMF()
		{
			return computeSumPMF(minValue, maxValue, count);
		}
	}

	// Class to represent a group of compared random rolls
	private static class RandomComparedRollGroup implements PMFGroup
	{
		int min1;
		int max1;
		int min2;
		int max2;
		int count;

		public RandomComparedRollGroup(int min1, int max1, int min2, int max2, int count)
		{
			this.min1 = min1;
			this.max1 = max1;
			this.min2 = min2;
			this.max2 = max2;
			this.count = count;
		}

		@Override
		public Map<Integer, Double> getPMF()
		{
			Map<Integer, Double> minPMF = computeMinUniformPMF(min1, max1, min2, max2);

			Map<Integer, Double> pmf = new HashMap<>(minPMF);

			for (int i = 1; i < count; i++)
			{
				pmf = convolve(pmf, minPMF);
			}

			return pmf;
		}
	}

	private static class RandomAccuracyCheckedComparedRollGroup implements PMFGroup
	{
		int minA, maxA, minB, maxB, minC, maxC, minD, maxD, count;

		public RandomAccuracyCheckedComparedRollGroup(int minA, int maxA, int minB, int maxB,
													  int minC, int maxC, int minD, int maxD, int count)
		{
			this.minA = minA;
			this.maxA = maxA;
			this.minB = minB;
			this.maxB = maxB;
			this.minC = minC;
			this.maxC = maxC;
			this.minD = minD;
			this.maxD = maxD;
			this.count = count;
		}

		@Override
		public Map<Integer, Double> getPMF()
		{
			Map<Integer, Double> eventPMF = computeEventPMF();
			Map<Integer, Double> pmf = new HashMap<>(eventPMF);

			for (int i = 1; i < count; i++)
			{
				pmf = convolve(pmf, eventPMF);
			}

			return pmf;
		}

		// Compute the PMF of a single event
		private Map<Integer, Double> computeEventPMF()
		{
			Map<Integer, Double> pmf = new HashMap<>();

			double pAGreaterB = computePAGreaterB(minA, maxA, minB, maxB);
			double pALessEqualB = 1.0 - pAGreaterB;

			// Compute P(min(C,D) = w)
			Map<Integer, Double> minCDPMF = computeMinUniformPMF(minC, maxC, minD, maxD);

			// Outcome is 0 when A ≤ B
			pmf.put(0, pALessEqualB);

			// Add probabilities for when A > B and min(C,D) = w
			for (Map.Entry<Integer, Double> entry : minCDPMF.entrySet())
			{
				int w = entry.getKey();
				double pMinCD = entry.getValue();
				double pW = pAGreaterB * pMinCD;

				pmf.put(w, pmf.getOrDefault(w, 0.0) + pW);
			}

			return pmf;
		}
	}

	private static class RandomAccuracyCheckedRollGroup implements PMFGroup
	{
		int minA, maxA, minB, maxB, minC, maxC, count;

		public RandomAccuracyCheckedRollGroup(int minA, int maxA, int minB, int maxB,
											  int minC, int maxC, int count)
		{
			this.minA = minA;
			this.maxA = maxA;
			this.minB = minB;
			this.maxB = maxB;
			this.minC = minC;
			this.maxC = maxC;
			this.count = count;
		}

		@Override
		public Map<Integer, Double> getPMF()
		{
			Map<Integer, Double> eventPMF = computeEventPMF();
			Map<Integer, Double> pmf = new HashMap<>(eventPMF);

			for (int i = 1; i < count; i++)
			{
				pmf = convolve(pmf, eventPMF);
			}

			return pmf;
		}

		// Compute the PMF of a single event
		private Map<Integer, Double> computeEventPMF()
		{
			Map<Integer, Double> pmf = new HashMap<>();

			double pAGreaterB = computePAGreaterB(minA, maxA, minB, maxB);
			double pALessEqualB = 1.0 - pAGreaterB;

			// Outcome is 0 when A ≤ B
			pmf.put(0, pALessEqualB);

			// When A > B, outcome is a roll from minC to maxC
			Map<Integer, Double> cPMF = computeUniformPMF(minC, maxC);

			// Multiply probabilities by pAGreaterB
			for (Map.Entry<Integer, Double> entry : cPMF.entrySet())
			{
				int w = entry.getKey();
				double pC = entry.getValue();
				double pW = pAGreaterB * pC;

				pmf.put(w, pmf.getOrDefault(w, 0.0) + pW);
			}

			return pmf;
		}
	}

	// List to hold the PMF groups
	private List<PMFGroup> pmfGroups;

	// Target sum and comparison operator
	private int targetSum;
	private ComparisonOperator comparisonOperator;

	public ProbabilityCalculator()
	{
		pmfGroups = new ArrayList<>();
	}

	public void add(PMFGroup group)
	{
		pmfGroups.add(group);
	}

	private static Map<Integer, Double> computeUniformPMF(int minValue, int maxValue)
	{
		Map<Integer, Double> pmf = new HashMap<>();
		int numOutcomes = maxValue - minValue + 1;
		double probability = 1.0 / numOutcomes;
		for (int value = minValue; value <= maxValue; value++)
		{
			pmf.put(value, probability);
		}
		return pmf;
	}

	private static double computePAGreaterB(int minA, int maxA, int minB, int maxB)
	{
		int nA = maxA - minA + 1;
		int nB = maxB - minB + 1;

		double pAGreaterB = (2.0 * nA - nB - 1) / (2.0 * nA);
		return pAGreaterB;
	}

	// Method to set the target sum and comparison operator
	public void setSumToCalculate(int sum, ComparisonOperator operator)
	{
		this.targetSum = sum;
		this.comparisonOperator = operator;
	}

	// Method to compute the exact probability
	public double getProbability()
	{
		// Combine all group PMFs
		Map<Integer, Double> totalPMF = combineGroupPMFs();

		// Compute the probability based on the comparison operator
		double probability = computeProbability(totalPMF);

		return probability;
	}

	// Method to compute the probability via simulation
	public double getProbabilityBySim(int numSimulations)
	{
		int successCount = 0;

		Random random = new Random();

		for (int sim = 0; sim < numSimulations; sim++)
		{
			int totalSum = 0;

			for (PMFGroup group : pmfGroups)
			{
				if (group instanceof RandomRollGroup)
				{
					RandomRollGroup rollGroup = (RandomRollGroup) group;
					int groupSum = 0;
					int minValue = rollGroup.minValue;
					int maxValue = rollGroup.maxValue;
					int count = rollGroup.count;
					int range = maxValue - minValue + 1;

					for (int i = 0; i < count; i++)
					{
						int roll = random.nextInt(range) + minValue;
						groupSum += roll;
					}

					totalSum += groupSum;
				}
				else if (group instanceof RandomComparedRollGroup)
				{
					RandomComparedRollGroup comparedGroup = (RandomComparedRollGroup) group;
					int min1 = comparedGroup.min1;
					int max1 = comparedGroup.max1;
					int min2 = comparedGroup.min2;
					int max2 = comparedGroup.max2;
					int count = comparedGroup.count;

					int range1 = max1 - min1 + 1;
					int range2 = max2 - min2 + 1;

					int groupSum = 0;
					for (int i = 0; i < count; i++)
					{
						int roll1 = random.nextInt(range1) + min1;
						int roll2 = random.nextInt(range2) + min2;
						groupSum += Math.min(roll1, roll2);
					}
					totalSum += groupSum;
				}
				else if (group instanceof RandomAccuracyCheckedComparedRollGroup)
				{
					RandomAccuracyCheckedComparedRollGroup comparedGroup = (RandomAccuracyCheckedComparedRollGroup) group;
					int minA = comparedGroup.minA;
					int maxA = comparedGroup.maxA;
					int minB = comparedGroup.minB;
					int maxB = comparedGroup.maxB;
					int minC = comparedGroup.minC;
					int maxC = comparedGroup.maxC;
					int minD = comparedGroup.minD;
					int maxD = comparedGroup.maxD;
					int count = comparedGroup.count;

					int rangeA = maxA - minA + 1;
					int rangeB = maxB - minB + 1;
					int rangeC = maxC - minC + 1;
					int rangeD = maxD - minD + 1;

					int groupSum = 0;
					for (int i = 0; i < count; i++)
					{
						int rollA = random.nextInt(rangeA) + minA;
						int rollB = random.nextInt(rangeB) + minB;

						if (rollA > rollB)
						{
							int rollC = random.nextInt(rangeC) + minC;
							int rollD = random.nextInt(rangeD) + minD;
							groupSum += Math.min(rollC, rollD);
						}
						else
						{
							groupSum += 0;
						}
					}
					totalSum += groupSum;
				}
				else if (group instanceof RandomAccuracyCheckedRollGroup)
				{
					RandomAccuracyCheckedRollGroup rollGroup = (RandomAccuracyCheckedRollGroup) group;
					int minA = rollGroup.minA;
					int maxA = rollGroup.maxA;
					int minB = rollGroup.minB;
					int maxB = rollGroup.maxB;
					int minC = rollGroup.minC;
					int maxC = rollGroup.maxC;
					int count = rollGroup.count;

					int rangeA = maxA - minA + 1;
					int rangeB = maxB - minB + 1;
					int rangeC = maxC - minC + 1;

					int groupSum = 0;
					for (int i = 0; i < count; i++)
					{
						int rollA = random.nextInt(rangeA) + minA;
						int rollB = random.nextInt(rangeB) + minB;

						if (rollA > rollB)
						{
							int rollC = random.nextInt(rangeC) + minC;
							groupSum += rollC;
						}
						else
						{
							groupSum += 0;
						}
					}
					totalSum += groupSum;
				}
			}

			// Check if totalSum satisfies the condition
			boolean success = false;

			switch (comparisonOperator)
			{
				case LESS_THAN:
					if (totalSum < targetSum) success = true;
					break;
				case LESS_THAN_OR_EQUAL:
					if (totalSum <= targetSum) success = true;
					break;
				case EQUAL:
					if (totalSum == targetSum) success = true;
					break;
				case GREATER_THAN_OR_EQUAL:
					if (totalSum >= targetSum) success = true;
					break;
				case GREATER_THAN:
					if (totalSum > targetSum) success = true;
					break;
			}

			if (success)
			{
				successCount++;
			}
		}

		double probability = (double) successCount / numSimulations;
		return probability;
	}

	// Compute the PMF of the sum of 'count' iid discrete uniform variables from minValue to maxValue
	private static Map<Integer, Double> computeSumPMF(int minValue, int maxValue, int count)
	{
		Map<Integer, Double> pmf = new HashMap<>();

		// Base PMF for one roll
		Map<Integer, Double> basePMF = new HashMap<>();
		int numOutcomes = maxValue - minValue + 1;
		double probability = 1.0 / numOutcomes;
		for (int value = minValue; value <= maxValue; value++)
		{
			basePMF.put(value, probability);
		}

		// If count == 1, return basePMF
		if (count == 1)
		{
			return basePMF;
		}

		// Initialize pmf with basePMF
		pmf.putAll(basePMF);

		// Convolve pmf with basePMF (count - 1) times
		for (int i = 1; i < count; i++)
		{
			pmf = convolve(pmf, basePMF);
		}

		return pmf;
	}

	// Compute the PMF of the min of two discrete uniform variables
	private static Map<Integer, Double> computeMinUniformPMF(int min1, int max1, int min2, int max2)
	{
		Map<Integer, Double> pmf = new HashMap<>();

		int wMin = Math.min(min1, min2);
		int wMax = Math.max(max1, max2);

		int range1 = max1 - min1 + 1;
		int range2 = max2 - min2 + 1;

		for (int w = wMin; w <= wMax; w++)
		{
			double pXgeqW = (w <= min1) ? 1.0 : (w > max1) ? 0.0 : (double) (max1 - w + 1) / range1;
			double pYgeqW = (w <= min2) ? 1.0 : (w > max2) ? 0.0 : (double) (max2 - w + 1) / range2;

			double pXgeqWp1 = (w + 1 <= min1) ? 1.0 : (w + 1 > max1) ? 0.0 : (double) (max1 - (w + 1) + 1) / range1;
			double pYgeqWp1 = (w + 1 <= min2) ? 1.0 : (w + 1 > max2) ? 0.0 : (double) (max2 - (w + 1) + 1) / range2;

			double pWgeqW = pXgeqW * pYgeqW;
			double pWgeqWp1 = pXgeqWp1 * pYgeqWp1;

			double pW_eq_w = pWgeqW - pWgeqWp1;

			if (pW_eq_w > 0)
			{
				pmf.put(w, pW_eq_w);
			}
		}

		return pmf;
	}

	// Method to convolve two PMFs
	private static Map<Integer, Double> convolve(Map<Integer, Double> pmf1, Map<Integer, Double> pmf2)
	{
		Map<Integer, Double> result = new HashMap<>();

		for (Map.Entry<Integer, Double> entry1 : pmf1.entrySet())
		{
			int sum1 = entry1.getKey();
			double prob1 = entry1.getValue();
			for (Map.Entry<Integer, Double> entry2 : pmf2.entrySet())
			{
				int sum2 = entry2.getKey();
				double prob2 = entry2.getValue();

				int sum = sum1 + sum2;
				double prob = prob1 * prob2;

				result.put(sum, result.getOrDefault(sum, 0.0) + prob);
			}
		}

		return result;
	}

	// Combine PMFs of all groups
	private Map<Integer, Double> combineGroupPMFs()
	{
		if (pmfGroups.isEmpty())
		{
			return new HashMap<>();
		}

		Map<Integer, Double> totalPMF = null;

		for (PMFGroup group : pmfGroups)
		{
			Map<Integer, Double> pmf = group.getPMF();
			if (totalPMF == null)
			{
				totalPMF = new HashMap<>(pmf);
			}
			else
			{
				totalPMF = convolve(totalPMF, pmf);
			}
		}

		return totalPMF;
	}

	// Compute the probability based on the comparison operator
	private double computeProbability(Map<Integer, Double> totalPMF)
	{
		double probability = 0.0;

		for (Map.Entry<Integer, Double> entry : totalPMF.entrySet())
		{
			int sum = entry.getKey();
			double prob = entry.getValue();

			boolean include = false;

			switch (comparisonOperator)
			{
				case LESS_THAN:
					if (sum < targetSum) include = true;
					break;
				case LESS_THAN_OR_EQUAL:
					if (sum <= targetSum) include = true;
					break;
				case EQUAL:
					if (sum == targetSum) include = true;
					break;
				case GREATER_THAN_OR_EQUAL:
					if (sum >= targetSum) include = true;
					break;
				case GREATER_THAN:
					if (sum > targetSum) include = true;
					break;
			}

			if (include)
			{
				probability += prob;
			}
		}

		return probability;
	}
}

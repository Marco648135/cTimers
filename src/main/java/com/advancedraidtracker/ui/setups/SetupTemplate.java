package com.advancedraidtracker.ui.setups;

import java.util.List;
import lombok.Value;

@Value
public class SetupTemplate
{
	int runeA;
	int runeB;
	int runeC;
	int runeD;
	List<Integer> inventory;
	List<Integer> equipment;
	String label;
}

package com.advancedraidtracker.ui.dpsanalysis;

import java.awt.image.BufferedImage;
import java.util.List;
import lombok.Setter;
import lombok.Value;

@Value
public class NPCData
{
	int id;
	String name;
	String version;
	String image;
	int level;
	int speed;
	List<String> style;
	int size;
	String max_hit;
	SkillData skills;
	OffensiveData offensive;
	DefensiveData defensive;
	List<Object> attributes;
	Object weakness;
}

package com.advancedraidtracker.utility;

import com.advancedraidtracker.constants.RaidRoom;
import com.advancedraidtracker.utility.weapons.PlayerAnimation;
import lombok.Value;

@Value
public class PlayerAttack
{
	PlayerAnimation playerAnimation;
	String player;
	int tick;
	RaidRoom room;
}

package com.advancedraidtracker.utility;

import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;
@Value
public class PlayerDataChanged extends PartyMemberMessage
{
	 String username;
	 DataType changeType;
	 int newValue;
	 int roomTick;
}

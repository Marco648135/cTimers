package com.advancedraidtracker.rooms.cox;

import com.advancedraidtracker.AdvancedRaidTrackerConfig;
import com.advancedraidtracker.utility.datautility.DataWriter;
import net.runelite.api.Client;

public class VasaHandler extends CoxRoomHandler
{
    public VasaHandler(Client client, DataWriter clog, AdvancedRaidTrackerConfig config)
    {
        super(client, clog, config);
    }
}

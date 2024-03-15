package com.advancedraidtracker.utility.datautility.datapoints.tob;

import com.advancedraidtracker.constants.RaidRoom;
import com.advancedraidtracker.utility.datautility.DataPoint;
import com.advancedraidtracker.utility.datautility.datapoints.RoomParser;

import java.util.LinkedHashMap;
import java.util.Map;

public class MaidenParser extends RoomParser
{
    @Override
    public Map<Integer, String> getLines()
    {
        //todo maiden crabs (actually goes in chartframe but im too tired to find that class rn)
        lines.put(data.get(DataPoint.MAIDEN_70_SPLIT), "70s");
        lines.put(data.get(DataPoint.MAIDEN_50_SPLIT), "50s");
        lines.put(data.get(DataPoint.MAIDEN_30_SPLIT), "30s");
        lines.put(data.get(DataPoint.MAIDEN_TIME), "Dead");
        return lines;
    }

    public void init()
    {
        data.init(RaidRoom.MAIDEN);
    }
}
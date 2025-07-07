package com.advancedraidtracker.utility.datautility.datapoints.tob;

import com.advancedraidtracker.constants.RaidRoom;
import com.advancedraidtracker.utility.RoomUtil;
import com.advancedraidtracker.utility.datautility.DataPoint;
import com.advancedraidtracker.utility.datautility.datapoints.Raid;
import com.advancedraidtracker.utility.datautility.datapoints.RoomParser;

import javax.xml.crypto.Data;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MaidenParser extends RoomParser
{
    public MaidenParser(Raid data)
    {
        super(data);
    }

    @Override
    public int getFirstPossibleNonIdleTick()
    {
        return 5;
    }

    @Override
    public List<Integer> getRoomAutos() {
        if (autos.isEmpty())
        {
            autos.addAll(data.getList(DataPoint.MAIDEN_AUTOS));
        }
        return autos;
    }

    @Override
    public Map<Integer, String> getLines()
    {
        lines.put(data.get(DataPoint.MAIDEN_70_SPLIT), "70s - " + RoomUtil.time(data.get(DataPoint.MAIDEN_70_SPLIT)));
        lines.put(data.get(DataPoint.MAIDEN_50_SPLIT), "50s - " + RoomUtil.time(data.get(DataPoint.MAIDEN_7050_DURATION)));
        lines.put(data.get(DataPoint.MAIDEN_30_SPLIT), "30s - " + RoomUtil.time(data.get(DataPoint.MAIDEN_5030_SPLIT)));
        lines.put(data.get(DataPoint.MAIDEN_TIME), "Dead - " + RoomUtil.time(data.get(DataPoint.MAIDEN_SKIP_SPLIT)));
        return lines;
    }
}

package com.advancedraidtracker.utility.datautility.datapoints;

import com.advancedraidtracker.constants.RaidRoom;
import com.advancedraidtracker.utility.datautility.DataPoint;
import com.advancedraidtracker.utility.wrappers.PlayerDidAttack;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class RoomParser
{
    public abstract Map<Integer, String> getLines();

    protected Map<Integer, String> lines = new LinkedHashMap<>();
    public RoomDataManager data;
    protected RaidRoom room;

    public RoomParser()
    {
        room = RaidRoom.ANY;
        data = new RoomDataManager();
        data.init(room);
    }

    protected void addLinesFromCollection(List<Integer> list, String description)
    {
        for(Integer i : list)
        {
            lines.put(i, description);
        }
    }

    public void init()
    {
        data.init(room);
    }

    public int getRoomTime()
    {
        return 1; //hmm
    }
    public int getStartTick()
    {
        return 1;
    }
    public int getEndTick()
    {
        return getRoomTime();
    }
}
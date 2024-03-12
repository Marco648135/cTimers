package com.advancedraidtracker.constants;

import com.advancedraidtracker.utility.datautility.*;

public class ParseObject
{
    public ParseType type;
    public DataPoint dataPoint1;
    public DataPoint dataPoint2;
    public DataPoint dataPoint3;
    public String marker;
    public int value;
    public String pointType = "none";

    ParseObject(ParseType type) //MANUAL PARSE, RAID AGNOSTIC PARSE, RAID START RAID END
    {
        this.type = type;
    }
    ParseObject(ParseType type, DataPoint split, DataPoint duration, DataPoint oldValue)
    {
        this.type = type;
        this.dataPoint1 = split;
        this.dataPoint2 = duration;
        this.dataPoint3 = oldValue;
        this.pointType = DataPoint.class.getName();
    }

    ParseObject(ParseType type, DataPoint point) //INCREMENT SET ADD_TO_VALUE
    {
        this.type = type;
        this.dataPoint1 = point;
        this.pointType = DataPoint.class.getName();
    }

    ParseObject(ParseType type, DataPoint point, String marker, int value) //increment if greater than / less than
    {
        this.type = type;
        this.dataPoint1 = point;
        this.marker = marker;
        this.value = value;
        this.pointType = DataPoint.class.getName();
    }

    ParseObject(ParseType type, DataPoint point, int value)
    {
        this.type = type;
        this.dataPoint1 = point;
        this.value = value;
        this.pointType = DataPoint.class.getName();
    }

    public ParseObject(ParseType parseType, DataPoint dataPoint, DataPoint dataPoint2, int i)
    {
        this.type = parseType;
        this.dataPoint1 = dataPoint;
        this.dataPoint2 = dataPoint2;
        this.pointType = DataPoint.class.getName();
        this.value = i;
    }

}

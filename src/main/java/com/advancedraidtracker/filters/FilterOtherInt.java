package com.advancedraidtracker.filters;

import com.advancedraidtracker.SimpleTOBData;
import com.advancedraidtracker.utility.datautility.DataPoint;

public class FilterOtherInt extends FilterCondition
{
    DataPoint param;
    int qualifier;
    int value;
    public String stringValue;

    public FilterOtherInt(DataPoint param, int qualifier, int value, String val)
    {
        this.param = param;
        this.qualifier = qualifier;
        this.value = value;
        stringValue = val;
    }

    @Override
    public String toString()
    {
        return stringValue;
    }

    @Override
    public boolean evaluate(SimpleTOBData data)
    {
        int checkValue = data.getValue(param);
        if (data.getTimeAccurate(param) && value != -1)
        {
            return FilterUtil.compare(qualifier, value, checkValue);

        } else
        {
            return false;
        }
    }

    public String getFilterCSV()
    {
        return "1-" + param.name + "-" + qualifier + "-" + value + "-" + stringValue;
    }
}

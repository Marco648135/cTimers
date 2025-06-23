package com.advancedraidtracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

import java.util.Arrays;
import java.util.Arrays;

public class AdvancedRaidTrackerPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(AdvancedRaidTrackerPlugin.class);
        String[] debugArgs = Arrays.copyOf(args, args.length + 1);
        debugArgs[args.length] = "--developer-mode";
        RuneLite.main(debugArgs);
    }
}
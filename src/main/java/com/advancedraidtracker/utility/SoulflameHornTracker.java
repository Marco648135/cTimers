package com.advancedraidtracker.utility;

import com.advancedraidtracker.AdvancedRaidTrackerPlugin;
import com.advancedraidtracker.ui.charts.chartelements.SoulflameOutlineBox;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.gameval.SpotanimID;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

public class SoulflameHornTracker {

    private AdvancedRaidTrackerPlugin plugin;
    private Client client;
    public SoulflameHornTracker(AdvancedRaidTrackerPlugin plugin, Client client)
    {
        this.plugin = plugin;
        this.client = client;
    }

    public void onGraphicChanged(GraphicChanged event)
    {
        try {
            if (plugin.getRoomTick() > 0 && event.getActor() != null && event.getActor() instanceof Player && event.getActor().getSpotAnims() != null) {
                for (ActorSpotAnim spotAnim : event.getActor().getSpotAnims()) {
                    if (spotAnim.getId() == SpotanimID.VFX_SOULFLAME_HORN_IMPACT_SPOTANIM01) {
                        String roomName = "unknown";
                        if (plugin.getCurrentRoom() != null) {
                            roomName = plugin.getCurrentRoom().getName();
                        }
                        SoulflameOutlineBox sob = new SoulflameOutlineBox(event.getActor().getName(), plugin.getRoomTick(), roomName);
                        plugin.sendChatMessage("<col=EF1020>" + event.getActor().getName() + "<col=ffffff> was buffed by a Soulflame Horn");
                        plugin.addSoulflameOutlineBox(sob);
                    }
                }
            }
        } catch (Exception e) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "Something went wrong while tracking the soulflame horn", null, "");
            e.printStackTrace();
        }
    }

    public void addSelfHorn(String playerName, String roomName) {
        SoulflameOutlineBox sob = new SoulflameOutlineBox(playerName, plugin.getRoomTick(), roomName);
        plugin.sendChatMessage("<col=EF1020>" + playerName + "<col=ffffff> has horned.");
        plugin.addSoulflameOutlineBox(sob);
    }
}

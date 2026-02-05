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
            if (plugin.getRoomTick() <= 0 || event.getActor() == null || !(event.getActor() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getActor();
            String playerName = player.getName();

            // Player name can be null during loading
            if (playerName == null) {
                return;
            }

            var spotAnims = player.getSpotAnims();
            if (spotAnims == null) {
                return;
            }

            // Check for soulflame horn
            for (ActorSpotAnim spotAnim : spotAnims) {
                if (spotAnim != null && spotAnim.getId() == SpotanimID.VFX_SOULFLAME_HORN_IMPACT_SPOTANIM01) {
                    String roomName = "unknown";
                    if (plugin.getCurrentRoom() != null) {
                        roomName = plugin.getCurrentRoom().getName();
                    }
                    SoulflameOutlineBox sob = new SoulflameOutlineBox(playerName, plugin.getRoomTick(), roomName);
                    plugin.sendChatMessage("<col=EF1020>" + playerName + "<col=ffffff> was buffed by a Soulflame Horn");
                    plugin.addSoulflameOutlineBox(sob);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Error tracking soulflame horn: " + e.getMessage(), "");
        }
    }

    public void addSelfHorn(String playerName, String roomName) {
        SoulflameOutlineBox sob = new SoulflameOutlineBox(playerName, plugin.getRoomTick(), roomName);
        plugin.sendChatMessage("<col=EF1020>" + playerName + "<col=ffffff> has horned.");
        plugin.addSoulflameOutlineBox(sob);
    }
}

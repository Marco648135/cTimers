package com.advancedraidtracker.utility;

import com.advancedraidtracker.AdvancedRaidTrackerPlugin;
import com.advancedraidtracker.ui.charts.chartelements.SoulflameOutlineBox;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.gameval.SpotanimID;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

public class SoulflameHornTracker {

    private AdvancedRaidTrackerPlugin plugin;
    private Client client;
    private ArrayList<Pair<String, Integer>> hornBuffedPlayers = new ArrayList<>();

    public SoulflameHornTracker(AdvancedRaidTrackerPlugin plugin, Client client)
    {
        this.plugin = plugin;
        this.client = client;
        this.hornBuffedPlayers = new ArrayList<>();
    }

    public void onGraphicChanged(GraphicChanged event)
    {
        if (plugin.getRoomTick() > 0 && event.getActor() != null && event.getActor() instanceof Player && event.getActor().getSpotAnims() != null) {
            for (ActorSpotAnim spotAnim : event.getActor().getSpotAnims()) {
                if (spotAnim.getId() == SpotanimID.VFX_SOULFLAME_HORN_IMPACT_SPOTANIM01) {
                    System.out.println(event.getActor().getName() + " was buffed by the horn");
                    hornBuffedPlayers.add(Pair.of(event.getActor().getName(), client.getTickCount()));

                    SoulflameOutlineBox sob = new SoulflameOutlineBox(event.getActor().getName(), plugin.getRoomTick());
                    plugin.sendChatMessage("<col=EF1020>" + event.getActor().getName() + "<col=ffffff> was buffed by a Soulflame Horn");
                    plugin.addSoulflameOutlineBox(sob);
                }
            }
        }
    }
}

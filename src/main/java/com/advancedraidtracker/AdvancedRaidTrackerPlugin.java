package com.advancedraidtracker;

import com.advancedraidtracker.constants.*;
import com.advancedraidtracker.rooms.RoomHandler;
import com.advancedraidtracker.rooms.col.ColosseumHandler;
import com.advancedraidtracker.rooms.cox.*;
import com.advancedraidtracker.rooms.inf.InfernoHandler;
import com.advancedraidtracker.rooms.toa.*;
import com.advancedraidtracker.rooms.tob.*;
import com.advancedraidtracker.ui.charts.ChartIO;
import com.advancedraidtracker.ui.charts.ChartTheme;
import com.advancedraidtracker.ui.charts.LiveChart;
import com.advancedraidtracker.ui.RaidTrackerSidePanel;
import com.advancedraidtracker.ui.charts.chartelements.OutlineBox;
import com.advancedraidtracker.utility.*;
import static com.advancedraidtracker.utility.DataType.ATTACK;
import static com.advancedraidtracker.utility.DataType.PRAYER;
import static com.advancedraidtracker.utility.DataType.RING;
import static com.advancedraidtracker.utility.DataType.STRENGTH;
import com.advancedraidtracker.utility.datautility.DataReader;
import com.advancedraidtracker.utility.datautility.DataWriter;
import com.advancedraidtracker.utility.thrallvengtracking.*;
import com.advancedraidtracker.utility.wrappers.PlayerCopy;
import com.advancedraidtracker.utility.wrappers.PlayerDidAttack;
import com.advancedraidtracker.utility.wrappers.QueuedPlayerAttackLessProjectiles;
import com.advancedraidtracker.ui.charts.chartelements.ThrallOutlineBox;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.specialcounter.SpecialCounterUpdate;
import net.runelite.client.plugins.specialcounter.SpecialWeapon;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import static com.advancedraidtracker.constants.LogID.*;
import static com.advancedraidtracker.constants.Room.*;
import static com.advancedraidtracker.constants.RaidRoom.NYLOCAS;
import static com.advancedraidtracker.constants.RaidRoom.SOTETSEG;
import static com.advancedraidtracker.constants.RaidRoom.VERZIK;
import static com.advancedraidtracker.constants.RaidRoom.XARPUS;
import static com.advancedraidtracker.constants.TobIDs.*;
import static com.advancedraidtracker.utility.RoomUtil.inRegion;
import static com.advancedraidtracker.utility.datautility.LegacyFileUtility.migrateSavedFilesToZip;
import static com.advancedraidtracker.utility.datautility.LegacyFileUtility.splitLegacyFiles;
import static net.runelite.api.Varbits.TOA_RAID_LEVEL;


@Slf4j
@PluginDescriptor(
        name = "Advanced PVM Tracker",
        description = "Tracking and statistics for various PVM activities",
        tags = {"timers", "tob", "tracker", "time", "theatre", "analytics", "toa", "inferno", "colosseum", "col", "inf", "pvm", "tombs of amascut"}
)
public class AdvancedRaidTrackerPlugin extends Plugin
{
    private NavigationButton navButtonPrimary;
    public DataWriter clog;

    private boolean partyIntact = false;

    @Inject
    private AdvancedRaidTrackerConfig config;

    @Inject
    private ItemManager itemManager;

    public AdvancedRaidTrackerPlugin()
    {
    }

    @Provides
    AdvancedRaidTrackerConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AdvancedRaidTrackerConfig.class);
    }

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    public ClientThread clientThread;

    @Inject
    private PartyService party;

    @Inject
    private Client client;

    private InfernoHandler infernoHandler;
    private ColosseumHandler colloseumHandler;
    private TOBLobbyHandler lobbyTOB;
    private MaidenHandler maiden;
    private BloatHandler bloat;
    private NyloHandler nylo;
    private SotetsegHandler sote;
    private XarpusHandler xarpus;
    private VerzikHandler verzik;
    private TOALobbyHandler lobbyTOA;
    private NexusHandler nexus;
    private CrondisHandler crondis;
    private ZebakHandler zebak;
    private ScabarasHandler scabaras;
    private KephriHandler kephri;
    private ApmekenHandler apmeken;
    private BabaHandler baba;
    private HetHandler het;
    private AkkhaHandler akkha;
    private WardensHandler wardens;
    private TOAHandler toaHandler;

    private COXHandler coxHandler;
    private CrabsHandler crabs;
    private FirstResourceRoomHandler firstResourceRoom;
    private FirstScavengersHandler firstScavengers;
    private GuardiansHandler guardians;
    private IceDemonHandler iceDemon;
    private MuttadileHandler muttadile;
    private MysticsHandler mystics;
    private OlmHandler olm;
    private SecondResourceRoomHandler secondResourceRoom;
    private SecondSavengersHandler secondSavengers;
    private ShamansHandler shamans;
    private TektonHandler tekton;
    private ThievingHandler thieving;
    private TightRopeHandler tightRope;
    private VanguardsHandler vanguards;
    private VasaHandler vasa;
    private VespulaHandler vespula;


    private ArrayList<DamageQueueShell> queuedThrallDamage;

    private ArrayList<QueuedPlayerAttackLessProjectiles> playersAttacked;


    private boolean inTheatre;
    private boolean wasInTheatre;
    private RoomHandler currentRoom;
    int deferredTick;
    public ArrayList<String> currentPlayers;
    public static int scale = -1;
    public boolean verzShieldActive = false;
    public boolean loggingIn = false;

    private ThrallTracker thrallTracker;
    private VengTracker vengTracker;
    private List<PlayerShell> localPlayers;
    private List<ProjectileQueue> activeProjectiles;
    private List<VengDamageQueue> activeVenges;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private EventBus eventBus;

    @Inject
    private WSClient wsClient;

    @Inject
    private ConfigManager configManager;

    @Inject
    private SpriteManager spriteManager;

    Map<Player, Integer> activelyPiping;
    List<Player> wasPiping;
    private List<NPC> wasBarraged = new ArrayList<>();
    private List<WorldPoint> chinSpawned = new ArrayList<>();

    public LiveChart liveFrame;
	private int ringData;
	private int strLevelData;
	private int attLevelData;
	private List<Integer> prayerData;

    @Override
    protected void shutDown()
    {
		wsClient.unregisterMessage(PlayerDataChanged.class);
        partyIntact = false;
        if(currentRoom != null)
        {
            clog.addLine(LEFT_TOB, String.valueOf(client.getTickCount() - currentRoom.roomStartTick), currentRoom.getName());
        }
        clientToolbar.removeNavigation(navButtonPrimary);
		DataReader.parsedFiles.clear();
    }

    public void openLiveFrame()
    {
        liveFrame.open(currentRoom.getName());
    }

    public int getTick()
    {
        return client.getTickCount();
    }

    public boolean isVerzP2()
    {
        if (currentRoom instanceof VerzikHandler)
        {
            VerzikHandler room = (VerzikHandler) currentRoom;
            return room.roomState == RoomState.VerzikRoomState.PHASE_2 || room.roomState == RoomState.VerzikRoomState.PHASE_2_REDS;
        }
        return false;
    }

    private RaidTrackerSidePanel timersPanelPrimary;

	@Inject
	private Gson gson;

    @Override
    protected void startUp() throws Exception
    {
        super.startUp();
        splitLegacyFiles();
        migrateSavedFilesToZip();
        localPlayers = new ArrayList<>();
        thrallTracker = new ThrallTracker(this);
        vengTracker = new VengTracker(this);
        activeProjectiles = new ArrayList<>();
        activeVenges = new ArrayList<>();
        queuedThrallDamage = new ArrayList<>();
        timersPanelPrimary = injector.getInstance(RaidTrackerSidePanel.class);
        partyIntact = false;
        activelyPiping = new LinkedHashMap<>();
        wasPiping = new ArrayList<>();
        liveFrame = new LiveChart(config, itemManager, clientThread, configManager, spriteManager);
        playersTextChanged = new ArrayList<>();
        clog = new DataWriter(config);
		ChartIO.gson = gson;

        final BufferedImage icon = ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/icon.png");
        navButtonPrimary = NavigationButton.builder().tooltip("Advanced Raid Tracker").icon(icon).priority(10).panel(timersPanelPrimary).build();

        clientToolbar.addNavigation(navButtonPrimary);

        colloseumHandler = new ColosseumHandler(client, clog, config, liveFrame, this);

        lobbyTOB = new TOBLobbyHandler(client, clog, config, this);
        maiden = new MaidenHandler(client, clog, config, this, itemManager);
        bloat = new BloatHandler(client, clog, config, this);
        nylo = new NyloHandler(client, clog, config, this);
        sote = new SotetsegHandler(client, clog, config, this);
        xarpus = new XarpusHandler(client, clog, config, this);
        verzik = new VerzikHandler(client, clog, config, this, itemManager);

        toaHandler = new TOAHandler(client, clog);
        lobbyTOA = new TOALobbyHandler(client, clog, config, this, toaHandler);
        nexus = new NexusHandler(client, clog, config, this, toaHandler);
        crondis = new CrondisHandler(client, clog, config, this, toaHandler);
        zebak = new ZebakHandler(client, clog, config, this, toaHandler);
        scabaras = new ScabarasHandler(client, clog, config, this, toaHandler);
        kephri = new KephriHandler(client, clog, config, this, toaHandler);
        apmeken = new ApmekenHandler(client, clog, config, this, toaHandler);
        baba = new BabaHandler(client, clog, config, this, toaHandler);
        het = new HetHandler(client, clog, config, this, toaHandler);
        akkha = new AkkhaHandler(client, clog, config, this, toaHandler, itemManager);
        wardens = new WardensHandler(client, clog, config, this, toaHandler);
        coxHandler = new COXHandler();
        tekton = new TektonHandler(client, clog, config, this);

        infernoHandler = new InfernoHandler(client, clog, config, this);

		wsClient.registerMessage(PlayerDataChanged.class);

        inTheatre = false;
        wasInTheatre = false;
        deferredTick = 0;
        currentPlayers = new ArrayList<>();
        playersAttacked = new ArrayList<>();
		if(client.getLocalPlayer() != null)
		{
			clog.setName(client.getLocalPlayer().getName());
		}
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired e)
    {
        if (inTheatre)
        {
            currentRoom.updateScriptPreFired(e);

        }
    }


    @Subscribe
    public void onChatMessage(ChatMessage message)
    {
        if (inTheatre)
        {
            currentRoom.updateChatMessage(message);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState().equals(GameState.LOGGED_IN))
        {
            try
            {
                loggingIn = true;
            } catch (Exception e)
            {
                log.info("Failed to set name: " + client.getLocalPlayer().getName());
            }
        }
    }

    /**
     * @return Room as int if inside TOB (0 indexed), -1 otherwise
     */
    private Room getRoom()
    {
        for (Room room : Room.values())
        {
            if (inRegion(client, room))
            {
                return room;
            }
        }
        return Room.UNKNOWN;
    }

    private boolean inInferno = false;
    private boolean inColosseum = false;

    private void updateRoom()
    {
        RoomHandler previous = currentRoom;
        boolean activeState = false;
        Room room = getRoom();
        if (!inInferno)
        {
            if (client.isInInstancedRegion() && inRegion(client, 9043))
            {
                currentRoom = infernoHandler;
                infernoHandler.reset();
                infernoHandler.start();
                clog.setRaidType(RaidType.INFERNO);
                clog.migrateToNewRaid();
                clog.addLine(ENTERED_RAID);
                clog.addLine(PARTY_MEMBERS, client.getLocalPlayer().getName());
                liveFrame.switchToInf();
                ArrayList<String> players = new ArrayList<>();
                players.add(client.getLocalPlayer().getName());
                liveFrame.setPlayers(players);
                inTheatre = true;
                inInferno = true;
                activeState = true;
                lastSplits = "";
				currentDurationSum = 0;
            }
        } else
        {
            if (!inRegion(client, 9043))
            {
                infernoHandler.forceStatUpdate();
                if (infernoHandler.lastCompletedWave < 69)
                {
                    lastSplits = infernoHandler.getStatString() + lastSplits + "Duration (Failed): " + RoomUtil.time((client.getTickCount() - infernoHandler.roomStartTick)) + " (+" + RoomUtil.time(client.getTickCount() - infernoHandler.getLastWaveStartTime()) + ")";
                } else
                {
                    lastSplits = infernoHandler.getStatString() + lastSplits + "Duration (Success): " + RoomUtil.time(infernoHandler.getDuration()) + " (+" + RoomUtil.time(client.getTickCount() - infernoHandler.getLastWaveStartTime()) + ")";
                }
                currentRoom = lobbyTOB;
                infernoHandler.reset();
                inTheatre = false;
                inInferno = false;
                liveFrame.resetAll();
            } else
            {
                activeState = true;
            }
        }
        if (!inColosseum)
        {
            if (client.isInInstancedRegion() && inRegion(client, 7216))
            {
                currentRoom = colloseumHandler;
                colloseumHandler.inRegion = true;
                clog.setRaidType(RaidType.COLOSSEUM);
                clog.migrateToNewRaid();
                clog.addLine(ENTERED_RAID);
                clog.addLine(PARTY_MEMBERS, client.getLocalPlayer().getName());
                liveFrame.switchToCol();
                ArrayList<String> players = new ArrayList<>();
                players.add(client.getLocalPlayer().getName());
                liveFrame.setPlayers(players);
                inTheatre = true;
                inColosseum = true;
                activeState = true;
                lastSplits = "";
				currentDurationSum = 0;
            }
        } else
        {
            if (!inRegion(client, 7216))
            {
                colloseumHandler.inRegion = false;
                lastSplits = colloseumHandler.getInvos() + lastSplits;
                if (colloseumHandler.lastCompletedWave < colloseumHandler.currentWave)
                {
                    lastSplits += "Duration (Death): " + RoomUtil.time((client.getTickCount() - colloseumHandler.roomStartTick) + colloseumHandler.timeSum) + " (+" + RoomUtil.time(client.getTickCount() - colloseumHandler.roomStartTick) + ")";
                } else if (colloseumHandler.lastCompletedWave == 12)
                {
                    lastSplits += "Duration (Success): " + RoomUtil.time(colloseumHandler.timeSum) + " (+" + RoomUtil.time(colloseumHandler.lastWaveDuration) + ")";
                } else
                {
                    lastSplits += "Duration (Reset): " + RoomUtil.time(colloseumHandler.timeSum) + " (+" + RoomUtil.time(colloseumHandler.lastWaveDuration) + ")";
                }
                currentRoom = lobbyTOB; //todo idk
                colloseumHandler.reset();
                inTheatre = false;
                inColosseum = false;
                liveFrame.resetAll();
            } else
            {
                activeState = true;
            }
        }
        if (inRegion(client, LOBBY_REGION))
        {
            currentRoom = lobbyTOB;
        } else if (previous == lobbyTOB && inRegion(client, BLOAT_REGION, NYLO_REGION, SOTETSEG_REGION, XARPUS_REGION, VERZIK_REGION))
        {
            clog.setRaidType(RaidType.TOB);
            deferredTick = client.getTickCount() + 2; //Check two ticks from now for player names in orbs
            clog.checkForEndFlag();
            clog.migrateToNewRaid();
            clog.addLine(ENTERED_RAID);
            clog.addLine(SPECTATE);
            clog.addLine(LATE_START, room.name);
            liveFrame.resetAll();
            liveFrame.switchToTOB();
            lastSplits = "";
			currentDurationSum = 0;
        }
        if (inRegion(client, TOA_LOBBY))
        {
            currentRoom = lobbyTOA;
        } else if (previous == lobbyTOA && inRegion(client, TOA_NEXUS))
        {
            clog.setRaidType(RaidType.TOA);
            deferredTick = client.getTickCount() + 5; //check for player names in orbs 5t after entry
            clog.checkForEndFlag();
            clog.migrateToNewRaid();
            clog.addLine(ENTERED_TOA);
            liveFrame.resetAll();
            liveFrame.switchToTOA();
            lastSplits = "";
			currentDurationSum = 0;
        }
        switch (room)
        {
            case MAIDEN:
                if (previous != maiden)
                {
                    clog.setRaidType(RaidType.TOB);
                    currentRoom = maiden;
                    enteredMaiden();
                    liveFrame.resetAll();
                    liveFrame.switchToTOB();
                    lastSplits = "";
					currentDurationSum = 0;
                }
                activeState = true;
                break;
            case BLOAT:
                if (previous != bloat)
                {
                    currentRoom = bloat;
                    enteredBloat();
                }
                activeState = true;
                break;
            case NYLOCAS:
                if (previous != nylo)
                {
                    currentRoom = nylo;
                    enteredNylo();
                }
                activeState = true;
                break;
            case SOTETSEG:
                if (previous != sote)
                {
                    currentRoom = sote;
                    enteredSote();
                }
                activeState = true;
                break;
            case XARPUS:
                if (previous != xarpus)
                {
                    currentRoom = xarpus;
                    enteredXarpus();
                }
                activeState = true;
                break;
            case VERZIK:
                if (previous != verzik)
                {
                    currentRoom = verzik;
                    enteredVerzik();
                }
                activeState = true;
                break;
            case TOA_NEXUS:
                if (previous != nexus)
                {
                    currentRoom = nexus;
                    enteredNexus();
                }
                activeState = true;
                break;
            case CRONDIS:
                if (previous != crondis)
                {
                    currentRoom = crondis;
                    enteredCrondis();
                }
                activeState = true;
                break;
            case ZEBAK:
                if (previous != zebak)
                {
                    currentRoom = zebak;
                    enteredZebak();
                }
                activeState = true;
                break;
            case SCABARAS:
                if (previous != scabaras)
                {
                    currentRoom = scabaras;
                    enteredScabaras();
                }
                activeState = true;
                break;
            case KEPHRI:
                if (previous != kephri)
                {
                    currentRoom = kephri;
                    enteredKephri();
                }
                activeState = true;
                break;
            case APMEKEN:
                if (previous != apmeken)
                {
                    currentRoom = apmeken;
                    enteredApmeken();
                }
                activeState = true;
                break;
            case BABA:
                if (previous != baba)
                {
                    currentRoom = baba;
                    enteredBaba();
                }
                activeState = true;
                break;
            case HET:
                if (previous != het)
                {
                    currentRoom = het;
                    enteredHet();
                }
                activeState = true;
                break;
            case AKKHA:
                if (previous != akkha)
                {
                    currentRoom = akkha;
                    enteredAkkha();
                }
                activeState = true;
                break;
            case WARDENS:
                if (previous != wardens)
                {
                    currentRoom = wardens;
                    enteredWardens();
                }
                activeState = true;
                break;
            case TOMB:
                enteredTomb();
                break;
        }
        inTheatre = activeState;
    }

    private boolean TOAStarted = false;

    private void enteredTomb()
    {
        clog.addLine(ENTERED_NEW_TOA_REGION, "Tomb");
    }

    private void enteredNexus()
    {
        clog.addLine(ENTERED_NEW_TOA_REGION, TOA_NEXUS.name);
        nexus.reset();
    }

    private void enteredCrondis()
    {
        if (!toaHandler.isActive())
        {
            toaHandler.start();
        }
        clog.addLine(ENTERED_NEW_TOA_REGION, CRONDIS.name);
        liveFrame.tabbedPane.setSelectedIndex(4);
        crondis.reset();
    }

    private void enteredZebak()
    {
        clog.addLine(ENTERED_NEW_TOA_REGION, ZEBAK.name);
        liveFrame.tabbedPane.setSelectedIndex(5);
        zebak.reset();
    }

    private void enteredScabaras()
    {
        if (!toaHandler.isActive())
        {
            toaHandler.start();
        }
        clog.addLine(ENTERED_NEW_TOA_REGION, SCABARAS.name);
        liveFrame.tabbedPane.setSelectedIndex(2);
        scabaras.reset();
    }

    private void enteredKephri()
    {
        clog.addLine(ENTERED_NEW_TOA_REGION, KEPHRI.name);
        liveFrame.tabbedPane.setSelectedIndex(3);
        kephri.reset();
    }

    private void enteredApmeken()
    {
        if (!toaHandler.isActive())
        {
            toaHandler.start();
        }
        clog.addLine(ENTERED_NEW_TOA_REGION, APMEKEN.name);
        liveFrame.tabbedPane.setSelectedIndex(0);
        apmeken.reset();
    }

    private void enteredBaba()
    {
        clog.addLine(ENTERED_NEW_TOA_REGION, BABA.name);
        liveFrame.tabbedPane.setSelectedIndex(1);
        baba.reset();
    }

    private void enteredHet()
    {
        if (!toaHandler.isActive())
        {
            toaHandler.start();
        }
        clog.addLine(ENTERED_NEW_TOA_REGION, HET.name);
        liveFrame.tabbedPane.setSelectedIndex(6);
        het.reset();
    }

    private void enteredAkkha()
    {
        clog.addLine(ENTERED_NEW_TOA_REGION, AKKHA.name);
        liveFrame.tabbedPane.setSelectedIndex(7);
        akkha.reset();
    }

    private void enteredWardens()
    {
        clog.addLine(ENTERED_NEW_TOA_REGION, WARDENS.name);
        liveFrame.tabbedPane.setSelectedIndex(8);
        wardens.reset();
    }

    private void leftTOA()
    {
        lastTickPlayer.clear();
        partyIntact = false;
        currentPlayers.clear();
        clog.addLine(LEFT_TOA, String.valueOf(client.getTickCount() - currentRoom.roomStartTick), currentRoom.getName());
        clog.writeFile();
        clog.migrateToNewRaid();
        currentRoom = null;
        activelyPiping.clear();
        deferredAnimations.clear();
    }

    private void enteredMaiden()
    {
        clog.checkForEndFlag();
        clog.migrateToNewRaid();
        clog.addLine(ENTERED_RAID);
        liveFrame.switchToTOB();
        deferredTick = client.getTickCount() + 2;
        maiden.reset();
        liveFrame.tabbedPane.setSelectedIndex(0);
    }

    private void enteredBloat()
    {
        clog.addLine(ENTERED_NEW_TOB_REGION, String.valueOf(RaidRoom.BLOAT.ordinal()));
        maiden.reset();
        bloat.reset();
        liveFrame.tabbedPane.setSelectedIndex(1);
    }

    private void enteredNylo()
    {
        clog.addLine(ENTERED_NEW_TOB_REGION, String.valueOf(NYLOCAS.ordinal()));
        bloat.reset();
        nylo.reset();
        liveFrame.tabbedPane.setSelectedIndex(2);
    }

    private void enteredSote()
    {
        clog.addLine(ENTERED_NEW_TOB_REGION, String.valueOf(SOTETSEG.ordinal()));
        nylo.reset();
        sote.reset();
        liveFrame.tabbedPane.setSelectedIndex(3);
    }

    private void enteredXarpus()
    {
        clog.addLine(ENTERED_NEW_TOB_REGION, String.valueOf(XARPUS.ordinal()));
        sote.reset();
        xarpus.reset();
        liveFrame.tabbedPane.setSelectedIndex(4);
    }

    private void enteredVerzik()
    {
        clog.addLine(ENTERED_NEW_TOB_REGION, String.valueOf(VERZIK.ordinal()));
        xarpus.reset();
        verzik.reset();
        liveFrame.tabbedPane.setSelectedIndex(5);
    }

    @Subscribe
    public void onSpecialCounterUpdate(SpecialCounterUpdate event)
    {
        if (inTheatre)
        {
            String name = party.getMemberById(event.getMemberId()).getDisplayName();
            if (name == null)
            {
                return;
            }
            boolean playerInRaid = false;
            // Ensures correct names across encodings
            for (String player : currentPlayers)
            {
                if (name.equals(player.replaceAll(String.valueOf((char) 160), String.valueOf((char) 32))))
                {
                    playerInRaid = true;
                    break;
                }
            }
            if (playerInRaid)
            {
                if (event.getWeapon().equals(SpecialWeapon.BANDOS_GODSWORD))
                {
                    clog.addLine(BGS_HIT, name, String.valueOf(event.getHit()), String.valueOf(client.getTickCount() - currentRoom.roomStartTick));
                }
                if (event.getWeapon().equals(SpecialWeapon.DRAGON_WARHAMMER))
                {
                    clog.addLine(HAMMER_HIT, name, String.valueOf(client.getTickCount() - currentRoom.roomStartTick));
                }
            }
        }
    }

    private String cleanString(String s1)
    {
        return s1.replaceAll(String.valueOf((char) 160), String.valueOf((char) 32));
    }

    private boolean isPartyComplete()
    {
        if (currentPlayers.size() > party.getMembers().size())
        {
            return false;
        }
        for (String raidPlayer : currentPlayers)
        {
            boolean currentPlayerMatched = false;
            for (PartyMember partyPlayer : party.getMembers())
            {
                if (cleanString(raidPlayer).equals(partyPlayer.getDisplayName()))
                {
                    currentPlayerMatched = true;
                }
            }
            if (!currentPlayerMatched)
            {
                return false;
            }
        }
        return true;
    }

    private void checkPartyUpdate()
    {
        if (inTheatre)
        {
            if (partyIntact)
            {
                if (!isPartyComplete())
                {
                    partyIntact = false;
                    clog.addLine(PARTY_INCOMPLETE);
                }
            } else
            {
                if (isPartyComplete())
                {
                    partyIntact = true;
                    clog.addLine(PARTY_COMPLETE);
                }
            }
        }

    }

    @Subscribe
    public void onPartyChanged(final PartyChanged party)
    {

        checkPartyUpdate();
    }

    @Subscribe
    public void onUserPart(final UserPart event)
    {
        checkPartyUpdate();
    }

    @Subscribe
    public void onUserJoin(final UserJoin event)
    {
        checkPartyUpdate();
    }

    public void addQueuedThrallDamage(int targetIndex, int sourceIndex, int offset, String source)
    {
        queuedThrallDamage.add(new DamageQueueShell(targetIndex, sourceIndex, offset, source, client.getTickCount()));
    }

    public void removeDeadProjectiles()
    {
        activeProjectiles.removeIf(projectileQueue -> projectileQueue.finalTick <= client.getTickCount());
    }

    public void removeDeadVenges()
    {
        activeVenges.removeIf(vengDamageQueue -> vengDamageQueue.appliedTick <= client.getTickCount());
    }

    public void addDelayedLine(RaidRoom room, int value, String description)
    {
        switch (RaidRoom.valueOf(room.value))
        {
            case MAIDEN:
                liveFrame.addLine("Maiden", value, description);
                break;
            case BLOAT:
                liveFrame.addLine("Bloat", value, description);
                break;
            case NYLOCAS:
                liveFrame.addLine("Nylocas", value, description);
                break;
            case SOTETSEG:
                liveFrame.addLine("Sotetseg", value, description);
                break;
            case XARPUS:
                liveFrame.addLine("Xarpus", value, description);
                break;
            case VERZIK:
                liveFrame.addLine("Verzik", value, description);
                break;
        }
    }

    public void thrallAttackedP2VerzikShield(int tickOffset)
    {
        if (currentRoom instanceof VerzikHandler)
        {
            VerzikHandler room = (VerzikHandler) currentRoom;
            room.thrallAttackedShield(client.getTickCount() + tickOffset);
        }
    }

    public void removeThrallBox(Thrall thrall)
    {
        clog.addLine(THRALL_DESPAWN, thrall.getOwner(), String.valueOf(client.getTickCount() - currentRoom.roomStartTick));
    }

    public void addThrallOutlineBox(ThrallOutlineBox outlineBox)
    {
        clog.addLine(THRALL_SPAWN, outlineBox.owner, String.valueOf(outlineBox.spawnTick), String.valueOf(outlineBox.id), currentRoom.getName());
        liveFrame.getPanel(currentRoom.getName()).addThrallBox(outlineBox);
    }

    public Map<String, PlayerCopy> lastTickPlayer = new HashMap<>();

    public int getRoomTick()
    {
        return client.getTickCount() - currentRoom.roomStartTick;
    }

    private void handleBarraged()
    {
        for (NPC npc : wasBarraged)
        {
            List<Player> potentialPlayers = new ArrayList<>();
            for (Player player : client.getPlayers())
            {
                if (player.getInteracting() != null)
                {
                    if (player.getInteracting().equals(npc) && (player.getAnimation() == 1979 || player.getAnimation() == 1978 || player.getAnimation() == BLOWPIPE_ANIMATION || player.getAnimation() == BLOWPIPE_ANIMATION_OR))
                    {
                        potentialPlayers.add(player);
                    }
                }
            }
            if (potentialPlayers.size() == 1)
            {
                if (potentialPlayers.get(0).getAnimation() == BLOWPIPE_ANIMATION || potentialPlayers.get(0).getAnimation() == BLOWPIPE_ANIMATION_OR)
                {
                    generatePlayerAttackInfo(potentialPlayers.get(0), 1979);
                }
            }
        }
        wasBarraged.clear();
    }

    private void handleChinSpawns()
    {
        for (WorldPoint wp : chinSpawned)
        {
            List<PlayerCopy> potentialPlayers = new ArrayList<>();
            for (PlayerCopy player : lastTickPlayer.values())
            {
                if (player.worldPoint.distanceTo(wp) == 0)
                {
                    potentialPlayers.add(player);
                }
            }
            if (potentialPlayers.size() == 1)
            {
                for (Player p : client.getPlayers())
                {
                    if (Objects.equals(p.getName(), potentialPlayers.get(0).name))
                    {
                        if (p.getAnimation() != BLOWPIPE_ANIMATION_OR && p.getAnimation() != BLOWPIPE_ANIMATION)
                        {
                            PlayerCopy previous = potentialPlayers.get(0);
                            clog.addLine(PLAYER_ATTACK,
                                    previous.name + ":" + (client.getTickCount() - currentRoom.roomStartTick - 1),
                                    7618 + ":" + previous.wornItems,
                                    "",
                                    previous.weapon + ":" + previous.interactingIndex + ":" + previous.interactingID,
                                    "-1:" + previous.interactingName, currentRoom.getName());
                            liveFrame.addAttack(new PlayerDidAttack(itemManager,
                                    previous.name,
                                    String.valueOf(7618),
                                    -1,
                                    previous.weapon,
                                    "-1",
                                    "",
                                    previous.interactingIndex,
                                    previous.interactingID,
                                    previous.interactingName,
                                    previous.wornItems
                            ), currentRoom.getName());
                        }
                    }
                }
            }
        }
        chinSpawned.clear();
    }

    private void checkGraphics()
    {
        for(Player p : deferredGraphics)
        {
            if(!deferredAnimations.contains(p))
            {
                generatePlayerAttackInfo(p, -2);
            }
        }
        deferredGraphics.clear();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (loggingIn)
        {
            try
            {
                clog.setName(client.getLocalPlayer().getName());
            } catch (Exception e)
            {
                log.info("failed to set name?");
            }
            loggingIn = false;
        }

        handleBarraged();
        handleChinSpawns();
        wasPiping.clear();
        checkGraphics();
        checkAnimationsThatChanged();
        checkOverheadTextsThatChanged();
        checkActivelyPiping();
        handleQueuedProjectiles();
        removeDeadProjectiles();
        removeDeadVenges();
        updateThrallVengTracker();

        updateRoom();
        if (inTheatre)
        {
			for(String p : scythedPreviousTick)
			{
				if(playerDataMap.get(p) != null)
				{
					if (playerDataMap.get(p).getStrengthLevel() != 118)
					{
						sendChatMessage(p + " scythed with: " + playerDataMap.get(p).getStrengthLevel() + " strength");
					}
					if (!playerDataMap.get(p).getPrayers().get(Prayer.PIETY))
					{
						sendChatMessage(p + " scythed without piety");
					}
				}
				else
				{
					log.info("not found");
				}
			}
			scythedPreviousTick.clear();
			scythedPreviousTick.addAll(scythedThisTick);
			scythedThisTick.clear();

            wasInTheatre = true;
            currentRoom.updateGameTick(event);
            if (currentRoom.isActive())
            {
				checkChangedPartyData();
				SwingUtilities.invokeLater(() ->
				{
					liveFrame.incrementTick(currentRoom.getName());
				});
                int HP_VARBIT = 6448;
                liveFrame.getPanel(currentRoom.getName()).addRoomHP(client.getTickCount() - currentRoom.roomStartTick, client.getVarbitValue(HP_VARBIT));
                clog.addLine(UPDATE_HP, String.valueOf(client.getVarbitValue(HP_VARBIT)), String.valueOf(client.getTickCount() - currentRoom.roomStartTick), currentRoom.getName());
            }
			else
			{
				ringData = -1;
				attLevelData = -1;
				strLevelData = -1;
				prayersActivelyBeingTracked = false;
				playerDataMap.clear();
			}
            if (client.getTickCount() == deferredTick)
            {
                if (currentRoom instanceof NexusHandler)
                {
                    String[] players = {"", "", "", "", "", "", "", ""};
                    int varcStrID = 1099;
                    for (int i = varcStrID; i < varcStrID + 8; i++)
                    {
                        if (client.getVarcStrValue(i) != null && !client.getVarcStrValue(i).isEmpty())
                        {
                            players[i - varcStrID] = client.getVarcStrValue(i).replaceAll(String.valueOf((char) 160), String.valueOf((char) 32));
                        }
                    }
                    for (String s : players)
                    {
                        if (!s.isEmpty())
                        {
                            currentPlayers.add(s);
                        }
                    }
                    liveFrame.setPlayers(currentPlayers);
                    clog.addLine(PARTY_MEMBERS, players[0], players[1], players[2], players[3], players[4], players[5], players[6], players[7]);
                    clog.addLine(INVOCATION_LEVEL, String.valueOf(client.getVarbitValue(TOA_RAID_LEVEL)));
                } else
                {
                    String[] players = {"", "", "", "", ""};
                    int varcStrID = 330; // Widget for player names
                    for (int i = varcStrID; i < varcStrID + 5; i++)
                    {
                        if (client.getVarcStrValue(i) != null && !client.getVarcStrValue(i).isEmpty())
                        {
                            players[i - varcStrID] = Text.escapeJagex(client.getVarcStrValue(i));
                        }
                    }
                    for (String s : players)
                    {
                        if (!s.isEmpty())
                        {
                            currentPlayers.add(s.replaceAll(String.valueOf((char) 160), String.valueOf((char) 32)));
                        }
                    }
                    liveFrame.setPlayers(currentPlayers);
                    checkPartyUpdate();
                    boolean flag = false;
                    for (String p : players)
                    {
                        if (p.replaceAll(String.valueOf((char) 160), String.valueOf((char) 32)).equals(Objects.requireNonNull(client.getLocalPlayer().getName()).replaceAll(String.valueOf((char) 160), String.valueOf((char) 32))))
                        {
                            flag = true;
                        }
                    }
                    deferredTick = 0;
                    if (!flag)
                    {
                        clog.addLine(SPECTATE);
                    }
                    clog.addLine(PARTY_MEMBERS, players[0], players[1], players[2], players[3], players[4]);
                    maiden.setScale((int) Arrays.stream(players).filter(x -> !x.isEmpty()).count());
                    scale = currentPlayers.size();
                }
            }
        } else
        {
            if (wasInTheatre)
            {
                if (currentRoom instanceof TOARoomHandler)
                {
                    leftTOA();
                } else
                {
                    leftRaid();
                }
                wasInTheatre = false;
                return;
            }
        }
        clog.writeFile();
    }

	private final Map<String, PlayerData> playerDataMap = new ConcurrentHashMap<>();

	private void checkChangedPartyData()
	{
		checkRing();
		checkLevels();
		checkPrayers();
	}

	private void checkPrayers()
	{
		for (Prayer prayer : Prayer.values())
		{
			boolean isActive = client.isPrayerActive(prayer);
			Boolean previousState = localPrayers.get(prayer);

			if (previousState == null || previousState != isActive || !prayersActivelyBeingTracked)
			{
				prayersActivelyBeingTracked = true;
				localPrayers.put(prayer, isActive);

				int prayerId = prayer.ordinal(); // Unique ID for the prayer

				String dataValueString = prayerId + (isActive ? "1" : "2");
				int dataValue = Integer.parseInt(dataValueString);

				party.send(new PlayerDataChanged(client.getLocalPlayer().getName(), DataType.PRAYER, dataValue, getRoomTick()));
			}
		}
	}

	private void checkLevels()
	{
		int att = client.getBoostedSkillLevel(Skill.ATTACK);
		int str = client.getBoostedSkillLevel(Skill.STRENGTH);

		if(att != attLevelData)
		{
			party.send(new PlayerDataChanged(client.getLocalPlayer().getName(), ATTACK, att, getRoomTick()));
			//log.info("sending changed attack: " + att);
			attLevelData = att;
		}
		if(str != strLevelData)
		{
			party.send(new PlayerDataChanged(client.getLocalPlayer().getName(), STRENGTH, str, getRoomTick()));
			//log.info("sending changed strength: " + str);
			strLevelData = str;
		}
	}

	private void checkRing()
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		int ring = -1;
		if (equipment != null)
		{
			Item[] equipmentItems = equipment.getItems();
			int ringSlotIndex = EquipmentInventorySlot.RING.getSlotIdx();

			if (ringSlotIndex >= 0 && ringSlotIndex < equipmentItems.length)
			{
				Item ringItem = equipmentItems[ringSlotIndex];
				if (ringItem != null && ringItem.getId() > 0)
				{
					ring = ringItem.getId();
				}
			}
		}
		if(ring != ringData)
		{
			party.send(new PlayerDataChanged(client.getLocalPlayer().getName(), RING, ring, getRoomTick()));
			//log.info("sending changed ring: " + ring);
			ringData = ring;
		}
	}

	Map<Prayer, Boolean> localPrayers = new HashMap<Prayer, Boolean>();
	private boolean prayersActivelyBeingTracked = false;

	private final SetMultimap<String, Prayer> playerActivePrayers = HashMultimap.create();

	@Subscribe
	public void onPlayerDataChanged(PlayerDataChanged e)
	{
		String playerName = e.getUsername();
		DataType dataType = e.getChangeType();
		int dataValue = e.getNewValue();

		clog.addLine(PLAYER_DATA_CHANGED, playerName, e.getChangeType().name, String.valueOf(e.getNewValue()), String.valueOf(e.getRoomTick()));

		// Get or create PlayerData for the player
		PlayerData playerData = playerDataMap.computeIfAbsent(playerName, k -> new PlayerData());

		switch (dataType)
		{
			case PRAYER:
				int prayerId = dataValue / 10;
				int stateIndicator = dataValue % 10;
				boolean isActive = stateIndicator == 1;
				Prayer prayer = Prayer.values()[prayerId];

				playerData.setPrayerState(prayer, isActive);

				//log.info("Player {}: Prayer {} is now {}", playerName, prayer.name(), isActive ? "active" : "inactive");
				break;

			case RING:
				playerData.setRingId(dataValue);
				//log.info("Player {}: Ring ID updated to {}", playerName, dataValue);
				break;

			case ATTACK:
				playerData.setAttackLevel(dataValue);
				//log.info("Player {}: Attack level updated to {}", playerName, dataValue);
				break;

			case STRENGTH:
				playerData.setStrengthLevel(dataValue);
				//log.info("Player {}: Strength level updated to {}", playerName, dataValue);
				break;

			default:
				break;
		}
	}

	private void updateThrallVengTracker()
    {
        playersTextChanged.clear();
        localPlayers.clear();
        for (Player p : client.getPlayers())
        {
            localPlayers.add(new PlayerShell(p.getWorldLocation(), p.getName()));
            thrallTracker.updatePlayerInteracting(p.getName(), p.getInteracting());
        }
        for (DamageQueueShell damage : queuedThrallDamage)
        {
            damage.offset--;
        }
        thrallTracker.updateTick();
        vengTracker.updateTick();
    }

    private void handleQueuedProjectiles()
    {
        for (QueuedPlayerAttackLessProjectiles playerAttackQueuedItem : playersAttacked)
        {
            playerAttackQueuedItem.tick--;
            if (playerAttackQueuedItem.tick == 0)
            {
                for (Projectile projectile : client.getProjectiles())
                {
                    int offset = 41; //zcb
                    if (projectile.getId() == DAWNBRINGER_AUTO_PROJECTILE || projectile.getId() == DAWNBRINGER_SPEC_PROJECTILE)
                    {
                        offset = 51; //dawnbringer
                    }
                    if (projectile.getStartCycle() == client.getGameCycle() + offset)
                    {
                        WorldPoint position = WorldPoint.fromLocal(client, new LocalPoint(projectile.getX1(), projectile.getY1()));
                        if (position.distanceTo(playerAttackQueuedItem.player.getWorldLocation()) == 0)
                        {
                            if (projectile.getId() == DAWNBRINGER_AUTO_PROJECTILE || projectile.getId() == DAWNBRINGER_SPEC_PROJECTILE)
                            {
                                int projectileHitTick = projectile.getRemainingCycles();
                                projectileHitTick = (projectileHitTick / 30);
                                clog.addLine(DAWN_SPEC, playerAttackQueuedItem.player.getName(), String.valueOf(client.getTickCount() - currentRoom.roomStartTick + projectileHitTick + 1));

                            }
                            if (projectile.getId() == DAWNBRINGER_AUTO_PROJECTILE || projectile.getId() == ZCB_PROJECTILE || projectile.getId() == ZCB_SPEC_PROJECTILE || projectile.getId() == DAWNBRINGER_SPEC_PROJECTILE)
                            {
                                int interactedIndex = -1;
                                int interactedID = -1;
                                Actor interacted = playerAttackQueuedItem.player.getInteracting();
                                String targetName = "";
                                if (interacted instanceof NPC)
                                {
                                    NPC npc = (NPC) interacted;
                                    interactedID = npc.getId();
                                    interactedIndex = npc.getIndex();
                                    targetName = npc.getName();
                                }
                                if (interacted instanceof Player)
                                {
                                    Player player = (Player) interacted;
                                    targetName = player.getName();
                                }
                                clog.addLine(PLAYER_ATTACK,
                                        playerAttackQueuedItem.player.getName() + ":" + (client.getTickCount() - currentRoom.roomStartTick),
                                        playerAttackQueuedItem.animation + ":" + PlayerWornItems.getStringFromComposition(playerAttackQueuedItem.player.getPlayerComposition()),
                                        playerAttackQueuedItem.spotAnims,
                                        playerAttackQueuedItem.weapon + ":" + interactedIndex + ":" + interactedID,
                                        projectile.getId() + ":" + targetName, currentRoom.getName());
                                liveFrame.addAttack(new PlayerDidAttack(itemManager,
                                                playerAttackQueuedItem.player.getName(),
                                                playerAttackQueuedItem.animation,
                                                0,
                                                playerAttackQueuedItem.weapon,
                                                String.valueOf(projectile.getId()),
                                                playerAttackQueuedItem.spotAnims,
                                                interactedIndex,
                                                interactedID,
                                                targetName,
                                                PlayerWornItems.getStringFromComposition(playerAttackQueuedItem.player.getPlayerComposition()))
                                        , currentRoom.getName());
                            }
                        }
                    }
                }
            }
        }
        playersAttacked.removeIf(p -> p.tick == 0);
    }

    private void checkActivelyPiping()
    {
        for (Player p : activelyPiping.keySet())
        {
            if ((client.getTickCount() > (activelyPiping.get(p) + 1)) && ((client.getTickCount() - activelyPiping.get(p) - 1) % 2 == 0))
            {
                if (p.getAnimation() == BLOWPIPE_ANIMATION || p.getAnimation() == BLOWPIPE_ANIMATION_OR)
                {
                    PlayerCopy previous = lastTickPlayer.get(p.getName());
                    if (previous != null)
                    {
                        clog.addLine(PLAYER_ATTACK,
                                previous.name + ":" + (client.getTickCount() - currentRoom.roomStartTick - 1),
                                previous.animation + ":" + previous.wornItems,
                                "",
                                previous.weapon + ":" + previous.interactingIndex + ":" + previous.interactingID,
                                "-1:" + previous.interactingName, currentRoom.getName());
                        liveFrame.addAttack(new PlayerDidAttack(itemManager,
                                previous.name,
                                String.valueOf(previous.animation),
                                -1,
                                previous.weapon,
                                "-1",
                                "",
                                previous.interactingIndex,
                                previous.interactingID,
                                previous.interactingName,
                                previous.wornItems
                        ), currentRoom.getName());
                    }
                }
            }
            int interactedIndex = -1;
            int interactedID = -1;
            String targetName = "";
            Actor interacted = p.getInteracting();
            if (interacted instanceof NPC)
            {
                NPC npc = (NPC) interacted;
                interactedID = npc.getId();
                interactedIndex = npc.getIndex();
                targetName = npc.getName();
            }
            if (interacted instanceof Player)
            {
                Player player = (Player) interacted;
                targetName = player.getName();
            }
            lastTickPlayer.put(p.getName(), new PlayerCopy(
                    p.getName(), interactedIndex, interactedID, targetName, p.getAnimation(), PlayerWornItems.getStringFromComposition(p.getPlayerComposition()
            ), p.getPlayerComposition().getEquipmentId(KitType.WEAPON), p.getWorldLocation()));
        }
    }

    private void checkOverheadTextsThatChanged()
    {
        for (String player : playersWhoHaveOverheadText)
        {
            for (VengPair vp : playersTextChanged)
            {
                if (vp.player.equals(player))
                {
                    activeVenges.add(new VengDamageQueue(vp.player, vp.hitsplat, client.getTickCount() + 1));
                }
            }
        }
        playersWhoHaveOverheadText.clear();
    }

    List<Player> deferredGraphics = new ArrayList<>();

    private void checkAnimationsThatChanged()
    {
        for (Player p : deferredAnimations)
        {
            checkAnimation(p);
        }
        deferredAnimations.clear();
    }

	List<String> scythedPreviousTick = new ArrayList<>();
	List<String> scythedThisTick = new ArrayList<>();
    private void checkAnimation(Player p)
    {
        if (inTheatre)
        {
            if (p.getPlayerComposition() != null)
            {
                int id = p.getPlayerComposition().getEquipmentId(KitType.WEAPON);
                if (p.getAnimation() == SCYTHE_ANIMATION)
                {
					scythedThisTick.add(p.getName());
                    if (id == UNCHARGED_SCYTHE || id == UNCHARGED_BLOOD_SCYTHE || id == UNCHARGED_HOLY_SCYTHE)
                    {
                        if (config.showMistakesInChat())
                        {
                            sendChatMessage(p.getName() + " is using an uncharged scythe");
                        }
                    }
                } else if (p.getAnimation() == BOP_ANIMATION)
                {
                    if (id == DRAGON_WARHAMMER || id == DRAGON_WARHAMMER_ALTERNATE)
                    {
                        if (config.showMistakesInChat())
                        {
                            sendChatMessage(p.getName() + " hammer bopped (bad rng)");
                        }
                        clog.addLine(DWH_BOP, p.getName());
                    }
                } else if (p.getAnimation() == WHACK_ANIMATION)
                {
                    if (id == KODAI_WAND || id == KODAI_WAND_ALTERNATE)
                    {
                        if (config.showMistakesInChat())
                        {
                            sendChatMessage(p.getName() + " kodai bopped (nothing they could've done to prevent it)");
                        }
                        clog.addLine(KODAI_BOP, p.getName());
                    }
                } else if (p.getAnimation() == STAB_ANIMATION)
                {
                    if (id == CHALLY)
                    {
                        if (config.showMistakesInChat())
                        {
                            sendChatMessage(p.getName() + " chally poked");
                        }
                        clog.addLine(CHALLY_POKE, p.getName());
                    }
                } else if (p.getAnimation() == TWO_HAND_SWORD_SWING)
                {
                    if ((id == BANDOS_GODSWORD || id == BANDOS_GODSWORD_OR) && !(currentRoom instanceof VerzikHandler && p.getInteracting().getName() != null && p.getInteracting().getName().contains("Matamenos")))
                    {
                        if (config.showMistakesInChat())
                        {
                            sendChatMessage(p.getName() + " swung BGS without speccing");
                        }
                        clog.addLine(BGS_WHACK, p.getName());
                    }
                }
                StringBuilder animations = new StringBuilder();
                for (ActorSpotAnim anim : p.getSpotAnims())
                {
                    animations.append(anim.getId());
                    animations.append(":");
                }
                if (p.getAnimation() == POWERED_STAFF_ANIMATION || p.getAnimation() == CROSSBOW_ANIMATION)
                {
                    if (p.getAnimation() != POWERED_STAFF_ANIMATION || p.getPlayerComposition().getEquipmentId(KitType.WEAPON) == DAWNBRINGER_ITEM)
                    { //Can be ZCB, Sang, or Dawnbringer. We only care about projectile for dawnbringer or ZCB. Sang & dawnbringer share animation
                        //so this filters powered staves unless it's dawnbringer
                        WorldPoint worldPoint = p.getWorldLocation();
                        playersAttacked.add(new QueuedPlayerAttackLessProjectiles(p, worldPoint, 1, animations.toString(), p.getPlayerComposition().getEquipmentId(KitType.WEAPON), String.valueOf(p.getAnimation())));
                    } else
                    {
                        int interactedIndex = -1;
                        int interactedID = -1;
                        Actor interacted = p.getInteracting();
                        generatePlayerAttackInfo(p, animations.toString(), interacted, -1);
                    }
                } else if (p.getAnimation() != -1)
                {
                    int interactedIndex = -1;
                    int interactedID = -1;
                    Actor interacted = p.getInteracting();
                    generatePlayerAttackInfo(p, animations.toString(), interacted, -1);
                    if (p.getAnimation() == BLOWPIPE_ANIMATION || p.getAnimation() == BLOWPIPE_ANIMATION_OR)
                    {
                        activelyPiping.put(p, client.getTickCount());
                        String targetName = interacted.getName();
                        if (interacted instanceof NPC)
                        {
                            NPC npc = (NPC) interacted;
                            interactedID = npc.getId();
                            interactedIndex = npc.getIndex();
                        }
                        lastTickPlayer.put(p.getName(), new PlayerCopy(
                                p.getName(), interactedIndex, interactedID, targetName,
                                p.getAnimation(), PlayerWornItems.getStringFromComposition(p.getPlayerComposition()
                        ), p.getPlayerComposition().getEquipmentId(KitType.WEAPON), p.getWorldLocation()));
                    } else
                    {
                        activelyPiping.remove(p);
                        lastTickPlayer.remove(p.getName());
                    }
                } else
                {
                    activelyPiping.remove(p);
                }

            }
        }
    }

    public void leftRaid()
    {
        lastTickPlayer.clear();
        partyIntact = false;
        currentPlayers.clear();
        clog.addLine(LEFT_TOB, String.valueOf(client.getTickCount() - currentRoom.roomStartTick), currentRoom.getName());
        clog.writeFile();
        clog.migrateToNewRaid();
        currentRoom = null;
        activelyPiping.clear();
        deferredAnimations.clear();
    }

    @Subscribe
    public void onActorDeath(ActorDeath event)
    {
        if (inTheatre)
        {
            Actor a = event.getActor();
            if (a instanceof Player)
            {
                clog.addLine(PLAYER_DIED, event.getActor().getName(), String.valueOf(client.getTickCount() - currentRoom.roomStartTick));
            }
        }
    }

    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned event)
    {
        if (inTheatre)
        {
            currentRoom.updateGroundObjectSpawned(event);
        }
    }

    @Subscribe
    public void onGroundObjectDespawned(GroundObjectDespawned event)
    {
        if (inTheatre)
        {
            currentRoom.updateGroundObjectDespawned(event);
        }
    }

    @Subscribe
    public void onGraphicChanged(GraphicChanged event)
    {
        if (event.getActor() instanceof Player)
        {
            int id = -1;
            if (event.getActor().hasSpotAnim(THRALL_CAST_GRAPHIC_MAGE))
            {
                id = THRALL_CAST_GRAPHIC_MAGE;
            } else if (event.getActor().hasSpotAnim(THRALL_CAST_GRAPHIC_MELEE))
            {
                id = THRALL_CAST_GRAPHIC_MELEE;
            } else if (event.getActor().hasSpotAnim(THRALL_CAST_GRAPHIC_RANGE))
            {
                id = THRALL_CAST_GRAPHIC_RANGE;
            } else if (event.getActor().hasSpotAnim(VENG_GRAPHIC))
            {
                vengTracker.vengSelfGraphicApplied((Player) event.getActor());
            } else if (event.getActor().hasSpotAnim(VENG_OTHER_GRAPHIC))
            {
                vengTracker.vengOtherGraphicApplied((Player) event.getActor());
            }
            if (id != -1)
            {
                thrallTracker.playerHasThrallCastSpotAnim((Player) event.getActor(), id);
            }

        } else if (event.getActor() instanceof NPC)
        {
            if (event.getActor().hasSpotAnim(369) || event.getActor().hasSpotAnim(377) || event.getActor().hasSpotAnim(85) || event.getActor().hasSpotAnim(367) || event.getActor().hasSpotAnim(375))
            {
                wasBarraged.add((NPC) event.getActor());
            }
        }
        if (inTheatre)
        {
            if(event.getActor() instanceof Player)
            {
                deferredGraphics.add((Player) event.getActor());
            }
            currentRoom.updateGraphicChanged(event);
        }
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event)
    {
        if (inTheatre)
        {
            currentRoom.updateGraphicsObjectCreated(event);
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        if (inTheatre)
        {
            currentRoom.updateGameObjectSpawned(event);
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (inTheatre)
        {
            currentRoom.updateStatChanged(event);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        if (inTheatre)
        {
            currentRoom.updateGameObjectDespawned(event);
        }
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned event)
    {
        if (inTheatre)
        {
            currentRoom.updateItemSpawned(event);
        }
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event)
    {
        if (event.getProjectile().getId() == 1272)
        {
            if (event.getProjectile().getStartCycle() == client.getGameCycle())
            {
                chinSpawned.add(WorldPoint.fromLocal(client, new LocalPoint(event.getProjectile().getX1(), event.getProjectile().getY1())));
            }
        }
        if (inTheatre)
        {
            int id = event.getProjectile().getId();
            if (id == THRALL_PROJECTILE_RANGE || id == THRALL_PROJECTILE_MAGE)
            {
                if (event.getProjectile().getStartCycle() == client.getGameCycle())
                {
                    thrallTracker.projectileCreated(event.getProjectile(), WorldPoint.fromLocal(client, new LocalPoint(event.getProjectile().getX1(), event.getProjectile().getY1())));
                }
            }
            //Thrall hitsplats come before damage hitsplits unless the source is a projectile that was spawned on a tick before the thrall projectile spawned
            else if (event.getProjectile().getStartCycle() == client.getGameCycle())
            { //Thrall projectiles move slower and the only time this situation occurs in TOB is max distance TBOW/ZCB during maiden
                if (id == TBOW_PROJECTILE || id == ZCB_PROJECTILE || id == ZCB_SPEC_PROJECTILE)
                { //Not sure why 10 is correct instead of 19 (60 - 41 tick delay) but extensive trial and error shows this to be accurate
                    int projectileHitTick = 10 + event.getProjectile().getRemainingCycles();
                    projectileHitTick = (projectileHitTick / 30);
                    if (event.getProjectile().getInteracting() instanceof NPC)
                    {
                        int index = ((NPC) event.getProjectile().getInteracting()).getIndex();
                        activeProjectiles.add(new ProjectileQueue(client.getTickCount(), projectileHitTick + client.getTickCount(), index));
                    }
                }
            }
            if (inTheatre)
            {
                currentRoom.updateProjectileMoved(event);
            }
        }
    }

    public void sendChatMessage(String msg)
    {
        clientThread.invoke(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null, false));
    }

    private final ArrayList<Player> deferredAnimations = new ArrayList<>();

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if(event.getGroup().equals("Advanced Raid Tracker") && event.getKey().contains("unkitted"))
        {
            OutlineBox.useUnkitted = config.useUnkitted();
        }
        if (event.getGroup().equals("Advanced Raid Tracker") && event.getKey().contains("primary"))
        {
            liveFrame.redrawAll();
        } else if (event.getGroup().equals("Advanced Raid Tracker") && event.getKey().contains("theme"))
        {
            ChartTheme theme = ChartTheme.valueOf(event.getNewValue());

            configManager.setConfiguration("Advanced Raid Tracker", "primaryDark", Color.decode(theme.getPrimaryDark()));
            configManager.setConfiguration("Advanced Raid Tracker", "primaryMiddle", Color.decode(theme.getPrimaryMiddle()));
            configManager.setConfiguration("Advanced Raid Tracker", "primaryLight", Color.decode(theme.getPrimaryLight()));
            configManager.setConfiguration("Advanced Raid Tracker", "idleColor", Color.decode(theme.getIdleTick()));
            configManager.setConfiguration("Advanced Raid Tracker", "fontColor", Color.decode(theme.getFontColor()));
            configManager.setConfiguration("Advanced Raid Tracker", "markerColor", Color.decode(theme.getMarkerColor()));
            configManager.setConfiguration("Advanced Raid Tracker", "boxColor", Color.decode(theme.getBoxColor()));
            configManager.setConfiguration("Advanced Raid Tracker", "attackColor", Color.decode(theme.getAttackBoxColor()));
            configManager.setConfiguration("Advanced Raid Tracker", "useRounded", theme.isUseRounded());
            configManager.setConfiguration("Advanced Raid Tracker", "wrapAllBoxes", theme.isWrapAllBoxes());
            configManager.setConfiguration("Advanced Raid Tracker", "showBoldTick", theme.isShowBoldTick());
            configManager.setConfiguration("Advanced Raid Tracker", "rightAlignTicks", theme.isRightAlignTicks());
            configManager.setConfiguration("Advanced Raid Tracker", "useAlternateFont", theme.isUseAlternateFont());
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if (event.getActor() instanceof NPC)
        {
            if (currentRoom != null)
            {
                liveFrame.addAttack(new PlayerDidAttack(itemManager,
                        String.valueOf(((NPC) event.getActor()).getIndex()),
                        String.valueOf(event.getActor().getAnimation()),
                        0,
                        100, //todo why is this 100?
                        "-1",
                        "",
                        0,
                        0,
                        client.getLocalPlayer().getName(),
                        ""), currentRoom.getName());
            }

        }
        if (event.getActor() instanceof Player)
        {
            Player p = (Player) event.getActor();
            if (event.getActor().getAnimation() == 6294 || event.getActor().getAnimation() == 722 || event.getActor().getAnimation() == 6299 || event.getActor().getAnimation() == -1)
            {
                if (activelyPiping.containsKey(p))
                {
                    wasPiping.add(p);
                }
                checkAnimation(p);
            } else
            {
                deferredAnimations.add(p);
            }
        }
        if (inTheatre)
        {
            int id = event.getActor().getAnimation();
            if (event.getActor().getAnimation() == THRALL_CAST_ANIMATION)
            {
                thrallTracker.castThrallAnimation((Player) event.getActor());
            } else if (event.getActor().getAnimation() == MELEE_THRALL_ATTACK_ANIMATION && event.getActor() instanceof NPC)
            {
                thrallTracker.meleeThrallAttacked((NPC) event.getActor());
            } else if (event.getActor().getAnimation() == VENG_CAST)
            {
                vengTracker.vengSelfCast((Player) event.getActor());
            } else if (event.getActor().getAnimation() == VENG_OTHER_CAST)
            {
                vengTracker.vengOtherCast((Player) event.getActor());
            } else if (id == DWH_SPEC)
            {
                clog.addLine(HAMMER_ATTEMPTED, event.getActor().getName());
            } else if (event.getActor().getName() != null && event.getActor().getName().contains("Maiden") && id == MAIDEN_BLOOD_THROW_ANIM)
            {
                clog.addLine(BLOOD_THROWN);
            }
            if (inTheatre)
            {
                currentRoom.updateAnimationChanged(event);
            }
        }
    }

    private void generatePlayerAttackInfo(Player p, int overriddenAnimation)
    {
        StringBuilder animations = new StringBuilder();
        for (ActorSpotAnim anim : p.getSpotAnims())
        {
            animations.append(anim.getId());
            animations.append(":");
        }
        generatePlayerAttackInfo(p, animations.toString(), p.getInteracting(), overriddenAnimation);
    }

    /**
     * Generates a PlayerDidAttack entry into the log.
     *
     * @param p          the current player
     * @param animations animations happening
     * @param interacted Actor (Player or NPC) that was interacted with.
     */
    private void generatePlayerAttackInfo(Player p, String animations, Actor interacted, int overriddenAnimation)
    {
        int interactedIndex = -1;
        int interactedID = -1;
        String targetName = "";
        if (interacted != null && interacted.getName() != null)
        {
            targetName = interacted.getName();
        }
        if (interacted instanceof NPC)
        {
            NPC npc = (NPC) interacted;
            interactedID = npc.getId();
            interactedIndex = npc.getIndex();
        }

        int animationToUse = (overriddenAnimation == -1) ? p.getAnimation() : overriddenAnimation;
        clog.addLine(PLAYER_ATTACK,
                p.getName() + ":" + (client.getTickCount() - currentRoom.roomStartTick),
                animationToUse + ":" + PlayerWornItems.getStringFromComposition(p.getPlayerComposition()),
                animations,
                p.getPlayerComposition().getEquipmentId(KitType.WEAPON) + ":" + interactedIndex + ":" + interactedID,
                "-1:" + targetName, currentRoom.getName());
        liveFrame.addAttack(new PlayerDidAttack(itemManager,
                String.valueOf(p.getName()),
                String.valueOf(animationToUse),
                0,
                p.getPlayerComposition().getEquipmentId(KitType.WEAPON),
                "-1",
                animations,
                interactedIndex,
                interactedID,
                targetName,
                PlayerWornItems.getStringFromComposition(p.getPlayerComposition())
        ), currentRoom.getName());
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        if (inTheatre)
        {
            currentRoom.updateInteractingChanged(event);
        }
    }

    @Subscribe
    public void onNpcChanged(NpcChanged event)
    {
        if (inTheatre)
        {
            currentRoom.handleNPCChanged(event.getNpc().getId());
        }
    }

    private void handleThrallSpawn(NPC npc)
    {
        ArrayList<PlayerShell> potentialPlayers = new ArrayList<>();
        for (PlayerShell p : localPlayers)
        {
            if (p.worldLocation.distanceTo(npc.getWorldLocation()) == 1)
            {
                potentialPlayers.add(p);
            }
        }
        thrallTracker.thrallSpawned(npc, potentialPlayers);
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        int id = event.getNpc().getId();
        if (id == MELEE_THRALL || id == RANGE_THRALL || id == MAGE_THRALL)
        {
            handleThrallSpawn(event.getNpc());
        }
        switch (event.getNpc().getId())
        {
            case TobIDs.MAIDEN_P0:
            case TobIDs.MAIDEN_P1:
            case TobIDs.MAIDEN_P2:
            case TobIDs.MAIDEN_P3:
            case TobIDs.MAIDEN_PRE_DEAD:
            case TobIDs.MAIDEN_DEAD:
            case TobIDs.MAIDEN_MATOMENOS:
            case TobIDs.MAIDEN_P0_HM:
            case TobIDs.MAIDEN_P1_HM:
            case TobIDs.MAIDEN_P2_HM:
            case TobIDs.MAIDEN_P3_HM:
            case TobIDs.MAIDEN_PRE_DEAD_HM:
            case TobIDs.MAIDEN_DEAD_HM:
            case TobIDs.MAIDEN_MATOMENOS_HM:
            case TobIDs.MAIDEN_P0_SM:
            case TobIDs.MAIDEN_P1_SM:
            case TobIDs.MAIDEN_P2_SM:
            case TobIDs.MAIDEN_P3_SM:
            case TobIDs.MAIDEN_PRE_DEAD_SM:
            case TobIDs.MAIDEN_DEAD_SM:
            case TobIDs.MAIDEN_MATOMENOS_SM:
            case TobIDs.MAIDEN_BLOOD:
            case TobIDs.MAIDEN_BLOOD_HM:
            case TobIDs.MAIDEN_BLOOD_SM:
            {
                maiden.updateNpcSpawned(event);
            }
            break;
            case TobIDs.BLOAT:
            case TobIDs.BLOAT_HM:
            case TobIDs.BLOAT_SM:
                bloat.updateNpcSpawned(event);
                break;
            case TobIDs.NYLO_MELEE_SMALL:
            case TobIDs.NYLO_MELEE_SMALL_AGRO:
            case TobIDs.NYLO_RANGE_SMALL:
            case TobIDs.NYLO_RANGE_SMALL_AGRO:
            case TobIDs.NYLO_MAGE_SMALL:
            case TobIDs.NYLO_MAGE_SMALL_AGRO:
            case TobIDs.NYLO_MELEE_BIG:
            case TobIDs.NYLO_MELEE_BIG_AGRO:
            case TobIDs.NYLO_RANGE_BIG:
            case TobIDs.NYLO_RANGE_BIG_AGRO:
            case TobIDs.NYLO_MAGE_BIG:
            case TobIDs.NYLO_MAGE_BIG_AGRO:
            case TobIDs.NYLO_MELEE_SMALL_HM:
            case TobIDs.NYLO_MELEE_SMALL_AGRO_HM:
            case TobIDs.NYLO_RANGE_SMALL_HM:
            case TobIDs.NYLO_RANGE_SMALL_AGRO_HM:
            case TobIDs.NYLO_MAGE_SMALL_HM:
            case TobIDs.NYLO_MAGE_SMALL_AGRO_HM:
            case TobIDs.NYLO_MELEE_BIG_HM:
            case TobIDs.NYLO_MELEE_BIG_AGRO_HM:
            case TobIDs.NYLO_RANGE_BIG_HM:
            case TobIDs.NYLO_RANGE_BIG_AGRO_HM:
            case TobIDs.NYLO_MAGE_BIG_HM:
            case TobIDs.NYLO_MAGE_BIG_AGRO_HM:
            case TobIDs.NYLO_MELEE_SMALL_SM:
            case TobIDs.NYLO_MELEE_SMALL_AGRO_SM:
            case TobIDs.NYLO_RANGE_SMALL_SM:
            case TobIDs.NYLO_RANGE_SMALL_AGRO_SM:
            case TobIDs.NYLO_MAGE_SMALL_SM:
            case TobIDs.NYLO_MAGE_SMALL_AGRO_SM:
            case TobIDs.NYLO_MELEE_BIG_SM:
            case TobIDs.NYLO_MELEE_BIG_AGRO_SM:
            case TobIDs.NYLO_RANGE_BIG_SM:
            case TobIDs.NYLO_RANGE_BIG_AGRO_SM:
            case TobIDs.NYLO_MAGE_BIG_SM:
            case TobIDs.NYLO_MAGE_BIG_AGRO_SM:
            case TobIDs.NYLO_BOSS_DROPPING:
            case TobIDs.NYLO_BOSS_DROPPING_HM:
            case TobIDs.NYLO_BOSS_DROPING_SM:
            case TobIDs.NYLO_BOSS_MELEE:
            case TobIDs.NYLO_BOSS_MELEE_HM:
            case TobIDs.NYLO_BOSS_MELEE_SM:
            case TobIDs.NYLO_BOSS_MAGE:
            case TobIDs.NYLO_BOSS_MAGE_HM:
            case TobIDs.NYLO_BOSS_MAGE_SM:
            case TobIDs.NYLO_BOSS_RANGE:
            case TobIDs.NYLO_BOSS_RANGE_HM:
            case TobIDs.NYLO_BOSS_RANGE_SM:
            case TobIDs.NYLO_PRINKIPAS_DROPPING:
            case TobIDs.NYLO_PRINKIPAS_MELEE:
            case TobIDs.NYLO_PRINKIPAS_MAGIC:
            case TobIDs.NYLO_PRINKIPAS_RANGE:
                nylo.updateNpcSpawned(event);
                break;
            case TobIDs.SOTETSEG_ACTIVE:
            case TobIDs.SOTETSEG_ACTIVE_HM:
            case TobIDs.SOTETSEG_ACTIVE_SM:
            case TobIDs.SOTETSEG_INACTIVE:
            case TobIDs.SOTETSEG_INACTIVE_HM:
            case TobIDs.SOTETSEG_INACTIVE_SM:
                sote.updateNpcSpawned(event);
                break;
            case TobIDs.XARPUS_INACTIVE:
            case TobIDs.XARPUS_P1:
            case TobIDs.XARPUS_P23:
            case TobIDs.XARPUS_DEAD:
            case TobIDs.XARPUS_INACTIVE_HM:
            case TobIDs.XARPUS_P1_HM:
            case TobIDs.XARPUS_P23_HM:
            case TobIDs.XARPUS_DEAD_HM:
            case TobIDs.XARPUS_INACTIVE_SM:
            case TobIDs.XARPUS_P1_SM:
            case TobIDs.XARPUS_P23_SM:
            case TobIDs.XARPUS_DEAD_SM:
                xarpus.updateNpcSpawned(event);
                break;
            case TobIDs.VERZIK_P1_INACTIVE:
            case TobIDs.VERZIK_P1:
            case TobIDs.VERZIK_P2_INACTIVE:
            case TobIDs.VERZIK_P2:
            case TobIDs.VERZIK_P3_INACTIVE:
            case TobIDs.VERZIK_P3:
            case TobIDs.VERZIK_DEAD:
            case TobIDs.VERZIK_P1_INACTIVE_HM:
            case TobIDs.VERZIK_P1_HM:
            case TobIDs.VERZIK_P2_INACTIVE_HM:
            case TobIDs.VERZIK_P2_HM:
            case TobIDs.VERZIK_P3_INACTIVE_HM:
            case TobIDs.VERZIK_P3_HM:
            case TobIDs.VERZIK_DEAD_HM:
            case TobIDs.VERZIK_P1_INACTIVE_SM:
            case TobIDs.VERZIK_P1_SM:
            case TobIDs.VERZIK_P2_INACTIVE_SM:
            case TobIDs.VERZIK_P2_SM:
            case TobIDs.VERZIK_P3_INACTIVE_SM:
            case TobIDs.VERZIK_P3_SM:
            case TobIDs.VERZIK_DEAD_SM:
                verzik.updateNpcSpawned(event);
                break;
            default:
                if (currentRoom != null)
                {
                    currentRoom.updateNpcSpawned(event);
                    liveFrame.getPanel(currentRoom.getName()).addNPCMapping(event.getNpc().getIndex(), event.getNpc().getName());
                }
                break;
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        int id = event.getNpc().getId();
        if (id == MELEE_THRALL || id == RANGE_THRALL || id == MAGE_THRALL)
        {
            thrallTracker.removeThrall(event.getNpc());
        }
        if (inTheatre)
        {
            currentRoom.updateNpcDespawned(event);
        }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        if (inTheatre)
        {
            if (event.getActor() instanceof Player && inTheatre)
            {
                playersTextChanged.add(new VengPair(event.getActor().getName(), event.getHitsplat().getAmount()));
            }
            queuedThrallDamage.sort(Comparator.comparing(DamageQueueShell::getSourceIndex));
            int index = -1;
            if (event.getActor() instanceof NPC && event.getHitsplat().getHitsplatType() != HitsplatID.HEAL)
            {
                for (int i = 0; i < queuedThrallDamage.size(); i++)
                {
                    int altIndex = 0;
                    int matchedIndex = -1;
                    boolean postponeThrallHit = false;
                    for (ProjectileQueue projectile : activeProjectiles)
                    {
                        if (projectile.targetIndex == ((NPC) event.getActor()).getIndex())
                        {
                            if (client.getTickCount() == projectile.finalTick)
                            {
                                if (projectile.originTick < queuedThrallDamage.get(i).originTick)
                                {
                                    postponeThrallHit = true;
                                    matchedIndex = altIndex;
                                }
                            }
                        }
                        altIndex++;
                    }
                    if (queuedThrallDamage.get(i).offset == 0 && queuedThrallDamage.get(i).targetIndex == ((NPC) event.getActor()).getIndex())
                    {
                        if (postponeThrallHit)
                        {
                            activeProjectiles.remove(matchedIndex);
                        } else
                        {
                            if (event.getHitsplat().getAmount() <= 3)
                            {
                                index = i;
                                clog.addLine(THRALL_DAMAGED, queuedThrallDamage.get(i).source, String.valueOf(event.getHitsplat().getAmount()));

                            }
                        }
                        if (index != -1)
                        {
                            queuedThrallDamage.remove(index);
                        }
                        if (inTheatre)
                        {
                            currentRoom.updateHitsplatApplied(event);
                        }
                        return;
                    }
                }
                for (VengDamageQueue veng : activeVenges)
                {
                    int expectedDamage = (int) (0.75 * veng.damage);
                    if (event.getHitsplat().getAmount() == expectedDamage)
                    {
                        //todo can be wrong if splat would overkill
                        clog.addLine(VENG_WAS_PROCCED, veng.target, String.valueOf(expectedDamage), String.valueOf(client.getTickCount()-currentRoom.roomStartTick));
                        if (inTheatre)
                        {
                            currentRoom.updateHitsplatApplied(event);
                        }
                        return;
                    }
                }
            }

            if (inTheatre)
            {
                currentRoom.updateHitsplatApplied(event);
            }
        }
    }


    private ArrayList<VengPair> playersTextChanged;

    private final ArrayList<String> playersWhoHaveOverheadText = new ArrayList<>();

    @Subscribe
    public void onOverheadTextChanged(OverheadTextChanged event)
    {
        if (inTheatre)
        {
            if (event.getOverheadText().equals("Taste vengeance!"))
            {
                playersWhoHaveOverheadText.add(event.getActor().getName());
            }
            if (currentRoom instanceof XarpusHandler)
            {
                xarpus.updateOverheadText(event);
            }
        }
    }

    @Getter
    public String lastSplits = "";

	public int currentDurationSum = 0;

}

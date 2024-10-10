package com.advancedraidtracker.ui.charts;

import com.advancedraidtracker.*;
import com.advancedraidtracker.constants.RaidRoom;
import com.advancedraidtracker.ui.BaseFrame;
import com.advancedraidtracker.ui.Raids;
import static com.advancedraidtracker.ui.charts.ChartPanel.isCtrlPressed;
import com.advancedraidtracker.utility.datautility.ChartData;
import com.advancedraidtracker.utility.datautility.DataPoint;
import com.advancedraidtracker.utility.datautility.DataReader;
import com.advancedraidtracker.utility.datautility.datapoints.col.Colo;
import com.advancedraidtracker.utility.datautility.datapoints.Raid;
import com.advancedraidtracker.utility.datautility.datapoints.inf.Inf;
import com.advancedraidtracker.utility.datautility.datapoints.toa.Toa;
import com.advancedraidtracker.utility.weapons.PlayerAnimation;
import com.advancedraidtracker.utility.wrappers.PlayerDidAttack;
import java.awt.event.ComponentListener;
import static java.awt.event.KeyEvent.KEY_RELEASED;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

import static com.advancedraidtracker.utility.UISwingUtility.*;

@Slf4j
public class ChartFrame extends BaseFrame
{
	private int frameX = this.getWidth();
	private int frameY = this.getHeight();
	private Set<String> TOBRooms = new LinkedHashSet<>(Arrays.asList("Maiden", "Bloat", "Nylocas", "Sotetseg", "Xarpus", "Verzik P1", "Verzik P2", "Verzik P3"));
	private Set<String> TOARooms = new LinkedHashSet<>(Arrays.asList("Apmeken", "Baba", "Scabaras", "Kephri", "Het", "Akkha", "Crondis", "Zebak", "Wardens P1", "Wardens P2", "Wardens P3"));
	private Set<String> COLRooms = new LinkedHashSet<>(Arrays.asList("Col Wave 1", "Col Wave 2", "Col Wave 3", "Col Wave 4", "Col Wave 5", "Col Wave 6", "Col Wave 7", "Col Wave 8", "Col Wave 9", "Col Wave 10", "Col Wave 11", "Col Wave 12"));
	private Set<String> InfRooms = new LinkedHashSet<>(Arrays.asList("Inf Wave 1", "Inf Wave 2", "Inf Wave 3", "Inf Wave 4", "Inf Wave 5", "Inf Wave 6", "Inf Wave 7", "Inf Wave 8", "Inf Wave 9", "Inf Wave 10", "Inf Wave 11", "Inf Wave 12"));
	AdvancedRaidTrackerConfig config;
	List<ChartPanel> panels = new ArrayList<>();
	ItemManager itemManager;
	ClientThread clientThread;
	ConfigManager configManager;
	SpriteManager spriteManager;
	Raids raids;
	int currentIndex = 0;

	public ChartFrame(AdvancedRaidTrackerConfig config, ItemManager itemManager, ClientThread clientThread, ConfigManager configManager, SpriteManager spriteManager, Raids raids)
	{
		this.config = config;
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.spriteManager = spriteManager;
		this.raids = raids;
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				releasePanels();
			}
		});

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e ->
		{
			synchronized (ChartFrame.class)
			{
				if (isCtrlPressed())
				{
					if (e.getID() == KEY_RELEASED)
					{
						if (e.getKeyCode() == VK_LEFT)
						{
							switchToPrevious();
						}
						else if (e.getKeyCode() == VK_RIGHT)
						{
							switchToNext();
						}
					}
				}
			}
			return false;
		});
	}

	private void releasePanels()
	{
		for (ChartPanel panel : panels)
		{
			panel.release();
		}
		panels.clear();
	}

	public void switchToNext()
	{
		Raid raid = raids.getNextRaid(currentIndex);
		if (raid != null)
		{
			switchTo(raid);
		}
	}

	public void switchToPrevious()
	{
		Raid raid = raids.getPreviousRaid(currentIndex);
		if (raid != null)
		{
			switchTo(raid);
		}
	}

	JTabbedPane basepane;

	public void switchTo(Raid roomData)
	{
		currentIndex = roomData.getIndex();
		getContentPane().removeAll();
		releasePanels();
		ChartData chartData = DataReader.getChartData(roomData.getFilepath(), itemManager);
		if (basepane != null)
		{
			basepane.removeAll();
		}
		basepane = getThemedTabbedPane();

		Set<String> activeSet;

		if (roomData instanceof Toa)
		{
			activeSet = TOARooms;
		}
		else if (roomData instanceof Colo)
		{
			activeSet = COLRooms;
		}
		else if (roomData instanceof Inf)
		{
			activeSet = InfRooms;
		}
		else
		{
			activeSet = TOBRooms;
		}

		for (ComponentListener cl : getComponentListeners())
		{
			removeComponentListener(cl);
		}

		for (String bossName : activeSet)
		{
			RaidRoom room = RaidRoom.getRoom(bossName);
			JPanel tab = getThemedPanel();
			tab.setLayout(new GridLayout(1, 2));
			JPanel chart = getThemedPanel();
			chart.setLayout(new BoxLayout(chart, BoxLayout.Y_AXIS));
			ChartPanel chartPanel = new ChartPanel(bossName, false, config, clientThread, configManager, itemManager, spriteManager);
			chartPanel.setNPCMappings(chartData.getNPCMapping(room));
			chartPanel.addAttacks(chartData.getAttacks(room));
			chartPanel.setRoomHP(chartData.getHPMapping(room));
			chartPanel.setAttackers(new ArrayList<>(roomData.getPlayers()));
			chartPanel.enableWrap();
			if (bossName.contains("Wave"))
			{
				int starttick = 1;
				if (roomData instanceof Inf)
				{
					java.util.List<Integer> waveDurations = roomData.getList(DataPoint.INFERNO_WAVE_DURATIONS);
					java.util.List<Integer> waveStarts = roomData.getList(DataPoint.INFERNO_WAVE_STARTS);
					int waveTime = 0;
					int waveStart = 0;
					int wave = Integer.parseInt(bossName.split(" ")[2]);
					if (waveDurations.size() > wave - 1)
					{
						waveTime = waveDurations.get(wave - 1);
					}
					waveStart = ((Inf) roomData).waveStarts.getOrDefault(wave, 1);
					chartPanel.setStartTick(waveStart);
					chartPanel.setEndTick(waveStart + waveTime);
				}
				else
				{
					chartPanel.setStartTick(starttick);
					chartPanel.setEndTick(starttick + roomData.get(bossName + " Duration"));
				}
			}
			else
			{
				chartPanel.setStartTick((bossName.contains("Verzik") || bossName.contains("Wardens")) ? //Just trust
					(bossName.contains("P1") ? 1 : (bossName.contains("P2") ? roomData.get(bossName.replace('2', '1') + " Time") :
						roomData.get(bossName.replace('3', '1') + " Time") + roomData.get(bossName.replace('3', '2') + " Time"))) : 1);
				chartPanel.setEndTick(((bossName.contains("Verzik") || bossName.contains("Wardens")) && !bossName.contains("P1"))
					? (bossName.contains("P2")) ? roomData.get(bossName + " Time") +
					roomData.get(bossName.replace('2', '1') + " Time") :
					roomData.get(bossName.substring(0, bossName.length() - 2) + "Time") : roomData.get(bossName + " Time"));

				chartPanel.setEndTick(Math.max(chartPanel.endTick + 1, getLastAttackTick(chartData.getAttacks(room))));
			}
			chartPanel.addThrallBoxes(chartData.getThralls(room));
			chartPanel.addLines(roomData.getLines(room));
			chartPanel.addRoomSpecificDatum(roomData.getRoomSpecificData(room));
			chartPanel.setRoomSpecificText(roomData.getRoomSpecificText(room));
			chartPanel.addAutos(roomData.getRoomAutos(room));
			chartPanel.addMaidenCrabs(chartData.maidenCrabs);
			chartPanel.addPlayerChancedDrains(chartData.playerChancedDrain);
			chartPanel.addPlayerStoodInThrownBloods(chartData.playerStoodInThrownBlood);
			chartPanel.addPlayerStoodInSpawnedBloods(chartData.playerStoodInSpawnedBlood);
			chartPanel.addPlayersHanded(chartData.playerHanded);
			chartPanel.addDinhsSpecs(chartData.dinhsSpecs);
			chartPanel.addBadChins(chartData.badChins);
			chartPanel.addPlayerDatumChanged(chartData.playerDataChangeds.get(room));
			if (room.equals(RaidRoom.VERZIK))
			{
				chartPanel.addDawnSpecs(chartData.dawnSpecs);
			}

			chartPanel.redraw();
			basepane.addChangeListener(cl -> chartPanel.redraw());
			panels.add(chartPanel);
			chart.add(chartPanel);
			tab.add(getThemedScrollPane(chart));
			basepane.add(bossName, tab);

			Timer resizeTimer = new Timer(20, e ->
			{
				chartPanel.setSize(frameX, frameY);
			});

			resizeTimer.setRepeats(false);

			addComponentListener(new ComponentAdapter()
			{
				@Override
				public void componentResized(ComponentEvent e)
				{
					super.componentResized(e);
					if (resizeTimer.isRunning()) //redrawing on every resize event will cause severe stuttering, wait 20ms after stopped resizing
					{
						resizeTimer.restart();
					}
					else
					{
						resizeTimer.start();
					}
					Component c = (Component) e.getSource();
					frameX = c.getWidth();
					frameY = c.getHeight();
				}
			});
		}
		add(basepane);
		pack();
	}

	private static int getLastAttackTick(Collection<PlayerDidAttack> attacks)
	{
		int highestDeathTick = 0;
		for (PlayerDidAttack attack : attacks)
		{
			if (attack.getPlayerAnimation().equals(PlayerAnimation.DEATH))
			{
				if (attack.tick > highestDeathTick)
				{
					highestDeathTick = attack.tick;
				}
			}
		}
		return highestDeathTick + 1;
	}
}
package com.advancedraidtracker.ui;

import com.advancedraidtracker.*;
import com.advancedraidtracker.ui.charts.chartcreator.ChartCreatorFrame;
import com.advancedraidtracker.ui.dpsanalysis.DPSWindow;
import com.advancedraidtracker.ui.setups.SetupsWindow;
import com.advancedraidtracker.utility.UISwingUtility;
import com.advancedraidtracker.utility.datautility.datapoints.Raid;
import com.advancedraidtracker.utility.wrappers.RaidsArrayWrapper;
import com.advancedraidtracker.utility.datautility.RaidsManager;
import com.google.inject.Inject;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;

import static com.advancedraidtracker.utility.UISwingUtility.*;
import static com.advancedraidtracker.utility.datautility.DataReader.getAllRaids;
import static com.advancedraidtracker.utility.datautility.DataReader.parsedFiles;

@Slf4j
public class RaidTrackerSidePanel extends PluginPanel
{
	private JLabel raidCountLabel;
	private List<Raid> raidsData;
	private JTable loadRaidsTable;
	private ArrayList<RaidsArrayWrapper> raidSets;

	private Raids raids;

	private AdvancedRaidTrackerPlugin plugin;
	public static AdvancedRaidTrackerConfig config;
	private static ItemManager itemManager;
	private final ConfigManager configManager;
	private final ClientThread clientThread;
	private final Client client;
	private final SpriteManager spriteManager;

	private final JLabel pleaseWait;

	@Inject
	RaidTrackerSidePanel(AdvancedRaidTrackerPlugin plugin, AdvancedRaidTrackerConfig config, ItemManager itemManager, ClientThread clientThread, ConfigManager configManager, SpriteManager spriteManager, Client client)
	{
		UISwingUtility.setConfig(config);
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.spriteManager = spriteManager;
		this.client = client;
		pleaseWait = new JLabel("Parsing Files...", SwingConstants.CENTER);
		add(pleaseWait);
		new Thread(() ->
		{
			RaidTrackerSidePanel.config = config;
			this.plugin = plugin;
			RaidTrackerSidePanel.itemManager = itemManager;
			raidsData = getAllRaids();
			raids = new Raids(config, itemManager, clientThread, configManager, spriteManager);
			if (raidsData != null)
			{
				raids.updateFrameData(raidsData);
			}
			removeAll();
			buildComponents();
			updateUI();
			try
			{
				DPSWindow.loadData();
			}
			catch (Exception e)
			{
				log.info("Failed to load npc data");
				e.printStackTrace();
			}
		}).start();
	}

	private void buildComponents()
	{
		JPanel container = new JPanel();
		JPanel primaryContainer = new JPanel();

		primaryContainer.setLayout(new GridLayout(0, 1));

		JButton viewRaidsButton = new JButton("View All Raids");
		JButton refreshRaidsButton = new JButton("Refresh");

		JButton tableRaidsButton = new JButton("View Saved Raids From Table");

		viewRaidsButton.addActionListener(
			al ->
				new Thread(() ->
				{
					if (raids.hasShelfedData)
					{
						raids.clearFrameData();
						raids.restoreShelfedData();
					}
					raids.repaint();
					raids.open();
				}).start());

		refreshRaidsButton.addActionListener(
			al ->
				new Thread(() ->
				{
					viewRaidsButton.setEnabled(false);
					tableRaidsButton.setEnabled(false);
					raidCountLabel.setText("Refreshing, Please Wait...");
					raidsData = getAllRaids();
					if (raidsData != null)
					{
						raids.updateFrameData(raidsData);
						raids.repaint();
						raids.pack();
					}
					DefaultTableModel model = getTableModel();
					loadRaidsTable.setModel(model);
					viewRaidsButton.setEnabled(true);
					tableRaidsButton.setEnabled(true);
					updateRaidCountLabel();
				}).start());

		tableRaidsButton.addActionListener(
			al ->
			{
				raids.shelfFrameData();
				raids.clearFrameData();
				raids.updateFrameData(getTableData());
				raids.repaint();
				raids.open();
			}
		);

		JButton livePanelButton = new JButton("View Live Room");
		livePanelButton.addActionListener(al ->
			plugin.openLiveFrame());

		JButton chartCreatorButton = new JButton("Create A Chart");
		chartCreatorButton.addActionListener(al ->
		{
			ChartCreatorFrame chartCreator = new ChartCreatorFrame(config, itemManager, clientThread, configManager, spriteManager);
			chartCreator.open();
		});

		JButton setupCreatorButton = new JButton("Create a Setup");
		setupCreatorButton.addActionListener(al ->
		{
			SetupsWindow setupCreator = new SetupsWindow(itemManager, clientThread, client);
			setupCreator.open();
		});

		JButton copyLastSplitsButton = new JButton("Copy Last Splits");
		copyLastSplitsButton.addActionListener(al ->
		{
			String lastSplits = plugin.getLastSplits();
			if (lastSplits.isEmpty())
			{
				JFrame messageDialog = new JFrame();
				messageDialog.setAlwaysOnTop(true);
				JOptionPane.showMessageDialog(messageDialog, "No splits found to copy.\nAfter leaving a tracked PVM encounter, pressing this button will copy the room/wave splits to the clipboard to paste.", "Dialog", JOptionPane.ERROR_MESSAGE);
			}
			else
			{
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(new StringSelection(lastSplits), null);
			}
		});

		JButton viewDPSComparisons = new JButton("View DPS Comparisons");
		viewDPSComparisons.addActionListener(al ->
		{
			DPSWindow dpsWindow = new DPSWindow(itemManager, spriteManager, clientThread);
		});

		raidCountLabel = new JLabel("", SwingConstants.CENTER);
		updateRaidCountLabel();
		primaryContainer.add(raidCountLabel);
		primaryContainer.add(refreshRaidsButton);
		primaryContainer.add(viewRaidsButton);
		primaryContainer.add(tableRaidsButton);
		primaryContainer.add(livePanelButton);
		primaryContainer.add(chartCreatorButton);
		primaryContainer.add(setupCreatorButton);
		primaryContainer.add(copyLastSplitsButton);
		primaryContainer.add(viewDPSComparisons);

		DefaultTableModel model = getTableModel();
		loadRaidsTable = new JTable(model)
		{
			@Override
			public Class<?> getColumnClass(int column)
			{
				if (column == 0)
				{
					return String.class;
				}
				return Boolean.class;
			}
		};

		loadRaidsTable.setPreferredScrollableViewportSize(loadRaidsTable.getPreferredScrollableViewportSize());
		JScrollPane scrollPane = new JScrollPane(loadRaidsTable);
		scrollPane.setPreferredSize(new Dimension(225, scrollPane.getPreferredSize().height));

		JPanel sumPanel = new JPanel();
		sumPanel.setLayout(new BorderLayout());
		JLabel sumLabel = new JLabel("Total Time: 0:00.0");
		JTextArea inputArea = new JTextArea(5, 20);
		inputArea.setLineWrap(true);
		inputArea.setWrapStyleWord(true);

		// Add a DocumentListener to the inputArea to listen for changes and update the sum
		inputArea.getDocument().addDocumentListener(new DocumentListener()
		{
			public void insertUpdate(DocumentEvent e)
			{
				updateSum();
			}

			public void removeUpdate(DocumentEvent e)
			{
				updateSum();
			}

			public void changedUpdate(DocumentEvent e)
			{
				updateSum();
			}

			private void updateSum()
			{
				String text = inputArea.getText();
				String[] lines = text.split("\\r?\\n");
				double totalSeconds = 0;
				for (String line : lines)
				{
					double time = parseTime(line);
					totalSeconds += time;
				}
				int totalMinutes = (int) (totalSeconds / 60);
				double remainingSeconds = totalSeconds % 60;
				String sumText = String.format("Total Time: %d:%05.2f", totalMinutes, remainingSeconds);
				sumLabel.setText(sumText);
			}

			private double parseTime(String s)
			{
				s = s.trim();
				if (s.isEmpty())
				{
					return 0;
				}
				try
				{
					if (s.contains(":"))
					{
						String[] parts = s.split(":");
						if (parts.length == 2)
						{
							double minutes = 0;
							if (!parts[0].isEmpty())
							{
								minutes = Double.parseDouble(parts[0]);
							}
							double seconds = Double.parseDouble(parts[1]);
							return minutes * 60 + seconds;
						}
						else
						{
							return 0; // Invalid format
						}
					}
					else
					{
						double seconds = Double.parseDouble(s);
						return seconds;
					}
				}
				catch (NumberFormatException e)
				{
					return 0; // Invalid number format
				}
			}
		});

		// Add the input area and sum label to the sumPanel
		sumPanel.add(new JLabel("Enter Times (one per line):"), BorderLayout.NORTH);
		sumPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
		sumPanel.add(sumLabel, BorderLayout.SOUTH);

		// Set preferred size for the sumPanel
		sumPanel.setPreferredSize(new Dimension(225, 150));

		container.setLayout(new BorderLayout());
		container.add(primaryContainer, BorderLayout.NORTH);
		container.add(sumPanel, BorderLayout.CENTER);
		container.add(scrollPane, BorderLayout.SOUTH);

		add(container);
	}

	private DefaultTableModel getTableModel()
	{
		Object[] columnNames = {"File Name", "Include?"};
		raidSets = RaidsManager.getRaidsSets();
		Object[][] tableData = new Object[raidSets.size()][2];
		for (int i = 0; i < raidSets.size(); i++)
		{
			tableData[i] = new Object[]{raidSets.get(i).filename, false};
		}
		return new DefaultTableModel(tableData, columnNames);
	}

	/**
	 * @return data in the table
	 */
	private List<Raid> getTableData()
	{
		ArrayList<String> includedSets = new ArrayList<>();
		for (int i = 0; i < loadRaidsTable.getRowCount(); i++)
		{
			if ((boolean) loadRaidsTable.getValueAt(i, 1))
			{
				includedSets.add((String) loadRaidsTable.getValueAt(i, 0));
			}
		}
		List<Raid> collectedRaids = new ArrayList<>();
		for (String set : includedSets)
		{
			for (RaidsArrayWrapper raidWrapper : raidSets)
			{
				if (set.equals(raidWrapper.filename))
				{
					collectedRaids.addAll(raidWrapper.data);
				}
			}
		}
		return collectedRaids;
	}

	private void updateRaidCountLabel()
	{
		raidCountLabel.setText("New Raids Found: " + raidsData.size() + " (" + parsedFiles.size() + " Total)");
	}

}

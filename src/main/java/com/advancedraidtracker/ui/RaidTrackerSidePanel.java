package com.advancedraidtracker.ui;

import com.advancedraidtracker.AdvancedRaidTrackerPlugin;
import com.advancedraidtracker.SimpleTOBData;
import com.advancedraidtracker.AdvancedRaidTrackerConfig;
import com.advancedraidtracker.utility.wrappers.RaidsArrayWrapper;
import com.advancedraidtracker.utility.datautility.RaidsManager;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static com.advancedraidtracker.utility.datautility.DataWriter.PLUGIN_DIRECTORY;

@Slf4j
public class RaidTrackerSidePanel extends PluginPanel
{
    private JLabel raidCountLabel;
    private ArrayList<SimpleTOBData> raidsData;
    private JTable loadRaidsTable;
    private ArrayList<RaidsArrayWrapper> raidSets;

    private Raids raids;

    private AdvancedRaidTrackerPlugin plugin;
    private static AdvancedRaidTrackerConfig config;
    private static ItemManager itemManager;
    private final ConfigManager configManager;

    private JLabel pleaseWait = new JLabel("Parsing Files...", SwingConstants.CENTER);

    @Inject
    RaidTrackerSidePanel(AdvancedRaidTrackerPlugin plugin, AdvancedRaidTrackerConfig config, ItemManager itemManager, ClientThread clientThread, ConfigManager configManager)
    {
        this.configManager = configManager;
        add(pleaseWait);
        new Thread(() ->
        {
            RaidTrackerSidePanel.config = config;
            this.plugin = plugin;
            RaidTrackerSidePanel.itemManager = itemManager;
            raidsData = new ArrayList<>();
            raidsData = getAllRaids();
            raids = new Raids(config, itemManager, clientThread, configManager);
            removeAll();
            buildComponents();
            updateUI();
        }).start();
    }

    private ArrayList<SimpleTOBData> getAllRaids()
    {
        ArrayList<SimpleTOBData> raids = new ArrayList<>();
        try
        {
            File logDirectory = new File(PLUGIN_DIRECTORY);
            File[] logFiles = logDirectory.listFiles();
            int raidCount = 0;
            if(logFiles != null)
            {
                for(File file : logFiles)
                {
                    if(file.isDirectory())
                    {
                        File subDirectory = new File(file.getAbsolutePath()+"/primary/");
                        File[] subDirectoryFiles = subDirectory.listFiles();
                        if(subDirectoryFiles != null)
                        {
                            raidCount += subDirectoryFiles.length;
                        }
                    }
                }
            }
            int index = 0;
            if (logFiles != null)
            {
                for (File file : logFiles)
                {
                    if (file.isDirectory())
                    {
                        File subDirectory = new File(file.getAbsolutePath()+"/primary/");
                        File[] subLogFiles = subDirectory.listFiles();
                        if(subLogFiles != null)
                        {
                            for (File dataFile : subLogFiles)
                            {
                                if (!dataFile.isDirectory())
                                {
                                    if (dataFile.getName().contains("tobdata"))
                                    {
                                        File currentFile = new File(subDirectory.getAbsolutePath() + "/" + dataFile.getName());
                                        parseLogFile(raids, currentFile, subDirectory.getAbsolutePath() + "/" + dataFile.getName());
                                        index++;
                                        if(raidCountLabel != null)
                                        {
                                            raidCountLabel.setText("Refreshing...(" + index + "/" + raidCount + ")");
                                        }
                                        else if(pleaseWait != null)
                                        {
                                            pleaseWait.setText("Parsing Files...(" + index + "/" + raidCount + ")");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.info("Could not retrieve raids");
            e.printStackTrace();
        }
        raids.sort(Comparator.comparing(SimpleTOBData::getDate));
        return raids;
    }

    public static void parseLogFile(ArrayList<SimpleTOBData> raids, File currentFile, String filePath) throws Exception
    {
        Scanner logReader = new Scanner(Files.newInputStream(currentFile.toPath()));
        ArrayList<String> raid = new ArrayList<>();
        boolean raidActive = false;
        boolean spectate = false;
        boolean lateStart = false;
        while (logReader.hasNextLine())
        {
            String line = logReader.nextLine();
            String[] lineSplit = line.split(",");
            if (!raidActive)
            {
                if (lineSplit.length > 3)
                {
                    if (Integer.parseInt(lineSplit[3]) == 0)
                    {
                        raid.add(line);
                        raidActive = true;
                        spectate = false;
                        lateStart = false;
                    }
                }
            } else
            {
                if (lineSplit.length > 3)
                {
                    int value = Integer.parseInt(lineSplit[3]);
                    if(value != 0)
                    {
                        if(value != 801 && value != 576 && value != 587)
                        {
                            if (Integer.parseInt(lineSplit[3]) == 99 && !spectate)
                            {
                                spectate = true;
                                raid.add(line);
                            } else if (value == 98 && !lateStart)
                            {
                                lateStart = true;
                                raid.add(line);
                            } else if (Integer.parseInt(lineSplit[3]) == 4)
                            {
                                raid.add(line);
                                raidActive = false;
                                raids.add(new SimpleTOBData(raid.toArray(new String[0]), filePath, currentFile.getName()));
                                raid.clear();
                            } else if (value != 99 && value != 98)
                            {
                                raid.add(line);
                            }
                        }
                    }
                }
            }
        }
        logReader.close();
    }

    private void buildComponents()
    {
        JPanel container = new JPanel();
        JPanel primaryContainer = new JPanel();

        primaryContainer.setLayout(new GridLayout(5, 1));

        JButton viewRaidsButton = new JButton("View All Raids");
        JButton refreshRaidsButton = new JButton("Refresh");

        JButton tableRaidsButton = new JButton("View Saved Raids From Table");

        viewRaidsButton.addActionListener(
                al ->
                        new Thread(() ->
                        {
                            raids = new Raids(config, itemManager, plugin.clientThread, configManager);
                            raids.createFrame(raidsData);
                            raids.getContentPane().setBackground(Color.BLACK);
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
                            DefaultTableModel model = getTableModel();
                            loadRaidsTable.setModel(model);
                            if(raids != null)
                            {
                                raids.clearData();
                                raids = null;
                            }
                            viewRaidsButton.setEnabled(true);
                            tableRaidsButton.setEnabled(true);
                            updateRaidCountLabel();
                        }).start());

        tableRaidsButton.addActionListener(
                al ->
                {
                    raids = new Raids(config, itemManager, plugin.clientThread, configManager);
                    raids.createFrame(getTableData());
                    raids.getContentPane().setBackground(Color.BLACK);
                    raids.repaint();
                    raids.open();
                }
        );

        JButton livePanelButton = new JButton("View Live Room");
        livePanelButton.addActionListener(al ->
                plugin.openLiveFrame());

        raidCountLabel = new JLabel("", SwingConstants.CENTER);
        updateRaidCountLabel();
        primaryContainer.add(raidCountLabel);
        primaryContainer.add(refreshRaidsButton);
        primaryContainer.add(viewRaidsButton);
        primaryContainer.add(tableRaidsButton);
        primaryContainer.add(livePanelButton);

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
        container.add(primaryContainer);
        container.add(scrollPane);
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

    private ArrayList<SimpleTOBData> getTableData()
    {
        ArrayList<String> includedSets = new ArrayList<>();
        for (int i = 0; i < loadRaidsTable.getRowCount(); i++)
        {
            if ((boolean) loadRaidsTable.getValueAt(i, 1))
            {
                includedSets.add((String) loadRaidsTable.getValueAt(i, 0));
            }
        }
        ArrayList<SimpleTOBData> collectedRaids = new ArrayList<>();
        for (RaidsArrayWrapper set : raidSets)
        {
            for (String s : includedSets)
            {
                if (s.equals(set.filename))
                {
                    collectedRaids.addAll(set.data);
                }
            }
        }
        return collectedRaids;
    }

    private void updateRaidCountLabel()
    {
        raidCountLabel.setText("Raids Found: " + raidsData.size());
    }

}

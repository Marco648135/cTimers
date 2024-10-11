package com.advancedraidtracker.ui.charts.chartcreator;

import com.advancedraidtracker.AdvancedRaidTrackerConfig;
import com.advancedraidtracker.ui.AddPresetWindow;
import com.advancedraidtracker.ui.BaseFrame;
import com.advancedraidtracker.ui.PresetManager;
import com.advancedraidtracker.ui.charts.ChartActionType;
import com.advancedraidtracker.ui.charts.ChartChangedEvent;
import static com.advancedraidtracker.ui.charts.ChartConstants.SELECTION_TOOL;
import com.advancedraidtracker.ui.charts.ChartIOData;
import com.advancedraidtracker.ui.charts.ChartListener;
import com.advancedraidtracker.ui.charts.ChartPanel;
import com.advancedraidtracker.ui.charts.ChartSpecCalculatorPanel;
import com.advancedraidtracker.ui.charts.chartelements.OutlineBox;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.clientThread;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.itemManager;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.spriteManager;
import com.advancedraidtracker.ui.dpsanalysis.DPSWindow;
import com.advancedraidtracker.ui.dpsanalysis.EquipmentData;
import com.advancedraidtracker.ui.dpsanalysis.NPCData;
import com.advancedraidtracker.ui.dpsanalysis.Preset;
import com.advancedraidtracker.utility.weapons.PlayerAnimation;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.List;

import static com.advancedraidtracker.ui.charts.ChartConstants.ADD_ATTACK_TOOL;
import static com.advancedraidtracker.ui.charts.ChartIO.loadChartFromClipboard;
import static com.advancedraidtracker.utility.UISwingUtility.*;
import net.runelite.client.util.AsyncBufferedImage;

@Slf4j
public class ChartCreatorFrame extends BaseFrame implements ChartListener
{
	private final ChartPanel chart;
	private final JTree tree;
	private Map<String, Preset> presets = new HashMap<>();
	private JList<Preset> equipmentSetupsList;
	private JScrollPane equipmentScrollPane;

	public static Preset selectedPrimaryPreset = null;
	public static Preset selectedSecondaryPreset = null;
	private PlayerAnimation selectedPrimary = PlayerAnimation.NOT_SET;
	private PlayerAnimation selectedSecondary = PlayerAnimation.NOT_SET;
	private Map<CustomPanel, MultiSplitPane> panelParentMap = new HashMap<>();

	public ChartCreatorFrame(AdvancedRaidTrackerConfig config, ItemManager itemManager, ClientThread clientThread, ConfigManager configManager, SpriteManager spriteManager)
	{
		OverlayPane overlayPane = new OverlayPane();
		setGlassPane(overlayPane);
		overlayPane.setVisible(false);
		PresetManager.loadPresets();
		equipmentSetupsList = new JList<>();
		equipmentSetupsList.setCellRenderer(new PresetListCellRenderer(itemManager));
		equipmentSetupsList.setFixedCellHeight(40);
		equipmentScrollPane = new JScrollPane(equipmentSetupsList);
		equipmentScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		equipmentScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		setTitle("Chart Creator");
		chart = new ChartPanel("Creator", false, config, clientThread, configManager, itemManager, spriteManager);
		//chart.setPreferredSize(new Dimension(0, 0));
		setPlayerCount(5);
		setEndTick(50);
		setStartTick(1);
		setPrimaryTool(PlayerAnimation.SCYTHE);
		setSecondaryTool(PlayerAnimation.NOT_SET);
		ChartTopMenuPanel menu = new ChartTopMenuPanel(this, config);
		menu.setBorder(BorderFactory.createTitledBorder("Menu"));
		menu.setPreferredSize(new Dimension(0, 50));
		menu.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

		CustomPanel toolsContainer = new CustomPanel("Tools", overlayPane);
		ChartToolPanel tools = new ChartToolPanel(config, this, itemManager, clientThread, spriteManager);
		toolsContainer.getContentPanel().setLayout(new BorderLayout());
		toolsContainer.getContentPanel().add(tools, BorderLayout.CENTER);
		//tools.setBorder(BorderFactory.createTitledBorder("Tools"));
		//tools.setPreferredSize(new Dimension(350, 0));

		// Create the main vertical split
		MultiSplitPane mainPane = new MultiSplitPane(false); // Vertical orientation

		// Create left horizontal split
		MultiSplitPane leftPane = new MultiSplitPane(true); // Horizontal orientation

		MultiSplitPane centerPane = new MultiSplitPane(true);

		// Create right vertical split
		MultiSplitPane rightPane = new MultiSplitPane(true); // Vertical orientation

		// Add left and right panes to the mainPane

// In ChartCreatorFrame constructor


		CustomPanel chartContainer = new CustomPanel("Chart", overlayPane);
		chartContainer.getContentPanel().setLayout(new BorderLayout());
		chartContainer.getContentPanel().add(chart, BorderLayout.CENTER);

		ChartStatusBar chartStatusBar = new ChartStatusBar("");
		chart.setStatusBar(chartStatusBar);
		chart.setToolSelection(ADD_ATTACK_TOOL);
		CustomPanel specCalculatorContainer = new CustomPanel("Preach/Ring Calculator", overlayPane);
		ChartSpecCalculatorPanel specCalculator = new ChartSpecCalculatorPanel(config);
		chart.addChartListener(specCalculator);
		chart.addChartListener(this);

		specCalculatorContainer.getContentPanel().setLayout(new BorderLayout());
		specCalculatorContainer.getContentPanel().add(specCalculator, BorderLayout.CENTER);

		presets = PresetManager.getPresets();

// Initialize the equipment setups list

// Populate the list with presets
		DefaultListModel<Preset> equipmentListModel = new DefaultListModel<>();
		for (Preset preset : presets.values())
		{
			equipmentListModel.addElement(preset);
		}
		equipmentSetupsList.setModel(equipmentListModel);

		equipmentSetupsList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				int index = equipmentSetupsList.locationToIndex(e.getPoint());
				if (index >= 0)
				{
					Preset preset = equipmentSetupsList.getModel().getElementAt(index);
					if (SwingUtilities.isLeftMouseButton(e))
					{
						// Associate this preset with the primary weapon
						selectedPrimaryPreset = preset;
						selectedPrimary = getPlayerAnimationFromPreset(preset);
						setPrimaryTool(selectedPrimary);
					}
					else if (SwingUtilities.isRightMouseButton(e))
					{
						// Associate this preset with the secondary weapon
						selectedSecondaryPreset = preset;
						selectedSecondary = getPlayerAnimationFromPreset(preset);
						setSecondaryTool(selectedSecondary);
					}
					updateEquipmentSelection();
				}
			}
		});


		tree = getThemedTree("Chart Actions");
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
		root.add(new DefaultMutableTreeNode("Attacks"));
		root.add(new DefaultMutableTreeNode("Lines"));
		root.add(new DefaultMutableTreeNode("Text"));
		root.add(new DefaultMutableTreeNode("Autos"));
		root.add(new DefaultMutableTreeNode("Thralls"));

		tree.addTreeSelectionListener(new TreeSelectionListener()
		{
			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				List<OutlineBox> boxesToSelect = new ArrayList<>();
				for (TreePath tp : tree.getSelectionModel().getSelectionPaths())
				{
					for (Object obj : tp.getPath())
					{
						DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) obj;
						if (dmtn.getUserObject() instanceof OutlineBox)
						{
							boxesToSelect.add((OutlineBox) dmtn.getUserObject());
						}
					}
				}
				chart.chartSelectionChanged(boxesToSelect);
				chart.setToolSelection(SELECTION_TOOL);
				tools.setTool(SELECTION_TOOL);
			}
		});

// Create the tree container
		JPanel treeContainer = getThemedPanel();
		treeContainer.setLayout(new BorderLayout());
		treeContainer.add(new JScrollPane(tree), BorderLayout.CENTER); // Make sure the tree is scrollable if needed

// Create the equipment list container
		JPanel equipmentListContainer = getTitledPanel("Equipment Setups");
		equipmentListContainer.setLayout(new BorderLayout());
		equipmentListContainer.add(equipmentScrollPane, BorderLayout.CENTER);


		JButton addSetupButton = new JButton("+");
		addSetupButton.addActionListener(e -> openCreateEquipmentPresetWindow());
		equipmentListContainer.add(addSetupButton, BorderLayout.SOUTH);


		CustomPanel chartActionsContainer = new CustomPanel("Chart Actions", overlayPane);
		chartActionsContainer.getContentPanel().setLayout(new BorderLayout());
		chartActionsContainer.getContentPanel().add(treeContainer, BorderLayout.CENTER);

		CustomPanel equipmentSetupsContainer = new CustomPanel("Equipment Setups", overlayPane);
		equipmentSetupsContainer.getContentPanel().setLayout(new BorderLayout());
		equipmentSetupsContainer.getContentPanel().add(equipmentListContainer, BorderLayout.CENTER);


		leftPane.addComponent(chartActionsContainer);
		leftPane.addComponent(equipmentSetupsContainer);
		panelParentMap.put(chartActionsContainer, leftPane);
		panelParentMap.put(equipmentSetupsContainer, leftPane);

		specCalculatorContainer.setPreferredSize(new Dimension(0, 150));
		chartContainer.setPreferredSize(new Dimension(0, 0));

		centerPane.addComponent(chartContainer);
		centerPane.addComponent(specCalculatorContainer);

		panelParentMap.put(chartContainer, centerPane);
		panelParentMap.put(specCalculatorContainer, centerPane);


		rightPane.addComponent(toolsContainer); // Panel C
		panelParentMap.put(toolsContainer, rightPane);


		leftPane.setPreferredSize(new Dimension(200, 0));
		centerPane.setPreferredSize(new Dimension(0, 0));
		rightPane.setPreferredSize(new Dimension(250, 0));


		mainPane.addComponent(leftPane);
		mainPane.addComponent(centerPane);
		mainPane.addComponent(rightPane);

		setLayout(new BorderLayout());
		add(menu, BorderLayout.NORTH);
		add(mainPane, BorderLayout.CENTER);
		add(chartStatusBar, BorderLayout.SOUTH);

		JMenuBar menuBar = new JMenuBar();

		JMenu fileMenu = getThemedMenu("File");

		JMenuItem newMenu = getThemedMenuItem("New...");
		newMenu.setAccelerator(KeyStroke.getKeyStroke('N', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
		newMenu.addActionListener(o ->
		{
			chart.newFile();
		});

		JMenuItem importFromClipboard = getThemedMenuItem("Import from clipboard");

		importFromClipboard.addActionListener(o ->
		{
			try
			{
				ChartIOData data = loadChartFromClipboard((String) Toolkit.getDefaultToolkit()
					.getSystemClipboard().getData(DataFlavor.stringFlavor));
				chart.applyFromSave(data);
			}
			catch (Exception e)
			{
				log.info("Failed to copy");
			}

		});

		JMenuItem openMenu = getThemedMenuItem("Open...");
		openMenu.addActionListener(o ->
		{
			chart.openFile();
		});
		openMenu.setAccelerator(KeyStroke.getKeyStroke('O', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

		JMenuItem saveMenu = getThemedMenuItem("Save...");
		saveMenu.addActionListener(o ->
		{
			chart.saveFile();
		});
		saveMenu.setAccelerator(KeyStroke.getKeyStroke('S', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

		JMenuItem saveAsMenu = getThemedMenuItem("Save As...");
		saveAsMenu.addActionListener(o ->
		{
			chart.saveAs();
		});
		saveAsMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.Event.SHIFT_MASK));

		JMenuItem exportMenu = getThemedMenuItem("Export to Image");
		exportMenu.addActionListener(o ->
		{
			chart.exportImage();
		});

		JMenuItem exportAttackData = getThemedMenuItem("Copy Attack Data to Clipboard");
		exportAttackData.addActionListener(o ->
		{
			chart.copyAttackData();
		});

		fileMenu.add(newMenu);
		fileMenu.add(openMenu);
		fileMenu.add(saveMenu);
		fileMenu.add(saveAsMenu);
		fileMenu.add(exportMenu);
		fileMenu.add(importFromClipboard);
		fileMenu.add(exportAttackData);

		menuBar.add(fileMenu);

		JMenu viewMenu = new JMenu("View");

// List of all possible panels
		CustomPanel[] panels = { chartContainer, toolsContainer, chartActionsContainer, equipmentSetupsContainer, specCalculatorContainer}; // Add all your panels here

		for (CustomPanel panel : panels) {
			JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(panel.title, true);

			menuItem.addActionListener(e -> {
				boolean selected = menuItem.isSelected();
				if (selected) {
					// Show the panel
					MultiSplitPane parentPane = panelParentMap.get(panel);
					if (parentPane != null && !isComponentInPane(panel, parentPane)) {
						parentPane.addComponent(panel);
						parentPane.revalidate();
						parentPane.repaint();
					}
				} else {
					// Hide the panel
					removePanelFromParent(panel);
				}
			});

			viewMenu.add(menuItem);
		}
		menuBar.add(viewMenu);
		setJMenuBar(menuBar);

		Timer resizeTimer = new Timer(20, e ->
		{
			chart.setSize();
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
			}
		});

		updateEquipmentSelection();

		pack();
		tools.build();

		this.setExtendedState(this.getExtendedState() | this.MAXIMIZED_BOTH);
	}



	private boolean isComponentInPane(Component comp, MultiSplitPane pane) {
		return Arrays.asList(pane.getComponents()).contains(comp);
	}

	private void removePanelFromParent(CustomPanel panel) {
		Container parent = panel.getParent();
		if (parent instanceof MultiSplitPane) {
			MultiSplitPane splitPane = (MultiSplitPane) parent;
			splitPane.removeComponent(panel);
		} else if (parent instanceof JTabbedPane) {
			JTabbedPane tabbedPane = (JTabbedPane) parent;
			int index = tabbedPane.indexOfComponent(panel);
			if (index != -1) {
				tabbedPane.remove(index);
			}

			// If tabbedPane is empty after removal, remove it from its parent
			if (tabbedPane.getTabCount() == 0) {
				Container tabParent = tabbedPane.getParent();
				if (tabParent instanceof MultiSplitPane) {
					MultiSplitPane splitPane = (MultiSplitPane) tabParent;
					splitPane.removeComponent(tabbedPane);
				} else {
					tabParent.remove(tabbedPane);
					tabParent.revalidate();
					tabParent.repaint();
				}
			}
		}
	}


	private void openCreateEquipmentPresetWindow()
	{
		new AddPresetWindow();
	}

	private void refreshEquipmentSetupsList()
	{
		// Reload presets
		PresetManager.loadPresets();
		presets = PresetManager.getPresets();

		// Update the list model
		DefaultListModel<Preset> equipmentListModel = new DefaultListModel<>();
		for (Preset preset : presets.values())
		{
			equipmentListModel.addElement(preset);
		}
		equipmentSetupsList.setModel(equipmentListModel);
	}

	public void setPlayerCount(int players)
	{
		List<String> playerList = new ArrayList<>();
		for (int i = 1; i < players + 1; i++)
		{
			playerList.add("Player" + i);
		}
		chart.setAttackers(playerList);
		chart.redraw();
	}

	public void setStartTick(int tick)
	{
		chart.setStartTick(tick);
	}

	public void setEndTick(int tick)
	{
		chart.setEndTick(tick);
	}

	public void setPrimaryTool(PlayerAnimation tool)
	{
		chart.setPrimaryTool(tool);
		selectedPrimary = tool;
		updateEquipmentSelection();
	}

	public void setSecondaryTool(PlayerAnimation tool)
	{
		chart.setSecondaryTool(tool);
		selectedSecondary = tool;
		updateEquipmentSelection();
	}

	private void updateEquipmentSelection()
	{
		// If the selected presets no longer match the tools, reset them
		if (selectedPrimaryPreset != null)
		{
			PlayerAnimation anim = getPlayerAnimationFromPreset(selectedPrimaryPreset);
			if (anim != selectedPrimary)
			{
				selectedPrimaryPreset = null;
			}
		}

		if (selectedSecondaryPreset != null)
		{
			PlayerAnimation anim = getPlayerAnimationFromPreset(selectedSecondaryPreset);
			if (anim != selectedSecondary)
			{
				selectedSecondaryPreset = null;
			}
		}

		// Find matching presets if not set
		if (selectedPrimaryPreset == null)
		{
			selectedPrimaryPreset = findPresetWithWeapon(selectedPrimary);
		}

		if ((selectedSecondaryPreset == null || selectedSecondaryPreset == selectedPrimaryPreset))
		{
			selectedSecondaryPreset = findPresetWithWeapon(selectedSecondary, selectedPrimaryPreset);
		}

		// Repaint the list to reflect the selection
		equipmentSetupsList.repaint();
	}

	private Preset findPresetWithWeapon(PlayerAnimation anim)
	{
		return findPresetWithWeapon(anim, null);
	}

	private Preset findPresetWithWeapon(PlayerAnimation anim, Preset excludePreset)
	{
		if (anim == null || anim.weaponIDs == null || anim.weaponIDs.length == 0)
		{
			return null;
		}
		int[] weaponIDs = anim.weaponIDs;
		for (Preset preset : presets.values())
		{
			if (preset == excludePreset)
			{
				continue;
			}
			EquipmentData weapon = preset.getEquipment().get("weapon");
			if (weapon != null)
			{
				int itemId = weapon.getId();
				for (int weaponID : weaponIDs)
				{
					if (itemId == weaponID)
					{
						return preset;
					}
				}
			}
		}
		return null;
	}

	private PlayerAnimation getPlayerAnimationFromPreset(Preset preset)
	{
		EquipmentData weapon = preset.getEquipment().get("weapon");
		if (weapon != null)
		{
			int weaponId = weapon.getId();
			for (PlayerAnimation anim : PlayerAnimation.values())
			{
				if (anim.weaponIDs != null)
				{
					for (int id : anim.weaponIDs)
					{
						if (id == weaponId)
						{
							return anim;
						}
					}
				}
			}
		}
		return PlayerAnimation.NOT_SET;
	}

	public void setEnforceCD(boolean bool)
	{
		chart.setEnforceCD(bool);
	}

	public void setToolSelection(int tool)
	{
		chart.setToolSelection(tool);
	}

	public void changeLineText(String text)
	{
		chart.setManualLineText(text);
	}

	@Override
	public void onChartChanged(ChartChangedEvent event)
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
		for (Object object : event.chartObjects)
		{
			if (event.actionType == ChartActionType.ADD_ELEMENT)
			{
				DefaultMutableTreeNode node = getNodeByObject(event.objectType.name, root, true);
				if (node != null)
				{
					node.add(new DefaultMutableTreeNode(object));
				}
			}
			else if (event.actionType == ChartActionType.REMOVE_ELEMENT)
			{
				DefaultMutableTreeNode parentNode = getNodeByObject(event.objectType.name, root, true);
				if (parentNode != null)
				{
					DefaultMutableTreeNode node = getNodeByObject(object, root, false);
					if (node != null)
					{
						parentNode.remove(node);
					}
				}
			}
		}
		reloadTreeButPreserveExpandState();
	}


	public static DefaultMutableTreeNode getNodeByObject(Object object, DefaultMutableTreeNode parentNode, boolean compareByValue)
	{
		DefaultMutableTreeNode treeNode;
		int size = parentNode.getChildCount();

		List<DefaultMutableTreeNode> parentNodes = new ArrayList<>();
		for (int i = 0; i < size; i++)
		{
			treeNode = (DefaultMutableTreeNode) parentNode.getChildAt(i);
			if (compareByValue)
			{
				if (treeNode.getUserObject().equals(object))
				{
					return treeNode;
				}
			}
			else
			{
				if (treeNode.getUserObject() == object)
				{
					return treeNode;
				}
			}
			if (treeNode.getChildCount() > 0)
			{
				parentNodes.add(treeNode);
			}
		}
		for (DefaultMutableTreeNode node : parentNodes)
		{
			treeNode = getNodeByObject(object, node, compareByValue);
			if (treeNode != null)
			{
				return treeNode;
			}
		}
		return null;
	}

	public void reloadTreeButPreserveExpandState()
	{
		List<TreePath> expanded = new ArrayList<>();
		for (int i = 0; i < tree.getRowCount() - 1; i++)
		{
			TreePath currPath = tree.getPathForRow(i);
			TreePath nextPath = tree.getPathForRow(i + 1);
			if (currPath.isDescendant(nextPath))
			{
				expanded.add(currPath);
			}
		}
		((DefaultTreeModel) tree.getModel()).reload();
		for (TreePath path : expanded)
		{
			tree.expandPath(path);
		}
	}

	class PresetListCellRenderer extends DefaultListCellRenderer
	{
		private final ItemManager itemManager;
		private final Map<Integer, ImageIcon> iconCache = new HashMap<>();

		public PresetListCellRenderer(ItemManager itemManager)
		{
			this.itemManager = itemManager;
		}

		@Override
		public Component getListCellRendererComponent(
			JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			Preset preset = (Preset) value;
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			label.setText(preset.getName());
			label.setIcon(null); // Clear existing icon

			// Get the weapon EquipmentData
			EquipmentData weapon = preset.getEquipment().get("weapon");
			if (weapon != null)
			{
				int itemId = weapon.getId();

				// Check if icon is cached
				if (iconCache.containsKey(itemId))
				{
					label.setIcon(iconCache.get(itemId));
				}
				else
				{
					AsyncBufferedImage itemImage = itemManager.getImage(itemId);

					itemImage.onLoaded(() -> {
						Image image = itemImage.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
						ImageIcon icon = new ImageIcon(image);
						iconCache.put(itemId, icon);
						// Update icon on the label
						label.setIcon(icon);
						// Repaint list cell to show the icon
						list.repaint(list.getCellBounds(index, index));
					});
				}
			}

			// Apply border outline for selection
			label.setBorder(null); // Reset border
			if (preset == selectedPrimaryPreset)
			{
				label.setBorder(BorderFactory.createLineBorder(new Color(51, 99, 140, 180), 2));
			}
			else if (preset == selectedSecondaryPreset)
			{
				label.setBorder(BorderFactory.createLineBorder(new Color(224, 124, 79, 180), 2));
			}
			else
			{
				label.setBorder(null);
			}

			// Reset background and foreground colors
			if (isSelected)
			{
				label.setBackground(list.getSelectionBackground());
				label.setForeground(list.getSelectionForeground());
			}
			else
			{
				label.setBackground(list.getBackground());
				label.setForeground(list.getForeground());
			}

			return label;
		}
	}

	public void setTarget(String target)
	{
		chart.setTarget(DPSWindow.getNPCFromName(target));
	}

}



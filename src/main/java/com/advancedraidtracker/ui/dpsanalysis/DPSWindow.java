package com.advancedraidtracker.ui.dpsanalysis;

import com.advancedraidtracker.AdvancedRaidTrackerPlugin;
import com.advancedraidtracker.ui.BaseFrame;
import static com.advancedraidtracker.ui.charts.ChartIO.gson;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.clientThread;
import static com.advancedraidtracker.utility.datautility.DataWriter.PLUGIN_DIRECTORY;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.event.*;
import java.awt.*;
import lombok.extern.slf4j.Slf4j;
import javax.swing.event.DocumentListener;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

@Slf4j
public class DPSWindow extends BaseFrame
{
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	List<String> allNPCs = new ArrayList<>();
	List<NPCData> allNPCData = new ArrayList<>();
	private Map<String, List<EquipmentData>> equipmentMap = new HashMap<>();

	private final Map<String, Set<String>> trigramIndex = new HashMap<>();

	private Map<String, EquipmentData> selectedEquipment = new HashMap<>();

	// Labels for cumulative bonuses
	// Attack Bonus labels
	private JLabel attackStabLabel;
	private JLabel attackSlashLabel;
	private JLabel attackCrushLabel;
	private JLabel attackMagicLabel;
	private JLabel attackRangeLabel;

	// Defense Bonus labels
	private JLabel defenseStabLabel;
	private JLabel defenseSlashLabel;
	private JLabel defenseCrushLabel;
	private JLabel defenseMagicLabel;
	private JLabel defenseRangeLabel;

	// Other Bonuses labels
	private JLabel meleeStrengthLabel;
	private JLabel rangedStrengthLabel;
	private JLabel magicDamageLabel;
	private JLabel prayerLabel;

	// Target-Specific labels
	private JLabel undeadLabel;
	private JLabel slayerLabel;

	private JLabel prayerAttackBonusLabel;
	private JLabel prayerStrengthBonusLabel;
	private JLabel prayerDefenseBonusLabel;
	private JLabel prayerMagicBonusLabel;
	private JLabel prayerRangeBonusLabel;
	private JLabel prayerRangedDamageBonusLabel;
	private JLabel prayerMagicDamageBonusLabel;

	private Map<String, List<String>> cellDiagnosticInfo = new HashMap<>();
	private final Map<Integer, BufferedImage> npcImages = new ConcurrentHashMap<>();
	private final Set<Integer> imagesBeingDownloaded = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final ExecutorService imageLoaderService = Executors.newFixedThreadPool(5); // Adjust the pool size as needed

	private static final Path DPS_UTILITY_FOLDER = Paths.get(PLUGIN_DIRECTORY, "dpsutility");
	private static final Path EQUIPMENT_PRESETS_FILE = DPS_UTILITY_FOLDER.resolve("equipment.presets");
	private static final Path NPC_PRESETS_FILE = DPS_UTILITY_FOLDER.resolve("npc.presets");

	private final static String initialNPCData = "[]"; // Initial data for npc.presets
	private final static String initialEquipmentData = "[]"; // Initial data for equipment.presets

	public void loadData() throws Exception
	{
		String url = "https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/main/cdn/json/monsters.json";
		Gson reader = gson.newBuilder().create();
		NPCData[] npcDataArray = reader.fromJson(new InputStreamReader(new URL(url).openStream()), NPCData[].class);
		for (NPCData data : npcDataArray)
		{
			String npcName = data.getName() + ", " + data.getVersion() + " (" + data.getLevel() + ")";
			allNPCs.add(npcName);
			allNPCData.add(data);
		}
		buildNPCTrigramIndex();
		// Initialize the equipmentMap
		equipmentMap = new HashMap<>();

		String url2 = "https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/main/cdn/json/equipment.json";
		reader = gson.newBuilder().create();
		EquipmentData[] equipmentDataArray = reader.fromJson(new InputStreamReader(new URL(url2).openStream()), EquipmentData[].class);

		for (EquipmentData data : equipmentDataArray)
		{
			String slot = data.getSlot();
			if (slot == null || slot.isEmpty())
			{
				continue; // Skip equipment without a slot or handle accordingly
			}
			equipmentMap.computeIfAbsent(slot, k -> new ArrayList<>()).add(data);
		}
	}

	private NPCData getNPCFromName(String name)
	{
		for (NPCData npc : allNPCData)
		{
			if (name.equals(npc.getName() + ", " + npc.getVersion() + " (" + npc.getLevel() + ")"))
			{
				return npc;
			}
		}
		return null;
	}

	private void buildNPCTrigramIndex()
	{
		for (String npcName : allNPCs)
		{
			String normalizedName = npcName.toLowerCase().replaceAll("\\s+", "");
			Set<String> trigrams = getTrigrams(normalizedName);
			for (String trigram : trigrams)
			{
				trigramIndex.computeIfAbsent(trigram, k -> new HashSet<>()).add(npcName);
			}
		}
	}

	private JPanel createPrayerSummaryPanel()
	{
		JPanel summaryPanel = new JPanel();
		summaryPanel.setLayout(new GridLayout(0, 2, 5, 5));
		summaryPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));

		prayerAttackBonusLabel = new JLabel("Attack Bonus: 0%");
		prayerStrengthBonusLabel = new JLabel("Strength Bonus: 0%");
		prayerDefenseBonusLabel = new JLabel("Defense Bonus: 0%");
		prayerMagicBonusLabel = new JLabel("Magic Bonus: 0%");
		prayerRangeBonusLabel = new JLabel("Ranged Bonus: 0%");
		prayerMagicDamageBonusLabel = new JLabel("Magic Damage Bonus: 0%");
		prayerRangedDamageBonusLabel = new JLabel("Ranged Damage Bonus: 0%");

		summaryPanel.add(prayerAttackBonusLabel);
		summaryPanel.add(prayerStrengthBonusLabel);
		summaryPanel.add(prayerDefenseBonusLabel);
		summaryPanel.add(prayerMagicBonusLabel);
		summaryPanel.add(prayerRangeBonusLabel);
		summaryPanel.add(prayerMagicDamageBonusLabel);
		summaryPanel.add(prayerRangedDamageBonusLabel);

		return summaryPanel;
	}

	private void updatePrayerSummary()
	{
		int totalAttackBonus = 0;
		int totalStrengthBonus = 0;
		int totalDefenseBonus = 0;
		int totalMagicAttackBonus = 0;
		int totalMagicDamageBonus = 0;
		int totalRangedAttackBonus = 0;
		int totalRangedDamageBonus = 0;

		for (Map.Entry<Prayers, Boolean> entry : selectedPrayers.entrySet())
		{
			if (entry.getValue())
			{
				Prayers prayer = entry.getKey();
				totalAttackBonus += prayer.att;
				totalStrengthBonus += prayer.str;
				totalDefenseBonus += prayer.def;
				totalMagicAttackBonus += prayer.mag;
				totalMagicDamageBonus += prayer.magD;
				totalRangedAttackBonus += prayer.ranA;
				totalRangedDamageBonus += prayer.ranD;
			}
		}

		prayerAttackBonusLabel.setText("Melee Attack Bonus: " + totalAttackBonus + "%");
		prayerStrengthBonusLabel.setText("Melee Strength Bonus: " + totalStrengthBonus + "%");
		prayerDefenseBonusLabel.setText("Defense Bonus: " + totalDefenseBonus + "%");
		prayerMagicBonusLabel.setText("Magic Attack Bonus: " + totalMagicAttackBonus + "%");
		prayerMagicDamageBonusLabel.setText("Magic Damage Bonus: " + totalMagicDamageBonus + "%");
		prayerRangeBonusLabel.setText("Ranged Attack Bonus: " + totalRangedAttackBonus + "%");
		// Add a new label for Ranged Damage Bonus
		prayerRangedDamageBonusLabel.setText("Ranged Damage Bonus: " + totalRangedDamageBonus + "%");
	}

	private Set<String> getTrigrams(String text)
	{
		Set<String> trigrams = new HashSet<>();
		int length = text.length();
		for (int i = 0; i < length - 2; i++)
		{
			String trigram = text.substring(i, i + 3);
			trigrams.add(trigram);
		}
		return trigrams;
	}

	private final Map<Prayers, BufferedImage> disabledPrayerIcons = new HashMap<>();
	private final Map<Prayers, BufferedImage> enabledPrayerIcons = new HashMap<>();

	DefaultTableModel tableModel;

	class CustomCellRenderer extends DefaultTableCellRenderer
	{
		private final JCheckBox checkbox = new JCheckBox();

		@Override
		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			if (row == 1 && column > 0)
			{
				// Render checkbox
				if (value instanceof Boolean)
				{
					checkbox.setSelected((Boolean) value);
				}
				else
				{
					checkbox.setSelected(false);
				}
				checkbox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
				checkbox.setHorizontalAlignment(JLabel.CENTER);
				return checkbox;
			}
			else
			{
				// Default rendering for other cells
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		}
	}

	class CustomCellEditor extends AbstractCellEditor implements TableCellEditor
	{
		private final JCheckBox checkbox = new JCheckBox();
		private final JTable table; // Add this member variable

		public CustomCellEditor(JTable table) // Modify constructor to accept JTable
		{
			this.table = table;
		}

		@Override
		public Object getCellEditorValue()
		{
			return checkbox.isSelected();
		}

		@Override
		public Component getTableCellEditorComponent(
			JTable table, Object value, boolean isSelected, int row, int column)
		{
			if (value instanceof Boolean)
			{
				checkbox.setSelected((Boolean) value);
			}
			else
			{
				checkbox.setSelected(false);
			}
			checkbox.setHorizontalAlignment(JLabel.CENTER);
			checkbox.setBackground(table.getSelectionBackground());
			return checkbox;
		}

		@Override
		public boolean isCellEditable(EventObject anEvent)
		{
			if (anEvent instanceof MouseEvent)
			{
				MouseEvent me = (MouseEvent) anEvent;
				int row = table.rowAtPoint(me.getPoint());
				int column = table.columnAtPoint(me.getPoint());
				return (row == 1 && column > 0);
			}
			return false;
		}
	}

	class CustomHeaderRenderer implements TableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table,
													   Object value,
													   boolean isSelected,
													   boolean hasFocus,
													   int row,
													   int column)
		{
			JLabel label = new JLabel();
			label.setHorizontalAlignment(JLabel.CENTER);
			label.setVerticalAlignment(JLabel.CENTER);
			label.setHorizontalTextPosition(JLabel.RIGHT); // Icon on the left, text on the right
			label.setVerticalTextPosition(JLabel.CENTER);

			String headerText = value != null ? value.toString() : "";

			// For columns beyond the first (index > 0)
			if (column > 0)
			{
				// Get the NPCData for this column
				NPCData npcData = columnToNPCDataMap.get(column);

				if (npcData != null)
				{
					int npcId = npcData.getId();
					BufferedImage img = npcImages.get(npcId);
					if (img != null)
					{
						// Image already loaded; use it
						Image scaledImg = img.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
						ImageIcon icon = new ImageIcon(scaledImg);
						label.setIcon(icon);
					}
					else
					{
						// Image not loaded yet; initiate download if not already downloading
						if (!imagesBeingDownloaded.contains(npcId))
						{
							imagesBeingDownloaded.add(npcId);
							loadNPCImageAsync(npcData, npcId);
						}
						// Optionally, set a placeholder icon
						// label.setIcon(yourPlaceholderIcon);
					}
				}
			}

			// Use HTML to allow text wrapping
			// Limit the width using CSS to control the wrapping
			String htmlHeaderText = "<html><div style='text-align:center; width:80px;'>" + headerText + "</div></html>";
			label.setText(htmlHeaderText);

			// Set border and background to match header style
			label.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			label.setOpaque(true);
			label.setBackground(UIManager.getColor("TableHeader.background"));
			label.setForeground(UIManager.getColor("TableHeader.foreground"));
			label.setFont(UIManager.getFont("TableHeader.font"));

			return label;
		}
	}

	private JTable table;

	private void loadNPCImageAsync(NPCData npcData, int npcId)
	{
		imageLoaderService.submit(() -> {
			try
			{
				String imageFilename = npcData.getImage();
				if (imageFilename != null && !imageFilename.isEmpty())
				{
					// URL-encode the image filename
					String encodedFilename = URLEncoder.encode(imageFilename, StandardCharsets.UTF_8).replace("+", "%20");
					String imageUrl = "https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/main/cdn/monsters/" + encodedFilename;

					BufferedImage img = ImageIO.read(new URL(imageUrl));

					if (img != null)
					{
						npcImages.put(npcId, img);
					}
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				log.error("Error loading image for NPC: " + npcData.getName(), ex);
			}
			finally
			{
				imagesBeingDownloaded.remove(npcId);
			}

			// After loading, trigger UI update on the Event Dispatch Thread
			SwingUtilities.invokeLater(() -> {
				// Repaint the header to show the new image
				table.getTableHeader().repaint();
			});
		});
	}

	private void applyCustomRenderers()
	{
		// Apply the PresetNameCellRenderer to the first column
		table.getColumnModel().getColumn(0).setCellRenderer(new PresetNameCellRenderer());

		// Apply the DPSCellRenderer to all other columns beyond the first
		for (int col = 1; col < table.getColumnCount(); col++)
		{
			table.getColumnModel().getColumn(col).setCellRenderer(new DPSCellRenderer());
		}
	}

	public DPSWindow(ItemManager itemManager, SpriteManager spriteManager, ClientThread clientThread)
	{
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			imageLoaderService.shutdownNow();
		}));

		clientThread.invoke(() ->
		{
			for (Prayers prayers : Prayers.values())
			{
				disabledPrayerIcons.put(prayers, spriteManager.getSprite(prayers.disabledID, 0));
				enabledPrayerIcons.put(prayers, spriteManager.getSprite(prayers.enabledID, 0));
			}
		});


		try
		{
			loadData();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		setSize(new Dimension(800, 600));

		// Create the table model
		tableModel = new DefaultTableModel()
		{
			@Override
			public Class<?> getColumnClass(int columnIndex)
			{
				if (columnIndex == 0)
				{
					return String.class; // First column contains Strings (Preset Names)
				}
				else if (getRowCount() > 1 && getValueAt(1, columnIndex) instanceof Boolean)
				{
					return Boolean.class; // Columns with checkboxes
				}
				else if (getRowCount() > 0 && getValueAt(0, columnIndex) instanceof Integer)
				{
					return Integer.class; // Defense values (if stored as Integers)
				}
				else
				{
					return Double.class; // DPS values (assumed to be Doubles)
				}
			}

			@Override
			public boolean isCellEditable(int row, int column)
			{
				return row == 1 && column > 0; // Editable checkboxes in row 1
			}
		};

		// Create the JTable
		table = new JTable(tableModel);
		// Add initial columns
		tableModel.addColumn("Name");

// Add the 'Defense' row
		tableModel.addRow(new Object[]{"Defense"}); // Row index 0

// Add the '15/16?' row
		Object[] fifteenSixteenRow = new Object[tableModel.getColumnCount()];
		fifteenSixteenRow[0] = "15/16?";
		for (int i = 1; i < fifteenSixteenRow.length; i++)
		{
			fifteenSixteenRow[i] = Boolean.FALSE;
		}
		tableModel.addRow(fifteenSixteenRow); // Row index 1

		table.setDefaultRenderer(Boolean.class, new CustomCellRenderer());
		table.setDefaultEditor(Boolean.class, new CustomCellEditor(table));
		table.getColumnModel().getColumn(0).setCellRenderer(new PresetNameCellRenderer());
		for (int col = 1; col < table.getColumnCount(); col++)
		{
			table.getColumnModel().getColumn(col).setCellRenderer(new DPSCellRenderer());
		}

		// Create a JScrollPane to hold the JTable
		JScrollPane scrollPane = new JScrollPane(table);

		// Create a panel to hold the table header and the '+' button
		JPanel headerPanel = new JPanel(new BorderLayout());

		// Get the table header
		JTableHeader tableHeader = table.getTableHeader();
		tableHeader.setDefaultRenderer(new CustomHeaderRenderer());
		tableHeader.setPreferredSize(new Dimension(-1, 60));

		// Create the '+' button for adding columns
		JButton addColumnButton = new JButton("+");
		addColumnButton.setMargin(new Insets(0, 0, 0, 0));
		addColumnButton.setPreferredSize(new Dimension(30, 20));

		// Add the table header and the '+' button to the header panel
		headerPanel.add(tableHeader, BorderLayout.CENTER);
		headerPanel.add(addColumnButton, BorderLayout.EAST);

		// Create the '+' button for adding rows
		JButton addRowButton = new JButton("+");
		addRowButton.setMargin(new Insets(0, 0, 0, 0));
		addRowButton.setPreferredSize(new Dimension(30, 20));

		JButton editConfigButton = new JButton("Edit Configuration");
		editConfigButton.addActionListener(e -> openEditConfigurationWindow());
		// Create a footer panel to hold the '+' button for rows
		JPanel footerPanel = new JPanel();
		footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.X_AXIS));
		footerPanel.add(addRowButton);
		footerPanel.add(Box.createHorizontalGlue());
		footerPanel.add(editConfigButton);

		// Set the header panel as the column header view
		scrollPane.setColumnHeaderView(headerPanel);

		// Create the main panel and add components
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(headerPanel, BorderLayout.NORTH);
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		mainPanel.add(footerPanel, BorderLayout.SOUTH);

		// Add the main panel to the frame
		add(mainPanel);

		// Add action listeners to the '+' buttons
		addColumnButton.addActionListener(e -> showAddColumnInputBox(addColumnButton, tableModel));
		addRowButton.addActionListener(e -> openCreateFrame()); // Directly open the create frame


		try
		{
			loadData();
			loadPresets(); // Load presets and NPC columns from files
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		pack();
		open();
	}

	private final Map<Integer, ImageIcon> weaponIconCache = new ConcurrentHashMap<>();

	class DPSCellRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			JLabel label = (JLabel) super.getTableCellRendererComponent(
				table, value, isSelected, hasFocus, row, column);

			if (row >= 2 && column >= 1 && value != null)
			{
				try
				{
					double cellValue = Double.parseDouble(value.toString());

					// Get min and max values for this column
					double min = getColumnMinValue(column);
					double max = getColumnMaxValue(column);

					// Get the base background color
					Color baseColor = isSelected ? table.getSelectionBackground() : table.getBackground();

					// Compute color based on value
					Color color = calculateColor(cellValue, min, max, baseColor);

					label.setBackground(color);
					label.setOpaque(true);

					String cellKey = row + "," + column;
					List<String> diagnosticInfo = cellDiagnosticInfo.get(cellKey);
					if (diagnosticInfo != null)
					{
						StringBuilder tooltip = new StringBuilder("<html>");
						for (String line : diagnosticInfo)
						{
							tooltip.append(line).append("<br/>");
						}
						tooltip.append("</html>");
						label.setToolTipText(tooltip.toString());
					}
					else
					{
						label.setToolTipText(null);
					}
				}
				catch (NumberFormatException e)
				{
					// Handle parsing error
					label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
					label.setOpaque(true);
					label.setToolTipText(null);
				}
			}
			else
			{
				label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
				label.setOpaque(true);
				label.setToolTipText(null);
			}

			return label;
		}

		private Color calculateColor(double value, double min, double max, Color baseColor)
		{
			if (max == min)
			{
				return blendColors(baseColor, Color.GREEN, 0.5); // All values are the same
			}

			// Normalize value to [0,1]
			double normalized = (value - min) / (max - min);

			// Invert to have red be the lowest value, green the highest
			normalized = 1.0 - normalized;

			// Compute red and green components
			int red = (int) (normalized * 255);
			int green = (int) ((1 - normalized) * 255);
			int blue = 0;

			Color calculatedColor = new Color(red, green, blue);

			// Blend with base color at 50% opacity
			return blendColors(baseColor, calculatedColor, 0.5);
		}

		private Color blendColors(Color color1, Color color2, double ratio)
		{
			double inverseRatio = 1.0 - ratio;

			int red = (int) (color1.getRed() * inverseRatio + color2.getRed() * ratio);
			int green = (int) (color1.getGreen() * inverseRatio + color2.getGreen() * ratio);
			int blue = (int) (color1.getBlue() * inverseRatio + color2.getBlue() * ratio);

			return new Color(red, green, blue);
		}

		private double getColumnMinValue(int column)
		{
			double min = Double.MAX_VALUE;
			for (int row = 2; row < tableModel.getRowCount(); row++)
			{
				Object value = tableModel.getValueAt(row, column);
				if (value != null)
				{
					try
					{
						double cellValue = Double.parseDouble(value.toString());
						if (cellValue < min)
						{
							min = cellValue;
						}
					}
					catch (NumberFormatException e)
					{
						// Ignore parsing errors
					}
				}
			}
			return min;
		}

		private double getColumnMaxValue(int column)
		{
			double max = Double.MIN_VALUE;
			for (int row = 2; row < tableModel.getRowCount(); row++)
			{
				Object value = tableModel.getValueAt(row, column);
				if (value != null)
				{
					try
					{
						double cellValue = Double.parseDouble(value.toString());
						if (cellValue > max)
						{
							max = cellValue;
						}
					}
					catch (NumberFormatException e)
					{
						// Ignore parsing errors
					}
				}
			}
			return max;
		}
	}

	private void updatePresetNameInTable(String oldName, String newName)
	{
		for (int row = 0; row < tableModel.getRowCount(); row++)
		{
			Object value = tableModel.getValueAt(row, 0);
			if (value != null && value.equals(oldName))
			{
				tableModel.setValueAt(newName, row, 0);
				rowToPresetMap.put(row, newName);
				break;
			}
		}
		// Refresh the table view
		table.repaint();
	}

	private void moveEquipmentPresetDown(JTable equipmentTable, DefaultTableModel equipmentTableModel, List<String> presetNames)
	{
		int row = equipmentTable.getSelectedRow();
		if (row < equipmentTableModel.getRowCount() - 1)
		{
			equipmentTableModel.moveRow(row, row, row + 1);
			equipmentTable.setRowSelectionInterval(row + 1, row + 1);

			// Update presetNames list
			Collections.swap(presetNames, row, row + 1);

			// Reorder equipment presets
			reorderEquipmentPresets(presetNames);
		}
	}

	private void openEditConfigurationWindow()
	{
		// Create new JFrame
		JFrame configFrame = new JFrame("Edit Configuration");
		configFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		configFrame.setSize(800, 600);
		configFrame.setLocationRelativeTo(null);

		// Create the split pane
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

		// Left panel: Equipment presets
		JPanel equipmentPanel = new JPanel(new BorderLayout());
		JLabel equipmentLabel = new JLabel("Equipment Presets", SwingConstants.CENTER);
		equipmentPanel.add(equipmentLabel, BorderLayout.NORTH);

		// Equipment Table Model
		DefaultTableModel equipmentTableModel = new DefaultTableModel(new Object[]{"Preset Name", "Visible"}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return column == 1 || column == 0; // Allow editing of preset names and visibility
			}

			@Override
			public Class<?> getColumnClass(int columnIndex)
			{
				if (columnIndex == 1)
				{
					return Boolean.class; // 'Visible' column is a checkbox
				}
				return String.class;
			}
		};

		// Populate the table with preset data
		List<String> presetNames = new ArrayList<>(presets.keySet()); // Preserve the order
		for (String presetName : presetNames)
		{
			boolean isVisible = visiblePresets.contains(presetName);
			equipmentTableModel.addRow(new Object[]{presetName, isVisible});
		}

		JTable equipmentTable = new JTable(equipmentTableModel);

		// Enable cell selection and editing
		equipmentTable.setCellSelectionEnabled(true);
		equipmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Set cell renderer and editor for 'Visible' column
		equipmentTable.getColumnModel().getColumn(1).setCellRenderer(new VisibilityCellRenderer());
		equipmentTable.getColumnModel().getColumn(1).setCellEditor(new VisibilityCellEditor());

		// Scroll pane for equipment table
		JScrollPane equipmentScrollPane = new JScrollPane(equipmentTable);
		equipmentPanel.add(equipmentScrollPane, BorderLayout.CENTER);

		// Buttons panel for equipment
		JPanel equipmentButtonPanel = new JPanel();
		JButton equipmentUpButton = new JButton("Up");
		JButton equipmentDownButton = new JButton("Down");
		JButton equipmentDeleteButton = new JButton("Delete");
		equipmentButtonPanel.add(equipmentUpButton);
		equipmentButtonPanel.add(equipmentDownButton);
		equipmentButtonPanel.add(equipmentDeleteButton);
		equipmentPanel.add(equipmentButtonPanel, BorderLayout.SOUTH);

		// Right panel: NPC columns
		JPanel npcPanel = new JPanel(new BorderLayout());
		JLabel npcLabel = new JLabel("NPC Columns", SwingConstants.CENTER);
		npcPanel.add(npcLabel, BorderLayout.NORTH);

		// NPC Table Model
		DefaultTableModel npcTableModel = new DefaultTableModel(new Object[]{"NPC Name"}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false; // NPC names are not editable
			}
		};

		// Populate the NPC table
		for (int col = 1; col < tableModel.getColumnCount(); col++)
		{
			String npcName = tableModel.getColumnName(col);
			npcTableModel.addRow(new Object[]{npcName});
		}

		JTable npcTable = new JTable(npcTableModel);
		npcTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane npcScrollPane = new JScrollPane(npcTable);
		npcPanel.add(npcScrollPane, BorderLayout.CENTER);

		// Buttons panel for NPCs
		JPanel npcButtonPanel = new JPanel();
		JButton npcUpButton = new JButton("Up");
		JButton npcDownButton = new JButton("Down");
		JButton npcDeleteButton = new JButton("Delete");
		npcButtonPanel.add(npcUpButton);
		npcButtonPanel.add(npcDownButton);
		npcButtonPanel.add(npcDeleteButton);
		npcPanel.add(npcButtonPanel, BorderLayout.SOUTH);

		// Add panels to split pane
		splitPane.setLeftComponent(equipmentPanel);
		splitPane.setRightComponent(npcPanel);
		splitPane.setDividerLocation(400); // Adjust divider location as needed

		// Add split pane to frame
		configFrame.add(splitPane);

		configFrame.setVisible(true);

		// Implement action listeners for equipment buttons
		equipmentUpButton.addActionListener(e -> moveEquipmentPresetUp(equipmentTable, equipmentTableModel, presetNames));
		equipmentDownButton.addActionListener(e -> moveEquipmentPresetDown(equipmentTable, equipmentTableModel, presetNames));
		equipmentDeleteButton.addActionListener(e -> deleteEquipmentPreset(equipmentTable, equipmentTableModel, presetNames));

		// Implement action listeners for NPC buttons
		npcUpButton.addActionListener(e -> moveNPCColumnUp(npcTable, npcTableModel));
		npcDownButton.addActionListener(e -> moveNPCColumnDown(npcTable, npcTableModel));
		npcDeleteButton.addActionListener(e -> deleteNPCColumn(npcTable, npcTableModel));

		// Implement editing of equipment preset names
		equipmentTableModel.addTableModelListener(e -> {
			if (e.getType() == TableModelEvent.UPDATE)
			{
				int row = e.getFirstRow();
				int column = e.getColumn();
				if (column == 0)
				{
					String oldName = presetNames.get(row);
					String newName = (String) equipmentTableModel.getValueAt(row, column);

					if (!oldName.equals(newName))
					{
						// Update presets map
						Preset preset = presets.remove(oldName);
						if (preset != null)
						{
							preset.setName(newName);
							presets.put(newName, preset);

							// Update presetNames list
							presetNames.set(row, newName);

							// Update tableModel in main window
							updatePresetNameInTable(oldName, newName);

							// Save equipment presets
							saveEquipmentPresets();
						}
					}
				}
				else if (column == 1)
				{
					String presetName = (String) equipmentTableModel.getValueAt(row, 0);
					boolean isVisible = (boolean) equipmentTableModel.getValueAt(row, column);

					Preset preset = presets.get(presetName);
					if (preset != null)
					{
						preset.setVisible(isVisible);

						if (isVisible)
						{
							addPresetRowToTable(presetName, tableModel);
						}
						else
						{
							removePresetRowFromTable(presetName);
						}

						// Save equipment presets
						saveEquipmentPresets();
					}
				}
			}
		});

		// Double-click to edit preset names
		equipmentTable.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					int row = equipmentTable.getSelectedRow();
					int column = equipmentTable.getSelectedColumn();
					if (column == 0)
					{
						equipmentTable.editCellAt(row, column);
					}
				}
			}
		});
	}

	private void moveEquipmentPresetUp(JTable equipmentTable, DefaultTableModel equipmentTableModel, List<String> presetNames)
	{
		int row = equipmentTable.getSelectedRow();
		if (row > 0)
		{
			equipmentTableModel.moveRow(row, row, row - 1);
			equipmentTable.setRowSelectionInterval(row - 1, row - 1);

			// Update presetNames list
			Collections.swap(presetNames, row, row - 1);

			// Reorder equipment presets
			reorderEquipmentPresets(presetNames);
		}
	}

	private void editEquipmentPreset(JList<String> equipmentList)
	{
		int index = equipmentList.getSelectedIndex();
		if (index >= 0)
		{
			String presetName = equipmentList.getSelectedValue();
			openCreateFrameWithPreset(presetName);
		}
	}

	private void reorderEquipmentPresets(List<String> presetNames)
	{
		// Clear the existing presets from the main table
		for (int i = tableModel.getRowCount() - 1; i >= 2; i--)
		{
			tableModel.removeRow(i);
		}
		rowToPresetMap.clear();

		// Re-add presets in the new order if they are visible
		for (String presetName : presetNames)
		{
			if (visiblePresets.contains(presetName))
			{
				addPresetRowToTable(presetName, tableModel);
			}
		}

		// Save equipment presets (ordering may not affect serialization unless you specifically handle it)
		saveEquipmentPresets();
	}

	private void removePresetRowFromTable(String presetName)
	{
		int rowIndex = -1;
		for (Map.Entry<Integer, String> entry : rowToPresetMap.entrySet())
		{
			if (entry.getValue().equals(presetName))
			{
				rowIndex = entry.getKey();
				break;
			}
		}
		if (rowIndex >= 0)
		{
			tableModel.removeRow(rowIndex);
			rowToPresetMap.remove(rowIndex);

			// Remove diagnostic info for this row
			removeDiagnosticInfoForRow(rowIndex);

			// Adjust indices in rowToPresetMap
			Map<Integer, String> updatedMap = new HashMap<>();
			for (Map.Entry<Integer, String> entry : rowToPresetMap.entrySet())
			{
				int key = entry.getKey();
				String value = entry.getValue();
				if (key > rowIndex)
				{
					updatedMap.put(key - 1, value);
				}
				else
				{
					updatedMap.put(key, value);
				}
			}
			rowToPresetMap = updatedMap;
		}
	}

	private void removeDiagnosticInfoForRow(int rowIndex)
	{
		Iterator<String> iterator = cellDiagnosticInfo.keySet().iterator();
		while (iterator.hasNext())
		{
			String key = iterator.next();
			if (key.startsWith(rowIndex + ","))
			{
				iterator.remove();
			}
		}
	}

	private void removeDiagnosticInfoForColumn(int columnIndex)
	{
		Iterator<String> iterator = cellDiagnosticInfo.keySet().iterator();
		while (iterator.hasNext())
		{
			String key = iterator.next();
			String[] parts = key.split(",");
			int col = Integer.parseInt(parts[1]);
			if (col == columnIndex)
			{
				iterator.remove();
			}
			else if (col > columnIndex)
			{
				String newKey = parts[0] + "," + (col - 1);
				List<String> info = cellDiagnosticInfo.get(key);
				iterator.remove();
				cellDiagnosticInfo.put(newKey, info);
			}
		}
	}

	private void moveNPCColumnUp(JTable npcTable, DefaultTableModel npcTableModel)
	{
		int row = npcTable.getSelectedRow();
		if (row > 0)
		{
			npcTableModel.moveRow(row, row, row - 1);
			npcTable.setRowSelectionInterval(row - 1, row - 1);

			// Reorder NPC columns in the main table
			reorderNPCColumns(npcTableModel);
		}
	}

	private void moveNPCColumnDown(JTable npcTable, DefaultTableModel npcTableModel)
	{
		int row = npcTable.getSelectedRow();
		if (row < npcTableModel.getRowCount() - 1)
		{
			npcTableModel.moveRow(row, row, row + 1);
			npcTable.setRowSelectionInterval(row + 1, row + 1);

			// Reorder NPC columns in the main table
			reorderNPCColumns(npcTableModel);
		}
	}

	private void reorderNPCColumns(DefaultTableModel npcTableModel)
	{
		// Get the new order of NPC names
		List<String> npcNames = new ArrayList<>();
		for (int i = 0; i < npcTableModel.getRowCount(); i++)
		{
			npcNames.add((String) npcTableModel.getValueAt(i, 0));
		}

		// Remove existing NPC columns from main table
		while (tableModel.getColumnCount() > 1)
		{
			tableModel.setColumnCount(tableModel.getColumnCount() - 1);
		}
		columnToNPCDataMap.clear();

		// Re-add NPC columns in the new order
		for (String npcName : npcNames)
		{
			addNPCColumn(npcName);
		}
		applyCustomRenderers();

		// Save NPC columns
		saveNPCColumns();
	}

	private void applyDPSCellRenderer()
	{
		// Apply renderer to all columns beyond the first ("Name" column)
		for (int col = 1; col < table.getColumnCount(); col++)
		{
			table.getColumnModel().getColumn(col).setCellRenderer(new DPSCellRenderer());
		}
	}

	class PresetNameCellRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			JLabel label = (JLabel) super.getTableCellRendererComponent(
				table, value, isSelected, hasFocus, row, column);

			// Get the preset name
			String presetName = value != null ? value.toString() : "";

			// Use HTML to allow text wrapping
			String htmlText = "<html><div style='text-align:left; width:100px;'>" + presetName + "</div></html>";
			label.setText(htmlText);

			// Adjust label properties
			label.setVerticalAlignment(JLabel.CENTER);
			label.setHorizontalAlignment(JLabel.LEFT);
			label.setHorizontalTextPosition(JLabel.RIGHT);
			label.setVerticalTextPosition(JLabel.CENTER);
			label.setIconTextGap(5); // Gap between icon and text

			// Get the Preset object
			Preset preset = presets.get(presetName);
			if (preset != null)
			{
				// Get the weapon EquipmentData
				EquipmentData weapon = preset.getEquipment().get("weapon");
				if (weapon != null)
				{
					int itemId = weapon.getId();

					// Check if the image is in the cache
					ImageIcon icon = weaponIconCache.get(itemId);
					if (icon != null)
					{
						label.setIcon(icon);
					}
					else
					{
						// Set a placeholder icon or keep it empty
						label.setIcon(null);
						// Fetch the image on the client thread
						clientThread.invokeLater(() -> {
							AsyncBufferedImage itemImage = itemManager.getImage(itemId);

							// When the image is loaded, cache it and repaint the table
							itemImage.onLoaded(() -> {
								Image scaledImage = itemImage.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
								ImageIcon loadedIcon = new ImageIcon(scaledImage);
								weaponIconCache.put(itemId, loadedIcon);

								// Repaint the table on the EDT
								SwingUtilities.invokeLater(() -> {
									DPSWindow.this.table.repaint();
								});
							});
						});
					}
				}
				else
				{
					label.setIcon(null); // No weapon equipped
				}
			}

			return label;
		}
	}


	private void removeNPCColumnFromTable(String npcName)
	{
		int columnIndex = -1;
		for (int col = 1; col < tableModel.getColumnCount(); col++)
		{
			if (tableModel.getColumnName(col).equals(npcName))
			{
				columnIndex = col;
				break;
			}
		}
		if (columnIndex >= 0)
		{
			// Get the current column identifiers
			Vector<String> columnIdentifiers = new Vector<>();
			for (int col = 0; col < tableModel.getColumnCount(); col++)
			{
				if (col != columnIndex)
				{
					columnIdentifiers.add((String) tableModel.getColumnName(col));
				}
			}

			Vector dataVector = tableModel.getDataVector();
			Vector<Vector<Object>> newDataVector = new Vector<>();

			// Remove the corresponding data from each row
			for (Object rowDataObj : dataVector)
			{
				Vector rowData = (Vector) rowDataObj;
				Vector<Object> newRowData = new Vector<>();
				for (int col = 0; col < rowData.size(); col++)
				{
					if (col != columnIndex)
					{
						newRowData.add(rowData.get(col));
					}
				}
				newDataVector.add(newRowData);
			}

			// Set the new data and column identifiers
			tableModel.setDataVector(newDataVector, columnIdentifiers);

			// Remove from columnToNPCDataMap
			columnToNPCDataMap.remove(columnIndex);
			removeDiagnosticInfoForColumn(columnIndex);

			// Update column indices in columnToNPCDataMap
			Map<Integer, NPCData> newMap = new HashMap<>();
			for (Map.Entry<Integer, NPCData> entry : columnToNPCDataMap.entrySet())
			{
				int colIndex = entry.getKey();
				NPCData data = entry.getValue();
				if (colIndex > columnIndex)
				{
					newMap.put(colIndex - 1, data);
				}
				else if (colIndex < columnIndex)
				{
					newMap.put(colIndex, data);
				}
			}
			columnToNPCDataMap = newMap;
		}
	}

	private void deleteNPCColumn(JTable npcTable, DefaultTableModel npcTableModel)
	{
		int row = npcTable.getSelectedRow();
		if (row >= 0)
		{
			String npcName = (String) npcTableModel.getValueAt(row, 0);
			int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete the NPC column '" + npcName + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION)
			{
				npcTableModel.removeRow(row);
				removeNPCColumnFromTable(npcName);
				saveNPCColumns();
			}
		}
	}

	private void deleteEquipmentPreset(JTable equipmentTable, DefaultTableModel equipmentTableModel, List<String> presetNames)
	{
		int row = equipmentTable.getSelectedRow();
		if (row >= 0)
		{
			String presetName = presetNames.get(row);
			int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete the preset '" + presetName + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION)
			{
				equipmentTableModel.removeRow(row);
				presetNames.remove(row);
				presets.remove(presetName);
				visiblePresets.remove(presetName);
				removePresetRowFromTable(presetName);
				saveEquipmentPresets();
			}
		}
	}

	private void loadPresets()
	{
		try
		{
			// Ensure the dpsutility folder exists
			if (!Files.exists(DPS_UTILITY_FOLDER))
			{
				Files.createDirectories(DPS_UTILITY_FOLDER);
			}

			// Load equipment presets
			if (!Files.exists(EQUIPMENT_PRESETS_FILE))
			{
				// Create the file and write initialEquipmentData
				Files.write(EQUIPMENT_PRESETS_FILE, initialEquipmentData.getBytes(StandardCharsets.UTF_8));
			}
			String equipmentPresetsJson = new String(Files.readAllBytes(EQUIPMENT_PRESETS_FILE), StandardCharsets.UTF_8);
			java.lang.reflect.Type presetListType = new TypeToken<ArrayList<Preset>>()
			{
			}.getType();
			List<Preset> loadedPresets = gson.fromJson(equipmentPresetsJson, presetListType);

			for (Preset preset : loadedPresets)
			{
				presets.put(preset.getName(), preset);
				if (preset.isVisible())
				{
					addPresetRowToTable(preset.getName(), tableModel);
				}
			}

			// Load NPC columns
			if (!Files.exists(NPC_PRESETS_FILE))
			{
				// Create the file and write initialNPCData
				Files.write(NPC_PRESETS_FILE, initialNPCData.getBytes(StandardCharsets.UTF_8));
			}
			String npcPresetsJson = new String(Files.readAllBytes(NPC_PRESETS_FILE), StandardCharsets.UTF_8);
			java.lang.reflect.Type npcListType = new TypeToken<ArrayList<String>>()
			{
			}.getType();
			List<String> npcNames = gson.fromJson(npcPresetsJson, npcListType);
			for (String npcName : npcNames)
			{
				addNPCColumn(npcName);
			}
			applyCustomRenderers();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			log.error("Error loading presets", e);
		}
	}

	class VisibilityCellEditor extends AbstractCellEditor implements TableCellEditor
	{
		private JLabel label;
		private boolean isVisible;

		public VisibilityCellEditor()
		{
			label = new JLabel();
			label.setOpaque(true);
			label.setHorizontalAlignment(JLabel.CENTER);
			label.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					isVisible = !isVisible;
					fireEditingStopped();
				}
			});
		}

		@Override
		public Object getCellEditorValue()
		{
			return isVisible;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value,
													 boolean isSelected, int row, int column)
		{
			isVisible = (Boolean) value;
			label.setIcon(isVisible ? visibleIcon : invisibleIcon);
			return label;
		}
	}

	private ImageIcon visibleIcon;
	private ImageIcon invisibleIcon;

	class VisibilityCellRenderer extends DefaultTableCellRenderer
	{

		public VisibilityCellRenderer()
		{
			if (visibleIcon == null)
			{
				// Load the icons
				visibleIcon = new ImageIcon(ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/visible.png").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
			}
			if (invisibleIcon == null)
			{
				invisibleIcon = new ImageIcon(ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/invisible.png").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
			}
		}

		@Override
		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			boolean isVisible = (Boolean) value;
			JLabel label = new JLabel();
			label.setOpaque(true);
			label.setHorizontalAlignment(JLabel.CENTER);
			if (isVisible)
			{
				label.setIcon(visibleIcon);
			}
			else
			{
				label.setIcon(invisibleIcon);
			}
			if (isSelected)
			{
				label.setBackground(table.getSelectionBackground());
			}
			else
			{
				label.setBackground(table.getBackground());
			}
			return label;
		}
	}

	private Set<String> visiblePresets = new HashSet<>();

	private Map<Prayers, JButton> prayerButtons = new HashMap<>();

	private void showAddColumnInputBox(JButton addColumnButton, DefaultTableModel tableModel)
	{
		JLayeredPane layeredPane = getLayeredPane();

		JPanel overlayPanel = new JPanel(null);
		overlayPanel.setOpaque(false);
		overlayPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());

		final JPanel inputPanel = new JPanel(new BorderLayout());
		inputPanel.setBackground(new Color(255, 255, 255, 230));
		inputPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		final int[] preferredWidth = {200};

		overlayPanel.add(inputPanel);

		overlayPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				// Check if click was outside inputPanel
				if (!inputPanel.getBounds().contains(e.getPoint()))
				{
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
			}
		});

		// Consume mouse events on the overlay to prevent them from reaching underlying components
		overlayPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				// Do nothing to consume the event
			}
		});

		JTextField textField = new JTextField();
		inputPanel.add(textField, BorderLayout.NORTH);

		textField.setFocusTraversalKeysEnabled(false);
		textField.setHorizontalAlignment(JTextField.RIGHT);

		DefaultListModel<String> listModel = new DefaultListModel<>();
		JList<String> suggestionList = new JList<>(listModel);
		suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane suggestionScrollPane = new JScrollPane(suggestionList);
		inputPanel.add(suggestionScrollPane, BorderLayout.CENTER);

		suggestionScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		suggestionScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		suggestionList.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
														  boolean isSelected, boolean cellHasFocus)
			{
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				String text = textField.getText();
				String suggestion = value.toString();

				String lowerSuggestion = suggestion.toLowerCase();
				String lowerText = text.toLowerCase();

				int matchIndex = lowerSuggestion.indexOf(lowerText);
				if (matchIndex != -1)
				{
					String beforeMatch = suggestion.substring(0, matchIndex);
					String matchText = suggestion.substring(matchIndex, matchIndex + text.length());
					String afterMatch = suggestion.substring(matchIndex + text.length());
					label.setText("<html>" + beforeMatch + "<b>" + matchText + "</b>" + afterMatch + "</html>");
				}
				else
				{
					label.setText(suggestion);
				}

				label.setHorizontalAlignment(SwingConstants.RIGHT);

				return label;
			}
		});

		// Position the input panel below the '+' button
		Point buttonLocation = SwingUtilities.convertPoint(addColumnButton, new Point(0, addColumnButton.getHeight()), layeredPane);
		int screenWidth = layeredPane.getWidth();
		int x = buttonLocation.x - preferredWidth[0] + addColumnButton.getWidth();

// Adjust x position if it goes off-screen to the left
		if (x < 0)
		{
			x = 0;
		}
// Adjust x position if it goes off-screen to the right
		if (x + preferredWidth[0] > screenWidth)
		{
			x = screenWidth - preferredWidth[0];
		}

		inputPanel.setBounds(x, buttonLocation.y, preferredWidth[0], 130);

		layeredPane.add(overlayPanel, JLayeredPane.POPUP_LAYER);
		layeredPane.revalidate();
		layeredPane.repaint();

		textField.requestFocusInWindow();

		textField.addKeyListener(new KeyAdapter()
		{
			private SwingWorker<Void, List<String>> worker;

			@Override
			public void keyReleased(KeyEvent e)
			{
				int keyCode = e.getKeyCode();
				if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN)
				{
					return;
				}

				String text = textField.getText().toLowerCase().replaceAll("\\s+", "");
				if (text.length() < 3)
				{
					listModel.clear();
					return;
				}

				if (worker != null && !worker.isDone())
				{
					worker.cancel(true);
				}
				worker = new SwingWorker<Void, List<String>>()
				{
					@Override
					protected Void doInBackground() throws Exception
					{
						try
						{
							// Generate trigrams from input text
							Set<String> inputTrigrams = getTrigrams(text);

							// Collect candidate NPC names based on trigrams
							Map<String, Integer> candidateScores = new HashMap<>();

							for (String trigram : inputTrigrams)
							{
								if (isCancelled())
								{
									return null;
								}
								Set<String> matches = trigramIndex.get(trigram);
								if (matches != null)
								{
									for (String match : matches)
									{
										candidateScores.put(match, candidateScores.getOrDefault(match, 0) + 1);
									}
								}
							}

							// Convert candidateScores to a list and sort by score
							List<Map.Entry<String, Integer>> sortedCandidates = new ArrayList<>(candidateScores.entrySet());
							sortedCandidates.sort((a, b) -> b.getValue().compareTo(a.getValue()));

							// Collect the top suggestions
							List<String> suggestions = new ArrayList<>();
							for (Map.Entry<String, Integer> entry : sortedCandidates)
							{
								suggestions.add(entry.getKey());
								if (suggestions.size() >= 10)
								{
									break;
								}
							}

							publish(suggestions);
						}
						catch (Exception ex)
						{
							ex.printStackTrace();
							log.error("Error during trigram search", ex);
						}
						return null;
					}

					@Override
					protected void process(List<List<String>> chunks)
					{
						listModel.clear();
						List<String> suggestions = chunks.get(chunks.size() - 1);
						int maxWidth = 0;
						FontMetrics metrics = suggestionList.getFontMetrics(suggestionList.getFont());

						// Height calculations
						int rowHeight = suggestionList.getFixedCellHeight();
						if (rowHeight == -1)
						{
							rowHeight = suggestionList.getFontMetrics(suggestionList.getFont()).getHeight();
						}
						int totalHeight = 0;

						for (String suggestion : suggestions)
						{
							listModel.addElement(suggestion);
							// Calculate the width of the suggestion
							int width = SwingUtilities.computeStringWidth(metrics, suggestion);
							if (width > maxWidth)
							{
								maxWidth = width;
							}
						}

						// Calculate total height required
						int visibleRowCount = suggestions.size();
						totalHeight = visibleRowCount * rowHeight + 5; // Add a small margin

						// Set maximum height if necessary
						int maxPopupHeight = 300;
						if (totalHeight > maxPopupHeight)
						{
							totalHeight = maxPopupHeight;
							suggestionList.setVisibleRowCount(maxPopupHeight / rowHeight);
						}
						else
						{
							suggestionList.setVisibleRowCount(visibleRowCount);
						}

						// Adjust width settings (same as before)
						int scrollbarWidth = suggestionScrollPane.getVerticalScrollBar().isVisible() ? suggestionScrollPane.getVerticalScrollBar().getWidth() : 17;
						preferredWidth[0] = maxWidth + scrollbarWidth + 20; // 20 pixels for padding
						if (preferredWidth[0] < 200)
						{
							preferredWidth[0] = 200; // Minimum width
						}
						if (preferredWidth[0] > screenWidth)
						{
							preferredWidth[0] = screenWidth - 20; // Maximum width
						}

						// Update the sizes
						inputPanel.setPreferredSize(new Dimension(preferredWidth[0], totalHeight + textField.getHeight()));
						suggestionScrollPane.setPreferredSize(new Dimension(preferredWidth[0], totalHeight));
						inputPanel.revalidate();
						inputPanel.repaint();

						// Reposition the inputPanel if necessary
						int x = buttonLocation.x - preferredWidth[0] + addColumnButton.getWidth();
						if (x < 0)
						{
							x = 0;
						}
						if (x + preferredWidth[0] > screenWidth)
						{
							x = screenWidth - preferredWidth[0];
						}
						inputPanel.setBounds(x, buttonLocation.y, preferredWidth[0], totalHeight + textField.getHeight());

						if (listModel.getSize() > 0)
						{
							suggestionList.setSelectedIndex(0);
						}
					}
				};
				worker.execute();
			}

			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					String selectedOption;
					if (!suggestionList.isSelectionEmpty())
					{
						selectedOption = suggestionList.getSelectedValue();
					}
					else
					{
						selectedOption = textField.getText();
					}

					addNPCColumn(selectedOption);

					// Close the overlay panel
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
				else if (e.getKeyCode() == KeyEvent.VK_TAB)
				{
					if (!suggestionList.isSelectionEmpty())
					{
						textField.setText(suggestionList.getSelectedValue());
						listModel.clear();
					}
					e.consume();
				}
				else if (e.getKeyCode() == KeyEvent.VK_DOWN)
				{
					int index = suggestionList.getSelectedIndex();
					if (index < listModel.getSize() - 1)
					{
						suggestionList.setSelectedIndex(index + 1);
					}
					e.consume();
				}
				else if (e.getKeyCode() == KeyEvent.VK_UP)
				{
					int index = suggestionList.getSelectedIndex();
					if (index > 0)
					{
						suggestionList.setSelectedIndex(index - 1);
					}
					e.consume();
				}
			}
		});
	}

	private void saveNPCColumns()
	{
		try
		{
			// Ensure the dpsutility folder exists
			if (!Files.exists(DPS_UTILITY_FOLDER))
			{
				Files.createDirectories(DPS_UTILITY_FOLDER);
			}

			// Get the list of NPC names from the table model
			List<String> npcNames = new ArrayList<>();
			for (int col = 1; col < tableModel.getColumnCount(); col++)
			{
				String npcName = tableModel.getColumnName(col);
				npcNames.add(npcName);
			}

			// Serialize to JSON
			String npcPresetsJson = gson.toJson(npcNames);

			// Write to file
			Files.write(NPC_PRESETS_FILE, npcPresetsJson.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			e.printStackTrace();
			log.error("Error saving NPC columns", e);
		}
	}

	private Map<String, Integer> baseLevels = new HashMap<>(); // "Attack", "Strength", etc.
	private Map<String, String> selectedPotions = new HashMap<>(); // "Attack", "Strength", etc.

	// Reference to text fields and labels to update virtual levels dynamically
	private Map<String, JTextField> baseLevelFields = new HashMap<>();
	private Map<String, JComboBox<String>> potionComboBoxes = new HashMap<>();
	private Map<String, JLabel> virtualLevelLabels = new HashMap<>();

	{
		baseLevels.put("Attack", 99);
		baseLevels.put("Strength", 99);
		baseLevels.put("Ranged", 99);
		baseLevels.put("Magic", 99);

		// Default potion selections
		selectedPotions.put("Attack", "None");
		selectedPotions.put("Strength", "None");
		selectedPotions.put("Ranged", "None");
		selectedPotions.put("Magic", "None");
	}

	private Map<String, Integer> calculateVirtualLevelsForPreset(Map<String, Integer> baseLevels, Map<String, String> selectedPotions, Map<Prayers, Boolean> prayers)
	{
		Map<String, Integer> virtualLevels = new HashMap<>();

		// Get total prayer boosts
		Map<String, Integer> prayerBoosts = getPrayerBoostForPreset(prayers);

		// For Melee Attack
		int baseAttackLevel = baseLevels.getOrDefault("Attack", 1);
		int attackPotionBoost = getPotionBoost("Attack", baseAttackLevel, selectedPotions.getOrDefault("Attack", "None"));
		int meleeAttackPrayerBoost = (int) Math.floor((baseAttackLevel + attackPotionBoost) * prayerBoosts.get("meleeAttack") / 100.0);
		int virtualAttackLevel = baseAttackLevel + attackPotionBoost + meleeAttackPrayerBoost;
		virtualLevels.put("Attack", virtualAttackLevel);

		// For Melee Strength
		int baseStrengthLevel = baseLevels.getOrDefault("Strength", 1);
		int strengthPotionBoost = getPotionBoost("Strength", baseStrengthLevel, selectedPotions.getOrDefault("Strength", "None"));
		int meleeStrengthPrayerBoost = (int) Math.floor((baseStrengthLevel + strengthPotionBoost) * prayerBoosts.get("meleeStrength") / 100.0);
		int virtualStrengthLevel = baseStrengthLevel + strengthPotionBoost + meleeStrengthPrayerBoost;
		virtualLevels.put("Strength", virtualStrengthLevel);

		// For Ranged
		int baseRangedLevel = baseLevels.getOrDefault("Ranged", 1);
		int rangedPotionBoost = getPotionBoost("Ranged", baseRangedLevel, selectedPotions.getOrDefault("Ranged", "None"));
		int rangedAttackPrayerBoost = (int) Math.floor((baseRangedLevel + rangedPotionBoost) * prayerBoosts.get("rangedAttack") / 100.0);
		int virtualRangedAttackLevel = baseRangedLevel + rangedPotionBoost + rangedAttackPrayerBoost;
		virtualLevels.put("RangedAttack", virtualRangedAttackLevel);


		int rangedStrengthPrayerBoost = (int) Math.floor((baseRangedLevel + rangedPotionBoost) * prayerBoosts.get("rangedStrength") / 100.0);
		int virtualRangedStrengthLevel = baseRangedLevel + rangedPotionBoost + rangedStrengthPrayerBoost;
		virtualLevels.put("RangedStrength", virtualRangedStrengthLevel);

		// For Magic (if necessary)
		// ...

		// Note: Ranged Strength bonuses from prayer are applied separately in damage calculations, not to levels.

		return virtualLevels;
	}

	private Map<String, Integer> getPrayerBoostForPreset(Map<Prayers, Boolean> prayers)
	{
		Map<String, Integer> boosts = new HashMap<>();
		double meleeAttackBoostPercentage = 0.0;
		double meleeStrengthBoostPercentage = 0.0;
		double rangedAttackBoostPercentage = 0.0;
		double rangedStrengthBoostPercentage = 0.0;
		double magicAttackBoostPercentage = 0.0;
		double magicDamageBoostPercentage = 0.0;
		// Add more if needed

		for (Map.Entry<Prayers, Boolean> entry : prayers.entrySet())
		{
			if (entry.getValue())
			{
				Prayers prayer = entry.getKey();
				meleeAttackBoostPercentage += prayer.att;
				meleeStrengthBoostPercentage += prayer.str;
				rangedAttackBoostPercentage += prayer.ranA;
				rangedStrengthBoostPercentage += prayer.ranD;
				magicAttackBoostPercentage += prayer.mag;
				magicDamageBoostPercentage += prayer.magD;
				// Continue for other bonuses
			}
		}

		boosts.put("meleeAttack", (int) meleeAttackBoostPercentage);
		boosts.put("meleeStrength", (int) meleeStrengthBoostPercentage);
		boosts.put("rangedAttack", (int) rangedAttackBoostPercentage);
		boosts.put("rangedStrength", (int) rangedStrengthBoostPercentage);
		boosts.put("magicAttack", (int) magicAttackBoostPercentage);
		boosts.put("magicDamage", (int) magicDamageBoostPercentage);

		return boosts;
	}


	private int calculateDefenseRoll(NPCData npcData, int defenseValue, String selectedStyle, String attackType)
	{
		int npcDefenseBonus = 0;

		if (isMeleeStyle(selectedStyle))
		{
			switch (attackType)
			{
				case "Stab":
					npcDefenseBonus = npcData.getDefensive().getStab();
					break;
				case "Slash":
					npcDefenseBonus = npcData.getDefensive().getSlash();
					break;
				case "Crush":
					npcDefenseBonus = npcData.getDefensive().getCrush();
					break;
			}
		}
		else if (isRangedStyle(selectedStyle))
		{
			npcDefenseBonus = npcData.getDefensive().getStandard(); //TODO :angry:
		}
		else if (isMagicStyle(selectedStyle))
		{
			npcDefenseBonus = npcData.getDefensive().getMagic();
		}

		return (defenseValue + 9) * (npcDefenseBonus + 64);
	}

	private int calculateMagicMaxHit(Preset preset)
	{
		// Placeholder: Return a default max hit, or calculate based on magic level and equipment
		// You might need to add spell selection to the UI and Preset class for accurate calculations

		// Example calculation assuming standard spellbook
		int baseMaxHit = 20; // Adjust this value based on the spell

		// Calculate magic damage bonus from equipment
		int magicDamageBonus = 0;
		for (EquipmentData data : preset.getEquipment().values())
		{
			magicDamageBonus += data.getBonuses().getMagic_str(); // Assuming magic_str is the magic damage bonus in %
		}

		// Apply magic damage bonus
		int maxHit = (int) Math.floor(baseMaxHit * (1 + magicDamageBonus / 100.0));

		return maxHit;
	}

	private boolean isMeleeStyle(String selectedStyle)
	{
		return selectedStyle.equals("Accurate") ||
			selectedStyle.equals("Aggressive") ||
			selectedStyle.equals("Defensive") ||
			selectedStyle.equals("Controlled");
	}

	private boolean isRangedStyle(String selectedStyle)
	{
		return selectedStyle.equals("Accurate (Ranged)") ||
			selectedStyle.equals("Rapid") ||
			selectedStyle.equals("Longrange");
	}

	private boolean isMagicStyle(String selectedStyle)
	{
		return selectedStyle.equals("Standard") ||
			selectedStyle.equals("Defensive (Magic)");
	}

	class CalculationResult
	{
		String value;
		List<String> diagnosticInfo;

		public CalculationResult(String value, List<String> diagnosticInfo)
		{
			this.value = value;
			this.diagnosticInfo = diagnosticInfo;
		}
	}

	private CalculationResult computeCellValue(Preset preset, NPCData npcData, int defenseValue)
	{
		int effectiveAttackLevel = 0;
		int effectiveStrengthLevel = 0;
		int maxHit = 0;
		int attackRoll = 0;
		String selectedStyle = preset.getSelectedStyle();
		Map<Prayers, Boolean> prayers = preset.getPrayers();

		Map<String, Integer> baseLevels = preset.getBaseLevels();
		Map<String, String> selectedPotions = preset.getSelectedPotions();

		Map<String, Integer> virtualLevels = calculateVirtualLevelsForPreset(baseLevels, selectedPotions, prayers);
		Map<String, Integer> prayerBoosts = getPrayerBoostForPreset(prayers);

		double attackSpeed = getAttackSpeed(preset);

		String attackType = "";
		// Use the virtual levels in calculations
		if (isMeleeStyle(selectedStyle))
		{
			attackType = preset.getSelectedAttackType();
			effectiveAttackLevel = virtualLevels.get("Attack");
			effectiveStrengthLevel = virtualLevels.get("Strength");

			// Adjust for attack style
			if (selectedStyle.equals("Accurate"))
			{
				effectiveAttackLevel += 3;
			}
			else if (selectedStyle.equals("Aggressive"))
			{
				effectiveStrengthLevel += 3;
			}
			else if (selectedStyle.equals("Controlled"))
			{
				effectiveAttackLevel += 1;
				effectiveStrengthLevel += 1;
			}
			else if (selectedStyle.equals("Defensive"))
			{
				// Defensive style, no attack/strength bonus
			}

			effectiveStrengthLevel += 8;
			effectiveAttackLevel += 8;

			if(preset.isMeleeVoid())
			{
				effectiveStrengthLevel = (int) (effectiveStrengthLevel * 1.1);
				effectiveAttackLevel = (int) (effectiveAttackLevel * 1.1);
			}

			// Equipment bonuses
			int attackBonus = 0;
			int strengthBonus = 0;
			for (EquipmentData data : preset.getEquipment().values())
			{
				if (preset.getSelectedAttackType().equals("Stab"))
				{
					attackBonus += data.getOffensive().getStab();
				}
				else if (preset.getSelectedAttackType().equals("Slash"))
				{
					attackBonus += data.getOffensive().getSlash();
				}
				else if (preset.getSelectedAttackType().equals("Crush"))
				{
					attackBonus += data.getOffensive().getCrush();
				}
				strengthBonus += data.getBonuses().getStr();
			}

			maxHit = (int)((effectiveStrengthLevel * (strengthBonus + 64)+320)/640.0);
			attackRoll = effectiveAttackLevel * (attackBonus + 64);
			if(preset.isSalveE())
			{
				maxHit = (int) (maxHit*1.20);
				attackRoll = (int) (attackRoll*1.20);
			}
			else if(preset.isSalve() || preset.isSlayer())
			{
				maxHit = (int) (maxHit * (7.0/6.0));
				attackRoll = (int) (attackRoll*(7.0/6.0));
			}
		}
		else if (isRangedStyle(selectedStyle))
		{
			effectiveAttackLevel = virtualLevels.get("RangedAttack");
			effectiveStrengthLevel = virtualLevels.get("RangedStrength");

			// Adjust for attack style
			if (selectedStyle.equals("Accurate (Ranged)"))
			{
				effectiveAttackLevel += 3;
				effectiveStrengthLevel += 3;
			}
			else if(selectedStyle.equals("Rapid"))
			{
				attackSpeed--;
			}

			effectiveStrengthLevel += 8;
			effectiveAttackLevel += 8;

			if(preset.isRangeEVoid())
			{
				effectiveStrengthLevel = (int)(effectiveStrengthLevel*1.125);
				effectiveAttackLevel = (int) (effectiveAttackLevel*1.1);
			}
			else if(preset.isRangeVoid())
			{
				effectiveStrengthLevel = (int)(effectiveStrengthLevel*1.1);
				effectiveAttackLevel = (int) (effectiveAttackLevel*1.1);
			}

			// Equipment bonuses
			int attackBonus = 0;
			int strengthBonus = 0; // Ranged strength bonus

			for (EquipmentData data : preset.getEquipment().values())
			{
				attackBonus += data.getOffensive().getRanged();
				strengthBonus += data.getBonuses().getRanged_str();
				if(data.getName().toLowerCase().contains("quiver"))
				{
					attackBonus += 10;
					strengthBonus += 1;
				}
			}

			attackRoll = effectiveAttackLevel * (attackBonus + 64);

			maxHit = (int) (.5 + effectiveStrengthLevel * (strengthBonus+64)/640.0);

			if(preset.isSalveE())
			{
				maxHit = (int) (maxHit*1.2);
			}
			else if(preset.isSalve())
			{
				maxHit = (int) (maxHit * (7.0/6.0));
			}
			else if(preset.isSlayer())
			{
				maxHit = (int) (maxHit * 1.15); //todo craws/webweaver
			}

			if(preset.getEquipment().get("weapon").getName().contains("Twisted bow"))
			{
				int magicValue = Math.max(npcData.getSkills().getMagic(), npcData.getOffensive().getMagic());
				//outside of chambers:
				magicValue = Math.min(250, magicValue);
				double tbowAccuracyMultiplier = 140 + (((((30*magicValue)/10.0)-10) / 100.0) - (Math.pow((3*magicValue/10.0)-100, 2)/100.0));
				tbowAccuracyMultiplier = Math.min(tbowAccuracyMultiplier, 140);
				double tbowDamageMultiplier = 250 + (((((30*magicValue)/10.0)-14) / 100.0) - (Math.pow((3*magicValue/10.0)-140, 2)/100.0));
				tbowDamageMultiplier = Math.min(tbowDamageMultiplier, 250);

				tbowAccuracyMultiplier /= 100;
				tbowDamageMultiplier /= 100;

				maxHit = (int) (maxHit*tbowDamageMultiplier);
				attackRoll = (int) (attackRoll*tbowAccuracyMultiplier);
			}

		}
		else if (isMagicStyle(selectedStyle))
		{
			effectiveAttackLevel = magicLevel;

			// Adjust for attack style
			if (selectedStyle.equals("Standard"))
			{
				// No bonus
			}
			else if (selectedStyle.equals("Defensive (Magic)"))
			{
				effectiveAttackLevel += 3;
			}

			// Equipment bonuses
			int attackBonus = 0;
			for (EquipmentData data : preset.getEquipment().values())
			{
				attackBonus += data.getOffensive().getMagic();
			}

			attackRoll = effectiveAttackLevel * (attackBonus + 64);

			maxHit = calculateMagicMaxHit(preset); // Depends on spell selected

			// Continue with hit chance calculations...

		}

		int defenseRoll = calculateDefenseRoll(npcData, defenseValue, selectedStyle, attackType);

		double hitChance;
		if (attackRoll > defenseRoll)
		{
			hitChance = 1 - ((defenseRoll + 2.0) / (2.0 * attackRoll + 1));
		}
		else
		{
			hitChance = attackRoll / (2.0 * defenseRoll + 1);
		}

		double dpa;

		if (preset.getEquipment().get("weapon").getName().toLowerCase().contains("vitur"))
		{
			double[] scytheHitMultipliers = {1.0, 0.5, 0.25};
			dpa = 0.0;

			for (double multiplier : scytheHitMultipliers)
			{
				double adjustedMaxHit = Math.floor(maxHit * multiplier);
				dpa += calculateExpectedDamage(adjustedMaxHit, hitChance);
			}
		}
		else
		{
			dpa = calculateExpectedDamage(maxHit, hitChance);
		}

		double dps = dpa / (attackSpeed*.6);

		List<String> diagnosticInfo = List.of("Max Hit: " + maxHit, "Attack Roll: " + attackRoll, "Hitchance: " + hitChance, "Defense Roll: " + defenseRoll, "DPA: " + dpa, "Weapon Speed: " + attackSpeed, "DPS: " + dps);

		String value = String.format("%.2f", dps);
		return new CalculationResult(value, diagnosticInfo);
	}

	private String getEquipmentTooltipText(EquipmentData equipmentData)
	{
		StringBuilder tooltip = new StringBuilder("<html>");
		tooltip.append("<b>").append(equipmentData.getName()).append("</b><br/>");

		EquipmentStats offensive = equipmentData.getOffensive();
		EquipmentStats defensive = equipmentData.getDefensive();
		EquipmentBonuses bonuses = equipmentData.getBonuses();

		boolean hasOffensive = false;
		boolean hasDefensive = false;
		boolean hasBonuses = false;

		if (offensive != null)
		{
			StringBuilder offensiveStats = new StringBuilder();
			if (offensive.getStab() != 0) {
				offensiveStats.append("&nbsp;&nbsp;Stab Attack: ").append(offensive.getStab()).append("<br/>");
			}
			if (offensive.getSlash() != 0) {
				offensiveStats.append("&nbsp;&nbsp;Slash Attack: ").append(offensive.getSlash()).append("<br/>");
			}
			if (offensive.getCrush() != 0) {
				offensiveStats.append("&nbsp;&nbsp;Crush Attack: ").append(offensive.getCrush()).append("<br/>");
			}
			if (offensive.getMagic() != 0) {
				offensiveStats.append("&nbsp;&nbsp;Magic Attack: ").append(offensive.getMagic()).append("<br/>");
			}
			if (offensive.getRanged() != 0) {
				offensiveStats.append("&nbsp;&nbsp;Ranged Attack: ").append(offensive.getRanged()).append("<br/>");
			}
			if (offensiveStats.length() > 0)
			{
				tooltip.append("Offensive Bonuses:<br/>").append(offensiveStats.toString());
				hasOffensive = true;
			}
		}

		if (defensive != null)
		{
			StringBuilder defensiveStats = new StringBuilder();
			if (defensive.getStab() != 0) {
				defensiveStats.append("&nbsp;&nbsp;Stab Defence: ").append(defensive.getStab()).append("<br/>");
			}
			if (defensive.getSlash() != 0) {
				defensiveStats.append("&nbsp;&nbsp;Slash Defence: ").append(defensive.getSlash()).append("<br/>");
			}
			if (defensive.getCrush() != 0) {
				defensiveStats.append("&nbsp;&nbsp;Crush Defence: ").append(defensive.getCrush()).append("<br/>");
			}
			if (defensive.getMagic() != 0) {
				defensiveStats.append("&nbsp;&nbsp;Magic Defence: ").append(defensive.getMagic()).append("<br/>");
			}
			if (defensive.getRanged() != 0) {
				defensiveStats.append("&nbsp;&nbsp;Ranged Defence: ").append(defensive.getRanged()).append("<br/>");
			}
			if (defensiveStats.length() > 0)
			{
				tooltip.append("Defensive Bonuses:<br/>").append(defensiveStats.toString());
				hasDefensive = true;
			}
		}

		if (bonuses != null)
		{
			StringBuilder otherBonuses = new StringBuilder();
			if (bonuses.getStr() != 0) {
				otherBonuses.append("&nbsp;&nbsp;Melee Strength: ").append(bonuses.getStr()).append("<br/>");
			}
			if (bonuses.getRanged_str() != 0) {
				otherBonuses.append("&nbsp;&nbsp;Ranged Strength: ").append(bonuses.getRanged_str()).append("<br/>");
			}
			if (bonuses.getMagic_str() != 0) {
				otherBonuses.append("&nbsp;&nbsp;Magic Damage: ").append(bonuses.getMagic_str()).append("<br/>");
			}
			if (bonuses.getPrayer() != 0) {
				otherBonuses.append("&nbsp;&nbsp;Prayer Bonus: ").append(bonuses.getPrayer()).append("<br/>");
			}
			if (otherBonuses.length() > 0)
			{
				tooltip.append("Other Bonuses:<br/>").append(otherBonuses.toString());
				hasBonuses = true;
			}
		}

		tooltip.append("</html>");
		return tooltip.toString();
	}

	private double calculateExpectedDamage(double maxHit, double hitChance)
	{
		if (maxHit <= 0.0)
		{
			return 0.0;
		}
		return hitChance * ((maxHit / 2.0) + (1.0 / (maxHit + 1)));
	}

	private Map<String, JToggleButton> attackTypeButtons = new HashMap<>();
	private String selectedAttackType = null;

	private int getAttackSpeed(Preset preset)
	{
		try
		{
			return preset.getEquipment().get("weapon").getSpeed();
		}
		catch (NullPointerException e)
		{
			return 5; //barehanded? todo idk this sucks
		}
	}

	private JPanel createSkillPanel()
	{
		skillPanel = new JPanel();
		skillPanel.setLayout(new BorderLayout());

		// Set border with padding
		skillPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));

		JLabel skillLabel = new JLabel("Skills", SwingConstants.CENTER);
		skillLabel.setFont(new Font("Arial", Font.BOLD, 16));
		skillPanel.add(skillLabel, BorderLayout.NORTH);

		// Create a table-like structure using GridBagLayout
		JPanel skillsGridPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.weightx = 1.0;

		// Define headers
		String[] headers = {"Skill", "Level", "Potion", "Virtual Level"};
		for (int i = 0; i < headers.length; i++)
		{
			gbc.gridx = i;
			gbc.gridy = 0;
			JLabel headerLabel = new JLabel(headers[i]);
			headerLabel.setFont(new Font("Arial", Font.BOLD, 12));
			skillsGridPanel.add(headerLabel, gbc);
		}

		// For each skill, create the row
		String[] skills = {"Attack", "Strength", "Ranged", "Magic"};
		int row = 1; // Start from row 1 since row 0 is the header

		for (String skill : skills)
		{
			gbc.gridy = row;

			// Skill name label
			gbc.gridx = 0;
			JLabel skillNameLabel = new JLabel(skill);
			skillsGridPanel.add(skillNameLabel, gbc);

			// Base level field
			gbc.gridx = 1;
			JTextField baseLevelField = new JTextField(3);
			baseLevelField.setText(String.valueOf(baseLevels.get(skill)));
			skillsGridPanel.add(baseLevelField, gbc);

			// Potion dropdown
			gbc.gridx = 2;
			JComboBox<String> potionComboBox = new JComboBox<>();
			for (String potion : getPotionsForSkill(skill))
			{
				potionComboBox.addItem(potion);
			}
			potionComboBox.setSelectedItem(selectedPotions.get(skill));
			skillsGridPanel.add(potionComboBox, gbc);

			// Virtual level label
			gbc.gridx = 3;
			JLabel virtualLevelLabel = new JLabel(String.valueOf(calculateVirtualLevel(skill)));
			skillsGridPanel.add(virtualLevelLabel, gbc);

			// Store references for later updates
			baseLevelFields.put(skill, baseLevelField);
			potionComboBoxes.put(skill, potionComboBox);
			virtualLevelLabels.put(skill, virtualLevelLabel);

			// Add listeners to update virtual levels when inputs change
			baseLevelField.getDocument().addDocumentListener(new DocumentListener()
			{
				public void changedUpdate(DocumentEvent e)
				{
					updateVirtualLevel(skill);
				}

				public void removeUpdate(DocumentEvent e)
				{
					updateVirtualLevel(skill);
				}

				public void insertUpdate(DocumentEvent e)
				{
					updateVirtualLevel(skill);
				}
			});

			potionComboBox.addActionListener(e -> updateVirtualLevel(skill));

			row++;
		}

		skillPanel.add(skillsGridPanel, BorderLayout.CENTER);

		return skillPanel;
	}

	private int getPotionBoost(String skill, int baseLevel, String potion)
	{
		switch (potion)
		{
			case "None":
				return 0;
			case "Attack":
				return (int) Math.floor(baseLevel * 0.10 + 3);
			case "Super Attack":
				return (int) Math.floor(baseLevel * 0.15 + 5);
			case "Zamorak Brew":
				return (int) Math.floor(baseLevel * 0.20 + 2);
			case "Strength":
				return (int) Math.floor(baseLevel * 0.10 + 3);
			case "Super Strength":
				return (int) Math.floor(baseLevel * 0.15 + 5);
			case "Ranging":
				return (int) Math.floor(baseLevel * 0.10 + 4);
			case "Super Ranging":
				return (int) Math.floor(baseLevel * 0.15 + 5);
			case "Magic":
				return 4;
			case "Super Magic":
				return (int) Math.floor(baseLevel * 0.15 + 5);
			case "Overload (Raids)":
				return (int) Math.floor(baseLevel * 0.16 + 6);
			case "Smelling Salts":
				return (int) Math.floor(baseLevel * 0.16 + 11);
			case "Ancient Brew":
				return (int) Math.floor(baseLevel * 0.05 + 2);
			case "Magister's Brew":
				return (int) Math.floor(baseLevel * 0.08 + 3);
			case "Imbued Heart":
				return (int) Math.floor(baseLevel * 0.10 + 1);
			case "Saturated Heart":
				return (int) Math.floor(baseLevel * 0.10 + 4);
			default:
				return 0;
		}
	}

	private int getPrayerBoost(String skill, int baseLevel)
	{
		double boostPercentage = 0.0;
		for (Map.Entry<Prayers, Boolean> entry : selectedPrayers.entrySet())
		{
			if (entry.getValue())
			{
				Prayers prayer = entry.getKey();
				switch (skill)
				{
					case "Attack":
						boostPercentage += prayer.att;
						break;
					case "Strength":
						boostPercentage += prayer.str;
						break;
					case "Ranged":
						boostPercentage += prayer.ranA;
						break;
					case "Magic":
						boostPercentage += prayer.mag;
						break;
				}
			}
		}
		return (int) Math.floor(baseLevel * boostPercentage / 100.0);
	}

	private void updateVirtualLevel(String skill)
	{
		// Update base level
		try
		{
			int level = Integer.parseInt(baseLevelFields.get(skill).getText());
			baseLevels.put(skill, Math.min(Math.max(level, 1), 99));
		}
		catch (NumberFormatException ex)
		{
			baseLevels.put(skill, 1); // Default to 1 if invalid input
		}

		// Update selected potion
		String potion = (String) potionComboBoxes.get(skill).getSelectedItem();
		selectedPotions.put(skill, potion);

		// Calculate virtual level
		int virtualLevel = calculateVirtualLevel(skill);

		// Update the virtual level label
		JLabel virtualLevelLabel = virtualLevelLabels.get(skill);
		virtualLevelLabel.setText(String.valueOf(virtualLevel));
	}

	private int calculateVirtualLevel(String skill)
	{
		int baseLevel = baseLevels.getOrDefault(skill, 1);
		String potion = selectedPotions.getOrDefault(skill, "None");

		int potionBoost = getPotionBoost(skill, baseLevel, potion);
		int prayerBoost = getPrayerBoost(skill, baseLevel + potionBoost);

		int virtualLevel = baseLevel + potionBoost;
		virtualLevel += prayerBoost;

		return virtualLevel;
	}

	private List<String> getPotionsForSkill(String skill)
	{
		switch (skill)
		{
			case "Attack":
				return Arrays.asList("None", "Attack", "Super Attack", "Zamorak Brew", "Overload (Raids)", "Smelling Salts");
			case "Strength":
				return Arrays.asList("None", "Strength", "Super Strength", "Overload (Raids)", "Smelling Salts");
			case "Ranged":
				return Arrays.asList("None", "Ranging", "Super Ranging", "Overload (Raids)", "Smelling Salts");
			case "Magic":
				return Arrays.asList("None", "Magic", "Super Magic", "Ancient Brew", "Magister's Brew", "Imbued Heart", "Saturated Heart", "Overload (Raids)", "Smelling Salts");
			default:
				return Collections.singletonList("None");
		}
	}

	private void openCreateFrameWithPreset(String presetName)
	{
		// Retrieve the Preset object from the presets map
		Preset preset = presets.get(presetName);
		if (preset == null)
		{
			JOptionPane.showMessageDialog(this, "Preset not found: " + presetName);
			return;
		}

		// Extract equipment, prayers, and selected style from the preset
		Map<String, EquipmentData> presetEquipment = preset.getEquipment();
		Map<Prayers, Boolean> presetPrayers = preset.getPrayers();
		selectedStyle = preset.getSelectedStyle();

		// Update the selectedEquipment and selectedPrayers with the preset data
		selectedEquipment = new HashMap<>(presetEquipment);
		selectedPrayers = new HashMap<>(presetPrayers);

		// Initialize createFrame
		createFrame = new JFrame("Edit Preset - " + presetName);
		createFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Main panel with vertical layout
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		// Panels for equipment, prayer, and style
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));

		equipmentPanel = createEquipmentPanel(presetEquipment);
		prayerPanel = createPrayerPanel();
		stylePanel = createStylePanel();
		skillPanel = createSkillPanel();

// Combine style and skill panels vertically
		JPanel styleAndSkillPanel = new JPanel();
		styleAndSkillPanel.setLayout(new BoxLayout(styleAndSkillPanel, BoxLayout.Y_AXIS));
		styleAndSkillPanel.add(stylePanel);
		styleAndSkillPanel.add(Box.createVerticalStrut(10));
		styleAndSkillPanel.add(skillPanel);

// Add panels to contentPanel
		contentPanel.add(equipmentPanel);
		contentPanel.add(Box.createHorizontalStrut(10));
		contentPanel.add(prayerPanel);
		contentPanel.add(Box.createHorizontalStrut(10));
		contentPanel.add(styleAndSkillPanel);

		// Load the preset's base levels and selected potions
		baseLevels = new HashMap<>(preset.getBaseLevels());
		selectedPotions = new HashMap<>(preset.getSelectedPotions());

// Update UI components
		updateEquipmentPanel();
		updatePrayerPanel();
		updateStylePanel();
		updateSkillPanel(); // Ensure this is called to update the skill panel with preset data

		// Add contentPanel to mainPanel
		mainPanel.add(contentPanel, BorderLayout.CENTER);

		// Create the save preset panel at the bottom
		JPanel savePresetPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		// Preset name field (non-editable)
		JTextField presetNameField = new JTextField(presetName, 20);
		presetNameField.setEditable(false);
		JButton savePresetButton = new JButton("Save Changes");

		savePresetPanel.add(new JLabel("Preset Name:"));
		savePresetPanel.add(presetNameField);
		savePresetPanel.add(savePresetButton);

		// Add savePresetPanel to mainPanel
		mainPanel.add(savePresetPanel, BorderLayout.SOUTH);

		// Add mainPanel to frame
		createFrame.add(mainPanel);

		setPreferredSize(new Dimension(1100, 775));
		createFrame.pack();
		createFrame.setLocationRelativeTo(null);
		createFrame.setVisible(true);

		// Add ActionListener to the savePresetButton
		savePresetButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				// Save the changes to the preset
				Map<String, EquipmentData> updatedEquipment = new HashMap<>(selectedEquipment);
				Map<Prayers, Boolean> updatedPrayers = new HashMap<>(selectedPrayers);
				String updatedSelectedStyle = selectedStyle;

				// Update the preset object with new data
				preset.setEquipment(updatedEquipment);
				preset.setPrayers(updatedPrayers);
				preset.setSelectedStyle(updatedSelectedStyle);

				JOptionPane.showMessageDialog(createFrame, "Preset '" + presetName + "' updated successfully.");
				createFrame.dispose();
			}
		});

		// After the UI is set up, call updatePrayerSummary() to refresh the summary panel
		updatePrayerSummary();
	}

	private JPanel createPresetPanel(String presetName, DefaultTableModel tableModel, JLayeredPane layeredPane, JPanel inputPanel)
	{
		JPanel presetPanel = new JPanel(new BorderLayout());

		presetPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35)); // Set max height

		// Preset name label
		JLabel nameLabel = new JLabel(presetName);
		nameLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Buttons panel
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));

		// "View" button
		JButton viewButton = new JButton("View");

		// "Select" button
		JButton selectButton = new JButton("Select");

		// Add buttons to buttonsPanel
		buttonsPanel.add(viewButton);
		buttonsPanel.add(selectButton);

		// Add components to presetPanel
		presetPanel.add(nameLabel, BorderLayout.CENTER);
		presetPanel.add(buttonsPanel, BorderLayout.EAST);

		// Action listener for "View" button
		viewButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Close the input panel
				layeredPane.remove(overlayPanel);
				layeredPane.repaint();

				// Open the createFrame with the preset data
				openCreateFrameWithPreset(presetName);
			}
		});

		// Action listener for "Select" button
		selectButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Add the preset to the table
				addPresetRowToTable(presetName, tableModel);
				layeredPane.remove(inputPanel);
				layeredPane.repaint();
			}
		});

		return presetPanel;
	}

	private JPanel overlayPanel;

	private void showAddRowInputBox(JButton addRowButton, JTable table, DefaultTableModel tableModel)
	{

		JLayeredPane layeredPane = getLayeredPane();

		// Prepare the overlay panel
		overlayPanel = new JPanel(null);
		overlayPanel.setOpaque(false);
		overlayPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());

		// Create the inputPanel
		JPanel inputPanel = new JPanel(new BorderLayout());
		inputPanel.setBackground(new Color(255, 255, 255, 230));
		inputPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));


		// Panel to hold presets
		JPanel presetsPanel = new JPanel();
		presetsPanel.setLayout(new BoxLayout(presetsPanel, BoxLayout.Y_AXIS));

		// Scroll pane for presetsPanel
		JScrollPane scrollPane = new JScrollPane(presetsPanel);
		scrollPane.setPreferredSize(new Dimension(250, 200));

		// Populate the presetsPanel with preset entries
		for (String presetName : presets.keySet())
		{
			JPanel presetPanel = createPresetPanel(presetName, tableModel, layeredPane, inputPanel);
			presetsPanel.add(presetPanel);
		}

		// Create the "Create..." button
		JButton createButton = new JButton("Create...");
		createButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		// Add the "Create..." button below the list
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		buttonPanel.add(Box.createVerticalStrut(5)); // Add some spacing
		buttonPanel.add(createButton);
		buttonPanel.add(Box.createVerticalStrut(5)); // Add some spacing

		inputPanel.add(scrollPane, BorderLayout.CENTER);
		inputPanel.add(buttonPanel, BorderLayout.SOUTH);

		// Position the input panel above the '+' button
		Point buttonLocation = SwingUtilities.convertPoint(addRowButton, new Point(0, -250), layeredPane);
		int inputPanelWidth = 250;
		int x = buttonLocation.x;

		// Adjust x position if it goes off-screen
		if (x + inputPanelWidth > layeredPane.getWidth())
		{
			x = layeredPane.getWidth() - inputPanelWidth;
		}
		if (x < 0)
		{
			x = 0;
		}

		inputPanel.setBounds(x, buttonLocation.y, inputPanelWidth, 250); // Adjusted height

		overlayPanel.add(inputPanel);

		layeredPane.add(overlayPanel, JLayeredPane.POPUP_LAYER);
		layeredPane.revalidate();
		layeredPane.repaint();

		// Action listener for "Create..." button
		createButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				layeredPane.remove(overlayPanel);
				layeredPane.repaint();
				openCreateFrame();
			}
		});

		// Event handling for preset selection
		// Modify the code to pass overlayPanel and layeredPane when creating preset panels
		presetsPanel.removeAll();
		for (String presetName : presets.keySet())
		{
			JPanel presetPanel = createPresetPanel(presetName, tableModel, layeredPane, overlayPanel);
			presetsPanel.add(presetPanel);
		}

		// If there are no presets available, show a message
		if (presets.isEmpty())
		{
			JLabel noPresetsLabel = new JLabel("No presets available. Please create one.");
			noPresetsLabel.setHorizontalAlignment(SwingConstants.CENTER);
			inputPanel.removeAll();
			inputPanel.add(noPresetsLabel, BorderLayout.CENTER);
			inputPanel.add(buttonPanel, BorderLayout.SOUTH); // Add the "Create..." button
		}

		// Add a mouse listener to the overlayPanel to detect clicks outside the inputPanel
		overlayPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				// Check if click was outside inputPanel
				if (!inputPanel.getBounds().contains(e.getPoint()))
				{
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
			}
		});

		// Consume mouse events on the overlay to prevent them from reaching underlying components
		overlayPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				// Do nothing to consume the event
			}
		});
	}

	JPanel skillPanel;

	private void openCreateFrame()
	{
		selectedEquipment = new HashMap<>();
		selectedPrayers = new HashMap<>();
		selectedStyle = null; // Initialize selectedStyle

		// Initialize createFrame
		createFrame = new JFrame("Create New Preset");
		createFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Main panel with BorderLayout
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		// === New Top Panel with Base Template Selection ===
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		JLabel baseTemplateLabel = new JLabel("Base Template:");
		JComboBox<String> baseTemplateComboBox = new JComboBox<>();

		// Add "None" as the default option
		baseTemplateComboBox.addItem("None");

		// Populate the combo box with existing preset names
		for (String presetName : presets.keySet())
		{
			baseTemplateComboBox.addItem(presetName);
		}

		topPanel.add(baseTemplateLabel);
		topPanel.add(baseTemplateComboBox);

		// Add action listener to update the create frame when a preset is selected
		baseTemplateComboBox.addActionListener(e -> {
			String selectedPresetName = (String) baseTemplateComboBox.getSelectedItem();
			if (selectedPresetName != null && !selectedPresetName.equals("None"))
			{
				// Load the selected preset as the base template
				loadPresetAsBaseTemplate(selectedPresetName);
			}
			else
			{
				// Clear the selections if "None" is selected
				selectedEquipment.clear();
				selectedPrayers.clear();
				selectedStyle = null;
				updateEquipmentPanel();
				updatePrayerPanel();
				updateStylePanel();
			}
		});

		// === Content Panels ===
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));

		// Create equipment, prayer, and style panels
		equipmentPanel = createEquipmentPanel(selectedEquipment);
		prayerPanel = createPrayerPanel();
		stylePanel = createStylePanel();
		skillPanel = createSkillPanel(); // Add this line

		// Combine style and skill panels vertically
		JPanel styleAndSkillPanel = new JPanel();
		styleAndSkillPanel.setLayout(new BoxLayout(styleAndSkillPanel, BoxLayout.Y_AXIS));
		styleAndSkillPanel.add(stylePanel);
		styleAndSkillPanel.add(Box.createVerticalStrut(10));
		styleAndSkillPanel.add(skillPanel);

		// Add panels to contentPanel
		contentPanel.add(equipmentPanel);
		contentPanel.add(Box.createHorizontalStrut(10));
		contentPanel.add(prayerPanel);
		contentPanel.add(Box.createHorizontalStrut(10));
		contentPanel.add(styleAndSkillPanel); // Add the combined panel

		// Save preset panel at the bottom
		JPanel savePresetPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JTextField presetNameField = new JTextField(20);
		JButton savePresetButton = new JButton("Save Preset");

		savePresetPanel.add(new JLabel("Preset Name:"));
		savePresetPanel.add(presetNameField);
		savePresetPanel.add(savePresetButton);

		// Add components to mainPanel
		mainPanel.add(topPanel, BorderLayout.NORTH);
		mainPanel.add(contentPanel, BorderLayout.CENTER);
		mainPanel.add(savePresetPanel, BorderLayout.SOUTH);

		// Add mainPanel to frame
		createFrame.add(mainPanel);

		createFrame.pack();
		createFrame.setSize(1100, 775); // Adjust the size to fit all components
		createFrame.setLocationRelativeTo(null);
		createFrame.setVisible(true);

		// Action listener for savePresetButton
		savePresetButton.addActionListener(e -> {
			String presetName = presetNameField.getText().trim();
			if (presetName.isEmpty())
			{
				JOptionPane.showMessageDialog(createFrame, "Please enter a preset name.");
				return;
			}
			if (presets.containsKey(presetName))
			{
				JOptionPane.showMessageDialog(createFrame, "A preset with that name already exists.");
				return;
			}
			if (savePreset(presetName))
			{
				createFrame.dispose();
			}
		});
	}

	private void loadPresetAsBaseTemplate(String presetName)
	{
		Preset basePreset = presets.get(presetName);
		if (basePreset == null)
		{
			JOptionPane.showMessageDialog(createFrame, "Preset not found: " + presetName);
			return;
		}

		// Copy the equipment, prayers, skill levels, potions, and style from the base preset
		selectedEquipment = new HashMap<>(basePreset.getEquipment());
		selectedPrayers = new HashMap<>(basePreset.getPrayers());
		selectedStyle = basePreset.getSelectedStyle();
		baseLevels = new HashMap<>(basePreset.getBaseLevels());
		selectedPotions = new HashMap<>(basePreset.getSelectedPotions());
		selectedAttackType = basePreset.getSelectedAttackType();

		// Update the UI components to reflect the selected base template
		updateEquipmentPanel();
		updatePrayerPanel();
		updateStylePanel();
		updateAttackTypeSelection();
		updateSkillPanel();
	}

	private void updateSkillPanel()
	{
		String[] skills = {"Attack", "Strength", "Ranged", "Magic"};
		for (String skill : skills)
		{
			baseLevelFields.get(skill).setText(String.valueOf(baseLevels.get(skill)));
			potionComboBoxes.get(skill).setSelectedItem(selectedPotions.get(skill));
			updateVirtualLevel(skill);
		}
	}

	private void updateEquipmentPanel()
	{
		// For each slot, update the button icon/text
		for (String slotName : slotButtons.keySet())
		{
			JButton slotButton = slotButtons.get(slotName);
			EquipmentData equipmentData = selectedEquipment.get(slotName);
			if (equipmentData != null)
			{
				// Update the button with the equipment's image
				updateButtonWithEquipment(slotButton, equipmentData);
			}
			else
			{
				// Clear the button or set it to default
				slotButton.setIcon(null);
				slotButton.setText("+");
				slotButton.setToolTipText(slotName);
			}
		}

		// Update the summary panel
		updateSummary();
	}

	private void updatePrayerPanel()
	{
		// For each prayer, update the button icon
		for (Prayers prayer : prayerButtons.keySet())
		{
			JButton button = prayerButtons.get(prayer);
			boolean isEnabled = selectedPrayers.getOrDefault(prayer, false);
			updatePrayerButtonIcon(button, prayer, isEnabled);
		}

		// Update the prayer summary
		updatePrayerSummary();
	}

	private void updateStylePanel()
	{
		// Update the style buttons
		for (String styleName : styleButtons.keySet())
		{
			JToggleButton button = styleButtons.get(styleName);
			if (styleName.equals(selectedStyle))
			{
				button.setSelected(true);
			}
			else
			{
				button.setSelected(false);
			}
		}

		updateStyleSelection(); // This will enable/disable attack type buttons

		// Update attack type buttons
		updateAttackTypeSelection();
	}

	private Map<Integer, String> rowToPresetMap = new HashMap<>();

	private void addPresetRowToTable(String presetName, DefaultTableModel tableModel)
	{
		Preset preset = presets.get(presetName);
		if (preset == null)
		{
			JOptionPane.showMessageDialog(this, "Preset not found: " + presetName);
			return;
		}
		int insertRowIndex = tableModel.getRowCount();

		// Create a new row with the correct number of columns
		int columnCount = tableModel.getColumnCount();
		Object[] rowData = new Object[columnCount];

		// Set the preset name in the "Name" column (column index 0)
		rowData[0] = presetName;

		// Add the row to the table model
		tableModel.addRow(rowData);


		// Store the preset associated with this row
		rowToPresetMap.put(insertRowIndex, presetName);

		// Compute cell values for existing NPC columns
		for (int colIndex = 1; colIndex < columnCount; colIndex++)
		{
			NPCData npcData = columnToNPCDataMap.get(colIndex);
			if (npcData != null)
			{
				int defenseValue = getDefenseValueForColumn(tableModel, colIndex);
				CalculationResult result = computeCellValue(preset, npcData, defenseValue);
				tableModel.setValueAt(result.value, insertRowIndex, colIndex);

				String cellKey = insertRowIndex + "," + colIndex;
				cellDiagnosticInfo.put(cellKey, result.diagnosticInfo);
			}
		}
	}

	private int getDefenseValueForColumn(DefaultTableModel tableModel, int columnIndex)
	{
		// Assuming defense value is stored in row index 1
		Object defenseObj = tableModel.getValueAt(0, columnIndex);
		if (defenseObj instanceof Integer)
		{
			return (Integer) defenseObj;
		}
		else if (defenseObj instanceof String)
		{
			try
			{
				return Integer.parseInt((String) defenseObj);
			}
			catch (NumberFormatException e)
			{
				// Handle invalid number format
			}
		}
		// Return a default value or handle error
		return 0;
	}

	private boolean savePreset(String presetName)
	{
		// Validate that a style is selected
		if (selectedStyle == null)
		{
			JOptionPane.showMessageDialog(createFrame, "Please select a combat style.");
			return false;
		}

		// If melee style, ensure attack type is selected
		if (isMeleeStyle(selectedStyle) && selectedAttackType == null)
		{
			JOptionPane.showMessageDialog(createFrame, "Please select an attack type for melee style.");
			return false;
		}
		// Create copies of the selected equipment and prayers
		Map<String, EquipmentData> presetEquipment = new HashMap<>(selectedEquipment);
		Map<Prayers, Boolean> presetPrayers = new HashMap<>(selectedPrayers);
		String presetSelectedStyle = selectedStyle;
		String presetAttackType = selectedAttackType;

		// Copy base levels and selected potions
		Map<String, Integer> presetBaseLevels = new HashMap<>(baseLevels);
		Map<String, String> presetSelectedPotions = new HashMap<>(selectedPotions);

		// Create a new preset object
		Preset preset = new Preset(
			presetName,
			presetEquipment,
			presetPrayers,
			presetSelectedStyle,
			presetBaseLevels,
			presetSelectedPotions,
			presetAttackType
		);

		preset.setVisible(true);

		// Save the preset
		presets.put(presetName, preset);

		// Automatically add the preset to the table
		if (!visiblePresets.contains(presetName))
		{
			visiblePresets.add(presetName);
			addPresetRowToTable(presetName, tableModel);
		}

		// Save equipment presets to file
		saveEquipmentPresets();
		JOptionPane.showMessageDialog(createFrame, "Preset '" + presetName + "' created successfully.");
		return true;
	}

	private void saveEquipmentPresets()
	{
		try
		{
			// Ensure the dpsutility folder exists
			if (!Files.exists(DPS_UTILITY_FOLDER))
			{
				Files.createDirectories(DPS_UTILITY_FOLDER);
			}

			// Convert the presets map to a list
			List<Preset> presetList = new ArrayList<>(presets.values());

			// Serialize to JSON
			String equipmentPresetsJson = gson.toJson(presetList);

			// Write to file
			Files.write(EQUIPMENT_PRESETS_FILE, equipmentPresetsJson.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			e.printStackTrace();
			log.error("Error saving equipment presets", e);
		}
	}

	private void addNPCColumn(String npcName)
	{
		tableModel.addColumn(npcName);
		int columnIndex = tableModel.getColumnCount() - 1;
		applyCustomRenderers();
		// Get the NPCData for this NPC
		NPCData npcData = getNPCFromName(npcName);
		if (npcData != null)
		{

			// Store the NPCData in the map
			columnToNPCDataMap.put(columnIndex, npcData);

			// Set the defense value in the defense row (row index 0)
			int defenseValue = npcData.getSkills().getDef();
			tableModel.setValueAt(defenseValue, 0, columnIndex);

			// Set checkbox default in '15/16?' row (row index 1)
			tableModel.setValueAt(Boolean.FALSE, 1, columnIndex);

			// Compute cell values for existing preset rows
			for (int rowIndex = 2; rowIndex < tableModel.getRowCount(); rowIndex++)
			{
				String presetName = rowToPresetMap.get(rowIndex);
				if (presetName != null)
				{
					Preset preset = presets.get(presetName);
					if (preset != null)
					{
						CalculationResult result = computeCellValue(preset, npcData, defenseValue);
						tableModel.setValueAt(result.value, rowIndex, columnIndex);

						String cellKey = rowIndex + "," + columnIndex;
						cellDiagnosticInfo.put(cellKey, result.diagnosticInfo);
					}
				}
			}
			table.getColumnModel().getColumn(columnIndex).setCellRenderer(new DPSCellRenderer());
			// Save NPC columns to file
			saveNPCColumns();
		}
	}

	private int attackLevel = 99; // Default values
	private int strengthLevel = 99;
	private int rangedLevel = 99;
	private int magicLevel = 99;
	Map<String, JButton> slotButtons = new HashMap<>();

	JPanel equipmentPanel;

	private JPanel createEquipmentPanel(Map<String, EquipmentData> presetEquipment)
	{
		equipmentPanel = new JPanel();
		equipmentPanel.setLayout(new BorderLayout());

		// Set border with padding
		equipmentPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));

		JLabel equipmentLabel = new JLabel("Equipment", SwingConstants.CENTER);
		equipmentLabel.setFont(new Font("Arial", Font.BOLD, 16));
		equipmentPanel.add(equipmentLabel, BorderLayout.NORTH);

		// Equipment grid panel
		JPanel equipmentGrid = new JPanel();
		equipmentGrid.setLayout(new GridBagLayout());
		equipmentPanel.add(equipmentGrid, BorderLayout.CENTER);

		// GridBagConstraints for layout
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5); // Spacing between buttons

		// List of equipment slots
		List<String> equipmentSlots = Arrays.asList(
			"head", "cape", "neck", "ammo", "weapon",
			"body", "shield", "legs", "hands", "feet", "ring"
		);
		int slotIndex = 0;

		// Clear the slotButtons map
		slotButtons.clear();

		// First row, 1 button centered
		gbc.gridy = 0;
		gbc.gridx = 1; // Centered in 3 columns
		JButton button1 = createSquareButton("+");
		String slot = equipmentSlots.get(slotIndex++);
		addEquipmentButtonListener(button1, slot);
		slotButtons.put(slot, button1);
		equipmentGrid.add(button1, gbc);

		// Second row, 3 buttons
		gbc.gridy = 1;
		for (int i = 0; i < 3; i++)
		{
			gbc.gridx = i;
			JButton button = createSquareButton("+");
			slot = equipmentSlots.get(slotIndex++);
			addEquipmentButtonListener(button, slot);
			slotButtons.put(slot, button);
			equipmentGrid.add(button, gbc);
		}

		// Third row, 3 buttons
		gbc.gridy = 2;
		for (int i = 0; i < 3; i++)
		{
			gbc.gridx = i;
			JButton button = createSquareButton("+");
			slot = equipmentSlots.get(slotIndex++);
			addEquipmentButtonListener(button, slot);
			slotButtons.put(slot, button);
			equipmentGrid.add(button, gbc);
		}

		// Fourth row, 1 button centered
		gbc.gridy = 3;
		gbc.gridx = 1;
		JButton button10 = createSquareButton("+");
		slot = equipmentSlots.get(slotIndex++);
		addEquipmentButtonListener(button10, slot);
		slotButtons.put(slot, button10);
		equipmentGrid.add(button10, gbc);

		// Fifth row, 3 buttons
		gbc.gridy = 4;
		for (int i = 0; i < 3; i++)
		{
			gbc.gridx = i;
			JButton button = createSquareButton("+");
			slot = equipmentSlots.get(slotIndex++);
			addEquipmentButtonListener(button, slot);
			slotButtons.put(slot, button);
			equipmentGrid.add(button, gbc);
		}

		// Create and add the summary panel
		JPanel summaryPanel = createSummaryPanel();
		equipmentPanel.add(summaryPanel, BorderLayout.SOUTH);

		// If presetEquipment is provided, update the buttons
		if (presetEquipment != null)
		{
			for (Map.Entry<String, EquipmentData> entry : presetEquipment.entrySet())
			{
				String slotName = entry.getKey();
				EquipmentData equipmentData = entry.getValue();
				JButton slotButton = slotButtons.get(slotName);
				if (slotButton != null)
				{
					// Update selectedEquipment
					selectedEquipment.put(slotName, equipmentData);
					// Update the button with the equipment's image
					updateButtonWithEquipment(slotButton, equipmentData);
				}
			}
			// Update the summary panel
			updateSummary();
		}

		return equipmentPanel;
	}

	private void updateButtonWithEquipment(JButton button, EquipmentData equipmentData)
	{
		// Use itemManager to get the image
		int itemId = equipmentData.getId();
		AsyncBufferedImage itemImage = itemManager.getImage(itemId);

		// Clear the button's text since we'll use an image
		button.setText("");
		// Set the tooltip to the equipment name and stats
		String tooltipText = getEquipmentTooltipText(equipmentData);
		button.setToolTipText(tooltipText);

		// Set a placeholder icon if desired
		button.setIcon(new ImageIcon()); // Empty icon for now

		// When the image is loaded, update the button's icon
		itemImage.onLoaded(() -> {
			SwingUtilities.invokeLater(() -> {
				ImageIcon icon = new ImageIcon(itemImage);
				// Resize the icon to fit the button
				int width = button.getWidth();
				int height = button.getHeight();
				if (width == 0 || height == 0)
				{
					// Fallback to default size if button size not available
					width = 60;
					height = 60;
				}
				Image image = icon.getImage();
				Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
				icon = new ImageIcon(scaledImage);
				button.setIcon(icon);
			});
		});
	}

	private JPanel createSummaryPanel()
	{
		JPanel summaryPanel = new JPanel();
		summaryPanel.setLayout(new BorderLayout());
		summaryPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));

		// Panels for categories
		JPanel categoriesPanel = new JPanel();
		categoriesPanel.setLayout(new BoxLayout(categoriesPanel, BoxLayout.Y_AXIS));

		// Attack Bonus
		JLabel attackBonusLabel = new JLabel("Attack Bonus", SwingConstants.CENTER);
		attackBonusLabel.setFont(new Font("Arial", Font.BOLD, 14));
		categoriesPanel.add(attackBonusLabel);
		JPanel attackPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		attackStabLabel = new JLabel("Stab: 0");
		attackSlashLabel = new JLabel("Slash: 0");
		attackCrushLabel = new JLabel("Crush: 0");
		attackMagicLabel = new JLabel("Magic: 0");
		attackRangeLabel = new JLabel("Range: 0");
		attackPanel.add(attackStabLabel);
		attackPanel.add(attackSlashLabel);
		attackPanel.add(attackCrushLabel);
		attackPanel.add(attackMagicLabel);
		attackPanel.add(attackRangeLabel);
		categoriesPanel.add(attackPanel);

		// Defense Bonus
		JLabel defenseBonusLabel = new JLabel("Defense Bonus", SwingConstants.CENTER);
		defenseBonusLabel.setFont(new Font("Arial", Font.BOLD, 14));
		categoriesPanel.add(defenseBonusLabel);
		JPanel defensePanel = new JPanel(new GridLayout(0, 2, 5, 5));
		defenseStabLabel = new JLabel("Stab: 0");
		defenseSlashLabel = new JLabel("Slash: 0");
		defenseCrushLabel = new JLabel("Crush: 0");
		defenseMagicLabel = new JLabel("Magic: 0");
		defenseRangeLabel = new JLabel("Range: 0");
		defensePanel.add(defenseStabLabel);
		defensePanel.add(defenseSlashLabel);
		defensePanel.add(defenseCrushLabel);
		defensePanel.add(defenseMagicLabel);
		defensePanel.add(defenseRangeLabel);
		categoriesPanel.add(defensePanel);

		// Other Bonuses
		JLabel otherBonusesLabel = new JLabel("Other Bonuses", SwingConstants.CENTER);
		otherBonusesLabel.setFont(new Font("Arial", Font.BOLD, 14));
		categoriesPanel.add(otherBonusesLabel);
		JPanel otherPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		meleeStrengthLabel = new JLabel("Melee Strength: 0");
		rangedStrengthLabel = new JLabel("Ranged Strength: 0");
		magicDamageLabel = new JLabel("Magic Damage: 0%");
		prayerLabel = new JLabel("Prayer: 0");
		otherPanel.add(meleeStrengthLabel);
		otherPanel.add(rangedStrengthLabel);
		otherPanel.add(magicDamageLabel);
		otherPanel.add(prayerLabel);
		categoriesPanel.add(otherPanel);

		// Target-Specific
		JLabel targetSpecificLabel = new JLabel("Target-Specific", SwingConstants.CENTER);
		targetSpecificLabel.setFont(new Font("Arial", Font.BOLD, 14));
		categoriesPanel.add(targetSpecificLabel);
		JPanel targetPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		undeadLabel = new JLabel("Undead: No");
		slayerLabel = new JLabel("Slayer: No");
		targetPanel.add(undeadLabel);
		targetPanel.add(slayerLabel);
		categoriesPanel.add(targetPanel);

		summaryPanel.add(categoriesPanel, BorderLayout.CENTER);

		return summaryPanel;
	}

	private void updateSummary()
	{
		// Initialize totals
		int totalAttackStab = 0;
		int totalAttackSlash = 0;
		int totalAttackCrush = 0;
		int totalAttackMagic = 0;
		int totalAttackRange = 0;

		int totalDefenseStab = 0;
		int totalDefenseSlash = 0;
		int totalDefenseCrush = 0;
		int totalDefenseMagic = 0;
		int totalDefenseRange = 0;

		int totalMeleeStrength = 0;
		int totalRangedStrength = 0;
		int totalMagicDamage = 0;
		int totalPrayer = 0;

		boolean hasUndead = false;
		boolean hasSlayer = false;

		// Sum up the bonuses from selected equipment
		for (EquipmentData eqData : selectedEquipment.values())
		{
			if (eqData == null)
			{
				continue;
			}

			// Offensive stats
			EquipmentStats offensive = eqData.getOffensive();
			if (offensive != null)
			{
				totalAttackStab += offensive.getStab();
				totalAttackSlash += offensive.getSlash();
				totalAttackCrush += offensive.getCrush();
				totalAttackMagic += offensive.getMagic();
				totalAttackRange += offensive.getRanged();
			}

			// Defensive stats
			EquipmentStats defensive = eqData.getDefensive();
			if (defensive != null)
			{
				totalDefenseStab += defensive.getStab();
				totalDefenseSlash += defensive.getSlash();
				totalDefenseCrush += defensive.getCrush();
				totalDefenseMagic += defensive.getMagic();
				totalDefenseRange += defensive.getRanged();
			}

			// Other bonuses
			EquipmentBonuses bonuses = eqData.getBonuses(); // Assuming EquipmentBonuses has the necessary fields
			if (bonuses != null)
			{
				totalMeleeStrength += bonuses.getStr();
				totalRangedStrength += bonuses.getRanged_str();
				totalMagicDamage += bonuses.getMagic_str();
				totalPrayer += bonuses.getPrayer();
				//if (bonuses.isUndead()) {
				hasUndead = true;
				//}
				//if (bonuses.isSlayer()) {
				hasSlayer = true;
				//}
			}
		}

		// Update labels
		attackStabLabel.setText("Stab: " + totalAttackStab);
		attackSlashLabel.setText("Slash: " + totalAttackSlash);
		attackCrushLabel.setText("Crush: " + totalAttackCrush);
		attackMagicLabel.setText("Magic: " + totalAttackMagic);
		attackRangeLabel.setText("Range: " + totalAttackRange);

		defenseStabLabel.setText("Stab: " + totalDefenseStab);
		defenseSlashLabel.setText("Slash: " + totalDefenseSlash);
		defenseCrushLabel.setText("Crush: " + totalDefenseCrush);
		defenseMagicLabel.setText("Magic: " + totalDefenseMagic);
		defenseRangeLabel.setText("Range: " + totalDefenseRange);

		meleeStrengthLabel.setText("Melee Strength: " + totalMeleeStrength);
		rangedStrengthLabel.setText("Ranged Strength: " + totalRangedStrength);
		magicDamageLabel.setText("Magic Damage: " + totalMagicDamage + "%");
		prayerLabel.setText("Prayer: " + totalPrayer);

		undeadLabel.setText("Undead: " + (hasUndead ? "Yes" : "No"));
		slayerLabel.setText("Slayer: " + (hasSlayer ? "Yes" : "No"));
	}

	private void addEquipmentButtonListener(JButton button, String slot)
	{
		// Existing ActionListener for left-click events
		button.addActionListener(e -> {
			showEquipmentSelectionDialog(button, slot);
		});

		// Add a MouseListener to detect right-click events
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isRightMouseButton(e))
				{
					// Remove the equipment from the selectedEquipment map
					selectedEquipment.remove(slot);

					// Reset the button to default appearance
					button.setIcon(null);
					button.setText("+");
					button.setToolTipText(slot);

					// Update the summary panel
					updateSummary();
				}
			}
		});
	}

	private void showEquipmentSelectionDialog(JButton button, String slot)
	{
		List<EquipmentData> equipmentList = equipmentMap.get(slot);
		if (equipmentList == null || equipmentList.isEmpty())
		{
			JOptionPane.showMessageDialog(createFrame, "No equipment available for slot: " + slot);
			return;
		}

		// Create a word bank of equipment names
		List<String> equipmentNames = new ArrayList<>();
		for (EquipmentData eq : equipmentList)
		{
			equipmentNames.add(eq.getName());
		}
		// Sort equipment names alphabetically
		Collections.sort(equipmentNames);

		// Create the autocomplete panel (inputPanel)
		JPanel inputPanel = new JPanel(new BorderLayout());
		inputPanel.setBackground(new Color(255, 255, 255, 230));
		inputPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

		JTextField textField = new JTextField();
		inputPanel.add(textField, BorderLayout.NORTH);

		textField.setFocusTraversalKeysEnabled(false);

		DefaultListModel<String> listModel = new DefaultListModel<>();
		JList<String> suggestionList = new JList<>(listModel);
		suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane suggestionScrollPane = new JScrollPane(suggestionList);
		suggestionScrollPane.setPreferredSize(new Dimension(200, 150));
		inputPanel.add(suggestionScrollPane, BorderLayout.CENTER);

		// Prepare the overlay panel
		JLayeredPane layeredPane = createFrame.getLayeredPane();

		JPanel overlayPanel = new JPanel(null);
		overlayPanel.setOpaque(false);
		overlayPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());

		// Position the input panel near the button
		Point buttonLocation = SwingUtilities.convertPoint(button, new Point(0, button.getHeight()), layeredPane);
		int inputPanelWidth = 200;
		int x = buttonLocation.x;
		int y = buttonLocation.y;

		// Adjust x position if it goes off-screen
		if (x + inputPanelWidth > layeredPane.getWidth())
		{
			x = layeredPane.getWidth() - inputPanelWidth;
		}
		if (x < 0)
		{
			x = 0;
		}

		inputPanel.setBounds(x, y, inputPanelWidth, 200);

		// Add inputPanel to overlayPanel
		overlayPanel.add(inputPanel);

		// Add overlayPanel to layeredPane
		layeredPane.add(overlayPanel, JLayeredPane.POPUP_LAYER);
		layeredPane.revalidate();
		layeredPane.repaint();

		textField.requestFocusInWindow();

		// Implement autocomplete functionality
		Runnable updateSuggestions = () -> {
			String text = textField.getText();
			listModel.clear();
			List<String> matchingNames = new ArrayList<>();
			List<String> otherNames = new ArrayList<>();

			for (String name : equipmentNames)
			{
				if (text.isEmpty())
				{
					matchingNames.add(name);
				}
				else if (name.toLowerCase().startsWith(text.toLowerCase()))
				{
					matchingNames.add(name);
				}
				else if (name.toLowerCase().contains(text.toLowerCase()))
				{
					otherNames.add(name);
				}
			}
			// Sort the lists alphabetically
			Collections.sort(matchingNames);
			Collections.sort(otherNames);
			// Add matching names first
			for (String name : matchingNames)
			{
				listModel.addElement(name);
			}
			// Then add other names
			for (String name : otherNames)
			{
				listModel.addElement(name);
			}
			if (!listModel.isEmpty())
			{
				suggestionList.setSelectedIndex(0);
			}
		};

		// Call updateSuggestions initially to populate the list
		updateSuggestions.run();

		textField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				updateSuggestions.run();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				updateSuggestions.run();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				updateSuggestions.run();
			}
		});

		suggestionList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					String selectedName = suggestionList.getSelectedValue();
					selectEquipmentForSlot(button, slot, selectedName);
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
			}
		});

		textField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					String selectedName;
					if (!suggestionList.isSelectionEmpty())
					{
						selectedName = suggestionList.getSelectedValue();
					}
					else
					{
						selectedName = textField.getText();
					}
					selectEquipmentForSlot(button, slot, selectedName);
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
				else if (e.getKeyCode() == KeyEvent.VK_DOWN)
				{
					int index = suggestionList.getSelectedIndex();
					if (index < listModel.getSize() - 1)
					{
						suggestionList.setSelectedIndex(index + 1);
						suggestionList.ensureIndexIsVisible(index + 1);
					}
				}
				else if (e.getKeyCode() == KeyEvent.VK_UP)
				{
					int index = suggestionList.getSelectedIndex();
					if (index > 0)
					{
						suggestionList.setSelectedIndex(index - 1);
						suggestionList.ensureIndexIsVisible(index - 1);
					}
				}
				else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					// Remove the overlayPanel when ESC is pressed
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
			}
		});

		// Add a mouse listener to the overlayPanel to detect clicks outside the inputPanel
		overlayPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				// Check if click was outside inputPanel
				if (!inputPanel.getBounds().contains(e.getPoint()))
				{
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
			}
		});

		// Consume mouse events on the overlay to prevent them from reaching underlying components
		overlayPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				// Do nothing to consume the event
			}
		});
	}

	private JFrame createFrame;

	private void selectEquipmentForSlot(JButton button, String slot, String equipmentName)
	{
		List<EquipmentData> equipmentList = equipmentMap.get(slot);
		if (equipmentList == null)
		{
			return;
		}
		EquipmentData selectedEquipmentData = null;
		for (EquipmentData eq : equipmentList)
		{
			if (eq.getName().equalsIgnoreCase(equipmentName))
			{
				selectedEquipmentData = eq;
				break;
			}
		}
		if (selectedEquipmentData == null)
		{
			JOptionPane.showMessageDialog(createFrame, "Equipment not found: " + equipmentName);
			return;
		}
		// Save the selected equipment
		selectedEquipment.put(slot, selectedEquipmentData);

		// Use itemManager to get the image
		int itemId = selectedEquipmentData.getId();
		AsyncBufferedImage itemImage = itemManager.getImage(itemId);

		// Clear the button's text since we'll use an image
		button.setText("");
		// Optionally, set the tooltip to the equipment name
		button.setToolTipText(selectedEquipmentData.getName());

		// Set a placeholder icon if desired
		button.setIcon(new ImageIcon()); // Empty icon for now

		// When the image is loaded, update the button's icon
		itemImage.onLoaded(() -> {
			SwingUtilities.invokeLater(() -> {
				ImageIcon icon = new ImageIcon(itemImage);
				// Resize the icon to fit the button
				int width = button.getWidth();
				int height = button.getHeight();
				if (width == 0 || height == 0)
				{
					// Fallback to default size if button size not available
					width = 60;
					height = 60;
				}
				Image image = icon.getImage();
				Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
				icon = new ImageIcon(scaledImage);
				button.setIcon(icon);
			});
		});

		// Save the selected equipment
		selectedEquipment.put(slot, selectedEquipmentData);

		// Update the button with the equipment's image
		updateButtonWithEquipment(button, selectedEquipmentData);

		// Update the summary panel
		updateSummary();
	}

	private void updateAllVirtualLevels()
	{
		String[] skills = {"Attack", "Strength", "Ranged", "Magic"};
		for (String skill : skills)
		{
			int virtualLevel = calculateVirtualLevel(skill);
			virtualLevelLabels.get(skill).setText(String.valueOf(virtualLevel));
		}
	}

	JPanel prayerPanel;

	private JPanel createPrayerPanel()
	{
		prayerPanel = new JPanel();
		prayerPanel.setLayout(new BorderLayout());

		// Set border with padding
		prayerPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));

		JLabel prayerLabel = new JLabel("Prayers", SwingConstants.CENTER);
		prayerLabel.setFont(new Font("Arial", Font.BOLD, 16));
		prayerPanel.add(prayerLabel, BorderLayout.NORTH);

		// Prayer grid panel
		JPanel prayerGrid = new JPanel();
		GridBagLayout gridBagLayout = new GridBagLayout();
		prayerGrid.setLayout(gridBagLayout);
		prayerPanel.add(prayerGrid, BorderLayout.CENTER);

		// Clear the prayerButtons map
		prayerButtons.clear();

		// Create buttons for each prayer
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5); // Spacing between buttons
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		gbc.weighty = 0;

		int prayersPerRow = 5; // Adjust as needed
		int row = 0;
		int col = 0;

		for (Prayers prayer : Prayers.values())
		{
			SquareButton button = new SquareButton("");
			button.setToolTipText(prayer.name);
			prayerButtons.put(prayer, button);

			// Add action listener to toggle prayer state
			button.addActionListener(e -> {
				boolean isEnabled = selectedPrayers.getOrDefault(prayer, false);
				selectedPrayers.put(prayer, !isEnabled);
				updatePrayerButtonIcon(button, prayer, !isEnabled);
				// Update the summary when a prayer is toggled
				updatePrayerSummary();

				// Update virtual levels
				updateAllVirtualLevels();
			});

			gbc.gridx = col;
			gbc.gridy = row;
			prayerGrid.add(button, gbc);

			col++;
			if (col >= prayersPerRow)
			{
				col = 0;
				row++;
			}
		}

		// After all buttons are added, update their icons
		for (Prayers prayer : Prayers.values())
		{
			boolean isEnabled = selectedPrayers.getOrDefault(prayer, false);
			JButton button = prayerButtons.get(prayer);
			updatePrayerButtonIcon(button, prayer, isEnabled);
		}

		// Create and add the summary panel at the bottom
		JPanel summaryPanel = createPrayerSummaryPanel();
		prayerPanel.add(summaryPanel, BorderLayout.SOUTH);

		return prayerPanel;
	}


	private Map<String, Preset> presets = new HashMap<>();

	private void updatePrayerButtonIcon(JButton button, Prayers prayer, boolean isEnabled)
	{
		int spriteId = isEnabled ? prayer.enabledID : prayer.disabledID;

		// Clear current icon
		button.setIcon(null);

		// Fetch the sprite on the client thread
		clientThread.invokeLater(() -> {
			BufferedImage sprite = spriteManager.getSprite(spriteId, 0);
			if (sprite != null)
			{
				int size = button.getWidth();
				if (size <= 0)
				{
					// Use preferred size if actual size is not available
					Dimension dim = button.getPreferredSize();
					size = Math.max(dim.width, dim.height);
				}

				// Resize the image to fit the button
				Image scaledImage = sprite.getScaledInstance(size, size, Image.SCALE_SMOOTH);

				// Update the UI on the EDT
				SwingUtilities.invokeLater(() -> {
					button.setIcon(new ImageIcon(scaledImage));
				});
			}
		});
	}

	private Map<Prayers, Boolean> selectedPrayers = new HashMap<>();

	// Helper method to create the Style panel
// Map to hold style buttons
	private Map<String, JToggleButton> styleButtons = new HashMap<>();

	// Variable to store the selected style
	private String selectedStyle;

	JPanel stylePanel;

	JTextField attackLevelField;
	JTextField strengthLevelField;
	JTextField rangedLevelField;
	JTextField magicLevelField;

	private void updateStyleSelection()
	{
		if (isMeleeStyle(selectedStyle))
		{
			// Enable attack type buttons
			for (JToggleButton button : attackTypeButtons.values())
			{
				button.setEnabled(true);
			}
		}
		else
		{
			// Disable attack type buttons and reset selection
			selectedAttackType = null;
			for (JToggleButton button : attackTypeButtons.values())
			{
				button.setEnabled(false);
				button.setSelected(false);
			}
		}
	}

	private void updateAttackTypeSelection()
	{
		for (String attackType : attackTypeButtons.keySet())
		{
			JToggleButton button = attackTypeButtons.get(attackType);
			if (attackType.equals(selectedAttackType))
			{
				button.setSelected(true);
			}
			else
			{
				button.setSelected(false);
			}
		}
	}

	private JToggleButton createAttackTypeToggleButton(String attackType)
	{
		JToggleButton button = new JToggleButton(attackType);
		button.setFocusPainted(false);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);

		// Set button dimensions
		Dimension buttonSize = new Dimension(80, 30);
		button.setPreferredSize(buttonSize);
		button.setMaximumSize(buttonSize);

		// Add action listener
		button.addActionListener(e -> {
			selectedAttackType = attackType;
			updateAttackTypeSelection();
		});

		// Highlight if selected
		if (attackType.equals(selectedAttackType))
		{
			button.setSelected(true);
		}

		return button;
	}

	private JToggleButton createStyleToggleButton(String styleName)
	{
		JToggleButton button = new JToggleButton(styleName);
		button.setFocusPainted(false);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);

		// Set button dimensions
		Dimension buttonSize = new Dimension(100, 30);
		button.setPreferredSize(buttonSize);
		button.setMaximumSize(buttonSize);

		// Add action listener
		button.addActionListener(e -> {
			selectedStyle = styleName;
			updateStyleSelection();
		});

		// Highlight if selected
		if (styleName.equals(selectedStyle))
		{
			button.setSelected(true);
		}

		styleButtons.put(styleName, button);

		return button;
	}

	private JPanel createStylePanel()
	{
		stylePanel = new JPanel();
		stylePanel.setLayout(new BorderLayout());

		// Set border with padding
		stylePanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));

		JLabel styleLabel = new JLabel("Styles", SwingConstants.CENTER);
		styleLabel.setFont(new Font("Arial", Font.BOLD, 16));
		stylePanel.add(styleLabel, BorderLayout.NORTH);

		// Main panel with vertical BoxLayout to hold the groups
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		stylePanel.add(mainPanel, BorderLayout.CENTER);

		// Define the style groups
		String[][] styleGroups = {
			{"Accurate", "Aggressive", "Defensive", "Controlled"},
			{"Accurate (Ranged)", "Rapid", "Longrange"},
			{"Standard", "Defensive (Magic)"}
		};

		String[] groupLabels = {
			"Melee Styles",
			"Ranged Styles",
			"Magic Styles"
		};

		ButtonGroup styleButtonGroup = new ButtonGroup(); // Ensure only one style is selected

		// Loop through the groups to create sections
		for (int i = 0; i < styleGroups.length; i++)
		{
			String[] styles = styleGroups[i];
			String groupLabel = groupLabels[i];

			// Section label
			JLabel sectionLabel = new JLabel(groupLabel, SwingConstants.CENTER);
			sectionLabel.setFont(new Font("Arial", Font.BOLD, 14));
			sectionLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0)); // Top and bottom padding
			sectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center within BoxLayout
			mainPanel.add(sectionLabel);

			// Panel for the styles in this group
			JPanel stylesGroupPanel = new JPanel(new GridLayout(0, 2, 5, 5)); // 2 columns
			for (String styleName : styles)
			{
				JToggleButton button = createStyleToggleButton(styleName);
				styleButtonGroup.add(button);
				stylesGroupPanel.add(button);
			}

			mainPanel.add(stylesGroupPanel);

			// If this is the melee group, add attack type options
			if (groupLabel.equals("Melee Styles"))
			{
				mainPanel.add(Box.createVerticalStrut(10)); // Spacing

				JLabel attackTypeLabel = new JLabel("Attack Type", SwingConstants.CENTER);
				attackTypeLabel.setFont(new Font("Arial", Font.BOLD, 14));
				attackTypeLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
				attackTypeLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center within BoxLayout
				mainPanel.add(attackTypeLabel);

				JPanel attackTypePanel = new JPanel(new GridLayout(0, 3, 5, 5)); // 3 columns

				ButtonGroup attackTypeButtonGroup = new ButtonGroup();
				String[] attackTypes = {"Stab", "Slash", "Crush"};
				for (String attackType : attackTypes)
				{
					JToggleButton attackTypeButton = createAttackTypeToggleButton(attackType);
					attackTypeButtonGroup.add(attackTypeButton);
					attackTypePanel.add(attackTypeButton);
					attackTypeButtons.put(attackType, attackTypeButton);

					// Initially disable attack type buttons
					attackTypeButton.setEnabled(false);
				}

				mainPanel.add(attackTypePanel);
			}

			mainPanel.add(Box.createVerticalStrut(10)); // Spacing between groups
		}

		// Add glue to push content to the top
		mainPanel.add(Box.createVerticalGlue());

		return stylePanel;
	}

	private Map<Integer, NPCData> columnToNPCDataMap = new HashMap<>();

	private JToggleButton createStyleButton(String styleName)
	{
		JToggleButton button = new JToggleButton(styleName);
		button.setFocusPainted(false); // Optional: Remove focus border
		button.setAlignmentX(Component.LEFT_ALIGNMENT);

		// Set button dimensions (wider than tall)
		Dimension buttonSize = new Dimension(200, 30); // Adjust width and height as needed
		button.setPreferredSize(buttonSize);
		button.setMaximumSize(buttonSize);

		// Add action listener to handle selection
		button.addActionListener(e -> {
			// Deselect previous selection
			if (selectedStyle != null && styleButtons.containsKey(selectedStyle))
			{

				JToggleButton previousButton = styleButtons.get(selectedStyle);
				previousButton.setBackground(null); // Reset background
				previousButton.setForeground(Color.BLACK); // Reset text color
			}

			// Update selected style
			selectedStyle = styleName;
			button.setBackground(Color.RED.darker().darker());
			button.setForeground(Color.WHITE); // Change text color for contrast

			// Save to selectedStyle variable
			this.selectedStyle = styleName;
		});

		// If this style is the currently selected one, highlight it
		if (styleName.equals(selectedStyle))
		{
			button.setBackground(Color.RED.darker().darker());
			button.setForeground(Color.WHITE); // Change text color for contrast
		}
		else
		{
			button.setBackground(null);
			button.setForeground(Color.BLACK);
		}

		// Add the button to the map
		styleButtons.put(styleName, button);

		return button;
	}

	// Helper method to create square buttons with a plus sign
	private JButton createSquareButton(String text)
	{
		JButton button = new JButton(text);
		Dimension size = new Dimension(60, 60); // Square button size
		button.setPreferredSize(size);
		button.setMinimumSize(size);
		button.setMaximumSize(size);
		return button;
	}
}

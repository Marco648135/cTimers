

package com.advancedraidtracker.ui.dpsanalysis;

import com.advancedraidtracker.AdvancedRaidTrackerPlugin;
import com.advancedraidtracker.ui.BaseFrame;
import com.advancedraidtracker.ui.PresetManager;
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
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.event.*;
import java.awt.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

@Slf4j
public class DPSWindow extends BaseFrame
{
	private final ItemManager itemManager;
	List<String> allNPCs = new ArrayList<>();
	List<NPCData> allNPCData = new ArrayList<>();
	private Map<String, List<EquipmentData>> equipmentMap = new HashMap<>();

	private final Map<String, Set<String>> trigramIndex = new HashMap<>();


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
		List<String> presetNames = new ArrayList<>(PresetManager.getPresets().keySet()); // Preserve the order
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
						Preset preset = PresetManager.getPresets().remove(oldName);
						if (preset != null)
						{
							preset.setName(newName);
							PresetManager.add(newName, preset);

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

					Preset preset = PresetManager.getPresets().get(presetName);
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
			Preset preset = PresetManager.getPresets().get(presetName);
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
				PresetManager.remove(presetName);
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
				PresetManager.add(preset.getName(), preset);
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

			if (preset.isMeleeVoid())
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

			maxHit = (int) ((effectiveStrengthLevel * (strengthBonus + 64) + 320) / 640.0);
			attackRoll = effectiveAttackLevel * (attackBonus + 64);
			if (preset.isSalveE())
			{
				maxHit = (int) (maxHit * 1.20);
				attackRoll = (int) (attackRoll * 1.20);
			}
			else if (preset.isSalve() || preset.isSlayer())
			{
				maxHit = (int) (maxHit * (7.0 / 6.0));
				attackRoll = (int) (attackRoll * (7.0 / 6.0));
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
			else if (selectedStyle.equals("Rapid"))
			{
				attackSpeed--;
			}

			effectiveStrengthLevel += 8;
			effectiveAttackLevel += 8;

			if (preset.isRangeEVoid())
			{
				effectiveStrengthLevel = (int) (effectiveStrengthLevel * 1.125);
				effectiveAttackLevel = (int) (effectiveAttackLevel * 1.1);
			}
			else if (preset.isRangeVoid())
			{
				effectiveStrengthLevel = (int) (effectiveStrengthLevel * 1.1);
				effectiveAttackLevel = (int) (effectiveAttackLevel * 1.1);
			}

			// Equipment bonuses
			int attackBonus = 0;
			int strengthBonus = 0; // Ranged strength bonus

			for (EquipmentData data : preset.getEquipment().values())
			{
				attackBonus += data.getOffensive().getRanged();
				strengthBonus += data.getBonuses().getRanged_str();
				if (data.getName().toLowerCase().contains("quiver"))
				{
					attackBonus += 10;
					strengthBonus += 1;
				}
			}

			attackRoll = effectiveAttackLevel * (attackBonus + 64);

			maxHit = (int) (.5 + effectiveStrengthLevel * (strengthBonus + 64) / 640.0);

			if (preset.isSalveE())
			{
				maxHit = (int) (maxHit * 1.2);
			}
			else if (preset.isSalve())
			{
				maxHit = (int) (maxHit * (7.0 / 6.0));
			}
			else if (preset.isSlayer())
			{
				maxHit = (int) (maxHit * 1.15); //todo craws/webweaver
			}

			if (preset.getEquipment().get("weapon").getName().contains("Twisted bow"))
			{
				int magicValue = Math.max(npcData.getSkills().getMagic(), npcData.getOffensive().getMagic());
				//outside of chambers:
				magicValue = Math.min(250, magicValue);
				double tbowAccuracyMultiplier = 140 + (((((30 * magicValue) / 10.0) - 10) / 100.0) - (Math.pow((3 * magicValue / 10.0) - 100, 2) / 100.0));
				tbowAccuracyMultiplier = Math.min(tbowAccuracyMultiplier, 140);
				double tbowDamageMultiplier = 250 + (((((30 * magicValue) / 10.0) - 14) / 100.0) - (Math.pow((3 * magicValue / 10.0) - 140, 2) / 100.0));
				tbowDamageMultiplier = Math.min(tbowDamageMultiplier, 250);

				tbowAccuracyMultiplier /= 100;
				tbowDamageMultiplier /= 100;

				maxHit = (int) (maxHit * tbowDamageMultiplier);
				attackRoll = (int) (attackRoll * tbowAccuracyMultiplier);
			}

		}
		else if (isMagicStyle(selectedStyle))
		{
			effectiveAttackLevel = virtualLevels.get("Magic");

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

		double dps = dpa / (attackSpeed * .6);

		List<String> diagnosticInfo = List.of("Max Hit: " + maxHit, "Attack Roll: " + attackRoll, "Hitchance: " + hitChance, "Defense Roll: " + defenseRoll, "DPA: " + dpa, "Weapon Speed: " + attackSpeed, "DPS: " + dps);

		String value = String.format("%.2f", dps);
		return new CalculationResult(value, diagnosticInfo);
	}


	private double calculateExpectedDamage(double maxHit, double hitChance)
	{
		if (maxHit <= 0.0)
		{
			return 0.0;
		}
		return hitChance * ((maxHit / 2.0) + (1.0 / (maxHit + 1)));
	}


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

	private void openCreateFrame()
	{
		PresetManager.openAddPresetWindow();
	}



	private Map<Integer, String> rowToPresetMap = new HashMap<>();

	private void addPresetRowToTable(String presetName, DefaultTableModel tableModel)
	{
		Preset preset = PresetManager.getPresets().get(presetName);
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
			List<Preset> presetList = new ArrayList<>(PresetManager.getPresets().values());

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
					Preset preset = PresetManager.getPresets().get(presetName);
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

	private Map<Integer, NPCData> columnToNPCDataMap = new HashMap<>();

}
package com.advancedraidtracker.ui.advancedstatistics;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;

import com.advancedraidtracker.ui.customrenderers.IconManager;
import com.advancedraidtracker.utility.RoomUtil;
import com.advancedraidtracker.utility.UISwingUtility;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedLabel;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedPanel;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedTable;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.advancedraidtracker.ui.advancedstatistics.AdvancedData.StallEvent;
import com.advancedraidtracker.ui.advancedstatistics.AdvancedData.KilledNylo;
import net.runelite.client.game.ItemManager;

public class AdvancedNylocasPanel extends DataTab
{
	private JPanel subpanel1;
	private JPanel subpanel2;
	private JPanel subpanel3;
	private JPanel subpanel4;

	// Tables
	private JTable stallEventsTable;
	private JTable naturalKillTable;
	// Removed bigNyloTable as per changes
	private JTable lastDeathsTable;

	// Slider components
	private JSlider targetTimeSlider;
	private JLabel targetTimeLabel;

	// Earliest and latest for slider
	private int earliest;
	private int latest;

	// Target time for coloring
	private int targetTime;

	public AdvancedNylocasPanel(AdvancedData dataSource, ItemManager itemManager)
	{
		super(dataSource);
		setLayout(new BorderLayout());

		// Initialize subpanels
		subpanel1 = getThemedPanel(new BorderLayout());
		subpanel2 = getThemedPanel(new BorderLayout());
		subpanel3 = getThemedPanel(new BorderLayout());
		subpanel4 = getThemedPanel(new BorderLayout());

		// Initialize tables
		stallEventsTable = getThemedTable();
		naturalKillTable = getThemedTable();
		// Removed bigNyloTable as per changes
		lastDeathsTable = getThemedTable();

		// Set row height to 40 pixels for specified tables
		stallEventsTable.setRowHeight(50);
		naturalKillTable.setRowHeight(20);
		lastDeathsTable.setRowHeight(40); // For Last Deaths panel

		// Add tables to subpanels with scroll panes
		subpanel1.add(new JScrollPane(stallEventsTable), BorderLayout.CENTER);
		subpanel2.add(new JScrollPane(naturalKillTable), BorderLayout.CENTER);
		// subpanel3.add(new JScrollPane(bigNyloTable), BorderLayout.CENTER); // Removed
		subpanel4.add(new JScrollPane(lastDeathsTable), BorderLayout.CENTER);

		// Create titled borders for each subpanel
		TitledBorder title1 = BorderFactory.createTitledBorder("Stall Details");
		TitledBorder title2 = BorderFactory.createTitledBorder("Expiry Analysis");
		TitledBorder title3 = BorderFactory.createTitledBorder("Big Analysis");
		TitledBorder title4 = BorderFactory.createTitledBorder("Last Deaths");

		JPanel wrapperPanel1 = getThemedPanel(new BorderLayout());
		wrapperPanel1.setBorder(title1);
		wrapperPanel1.add(subpanel1, BorderLayout.CENTER);

		JPanel wrapperPanel2 = getThemedPanel(new BorderLayout());
		wrapperPanel2.setBorder(title2);

		// Initialize slider components for Expiry Analysis
		earliest = 252;
		latest = Math.max(calculateLatest(), earliest + 16);
		targetTime = latest + 16; // Default to max value
		targetTimeSlider = new JSlider(earliest, latest + 16, targetTime);
		targetTimeSlider.setMajorTickSpacing(16);
		targetTimeSlider.setMinorTickSpacing(4);
		targetTimeSlider.setSnapToTicks(true);
		targetTimeSlider.setPaintTicks(true);

		targetTimeLabel = getThemedLabel("Target Time: " + RoomUtil.time(targetTime));

		// Panel to hold slider and label
		JPanel sliderPanel = getThemedPanel(new BorderLayout());
		sliderPanel.add(targetTimeLabel, BorderLayout.WEST);
		sliderPanel.add(targetTimeSlider, BorderLayout.CENTER);

		// Add slider panel above the table in subpanel2
		JPanel expiryPanel = getThemedPanel(new BorderLayout());
		expiryPanel.add(sliderPanel, BorderLayout.NORTH);
		expiryPanel.add(new JScrollPane(naturalKillTable), BorderLayout.CENTER);

		wrapperPanel2.add(expiryPanel, BorderLayout.CENTER);

		JPanel wrapperPanel3 = getThemedPanel(new BorderLayout());
		wrapperPanel3.setBorder(title3);
		// wrapperPanel3.add(subpanel3, BorderLayout.CENTER);

		// Initialize subpanel3 without adding bigNyloTable
		// subpanel3 = getThemedPanel(new BorderLayout()); // Already initialized

		wrapperPanel3.add(subpanel3, BorderLayout.CENTER);

		JPanel wrapperPanel4 = getThemedPanel(new BorderLayout());
		wrapperPanel4.setBorder(title4);
		wrapperPanel4.add(subpanel4, BorderLayout.CENTER);

		// Add wrapper panels to main panel
		JPanel mainPanel = getThemedPanel(new GridLayout(2, 2));
		mainPanel.add(wrapperPanel1);
		mainPanel.add(wrapperPanel2);
		mainPanel.add(wrapperPanel3);
		mainPanel.add(wrapperPanel4);

		add(mainPanel, BorderLayout.CENTER);

		// Add listener to slider to update label and table coloring
		targetTimeSlider.addChangeListener(e -> {
			targetTime = targetTimeSlider.getValue();
			targetTimeLabel.setText("Target Time: " + RoomUtil.time(targetTime));
			refreshNaturalKillTable(); // Update table coloring based on new target time
		});

		// Initial data setup
		refresh();
	}

	@Override
	public void refresh()
	{
		int stallEvents = dataSource.getStalls().size();
		int killedEvents = dataSource.getKilledNylos().size();
		System.out.println("Nylocas Panel refreshing. Stall Events: " + stallEvents + ", killed events: " + killedEvents);

		earliest = 252;
		latest = calculateLatest();
		targetTimeSlider.setMinimum(earliest);
		targetTimeSlider.setMaximum(latest + 16);

		// Update the tables with data
		updateStallEventsTable();
		updateNaturalKillTable();
		updateBigNyloTable();
		updateLastDeathsTable(); // New method to update Last Deaths table
	}

	private void refreshNaturalKillTable()
	{
		// Just refresh the renderer to update colors
		naturalKillTable.repaint();
	}

	private void updateStallEventsTable()
	{
		StallEventsTableModel model = new StallEventsTableModel(dataSource.getStalls());
		stallEventsTable.setModel(model);

		// Set up renderers for custom columns
		stallEventsTable.getColumnModel().getColumn(2).setCellRenderer(new OldestNylosRenderer());
		stallEventsTable.getColumnModel().getColumn(3).setCellRenderer(new AgeDistributionRenderer());
		stallEventsTable.getColumnModel().getColumn(4).setCellRenderer(new StatisticsRenderer());

		// Center text in all columns except custom rendered columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

		for (int i = 0; i < stallEventsTable.getColumnCount(); i++)
		{
			if (i != 2 && i != 3 && i != 4) // Skip custom renderers
			{
				stallEventsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
			}
		}

		// Adjust column widths
		Set<Integer> flexibleColumns = new HashSet<>();
		flexibleColumns.add(3); // "Age Distribution" column index
		adjustTableColumnWidths(stallEventsTable, flexibleColumns);

		// Set auto resize mode
		stallEventsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	}

	private void updateNaturalKillTable()
	{
		earliest = 252;
		latest = calculateLatest();

		NaturalKillTableModel model = new NaturalKillTableModel(dataSource.getKilledNylos(), earliest, latest);
		naturalKillTable.setModel(model);

		naturalKillTable.getColumnModel().getColumn(3).setCellRenderer(new ProgressBarRenderer());

		// Center text in all columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

		for (int i = 0; i < naturalKillTable.getColumnCount(); i++)
		{
			if (i != 3) // Skip custom renderer
			{
				naturalKillTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
			}
		}

		// Adjust column widths
		Set<Integer> flexibleColumns = new HashSet<>();
		flexibleColumns.add(3); // "Progress" column index
		adjustTableColumnWidths(naturalKillTable, flexibleColumns);

		// Set auto resize mode
		naturalKillTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	}

	private void updateBigNyloTable()
	{
		List<KilledNylo> bigNylos = new ArrayList<>();
		for (KilledNylo nylo : dataSource.getKilledNylos())
		{
			String descriptionLower = nylo.getDescription().toLowerCase();
			if (descriptionLower.contains("big") && !descriptionLower.contains("split"))
			{
				// Extract wave number from description
				int waveNumber = extractWaveNumberFromDescription(nylo.getDescription());
				if (waveNumber > 19)
				{
					bigNylos.add(nylo);
				}
			}
		}

		// Remove previous content from subpanel3
		subpanel3.removeAll();

		// Create the grid component
		BigNyloTileGrid grid = new BigNyloTileGrid(bigNylos);

		// Add the grid to subpanel3 within a scroll pane
		JScrollPane scrollPane = new JScrollPane(grid);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		subpanel3.add(scrollPane, BorderLayout.CENTER);

		// Revalidate and repaint
		subpanel3.revalidate();
		subpanel3.repaint();
	}

	private void updateLastDeathsTable()
	{
		List<KilledNylo> killedNylos = dataSource.getKilledNylos();
		// Sort the list by time descending
		killedNylos.sort((k1, k2) -> k2.getRoomTick() - k1.getRoomTick());

		LastDeathsTableModel model = new LastDeathsTableModel(killedNylos);
		lastDeathsTable.setModel(model);

		// Set up renderers
		lastDeathsTable.getColumnModel().getColumn(1).setCellRenderer(new NyloImageRenderer());

		// Center text in other columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

		for (int i = 0; i < lastDeathsTable.getColumnCount(); i++)
		{
			if (i != 1)
			{
				lastDeathsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
			}
		}

		// Adjust column widths
		Set<Integer> flexibleColumns = new HashSet<>();
		flexibleColumns.add(1); // "Nylo" is the flexible column
		adjustTableColumnWidths(lastDeathsTable, flexibleColumns);

		// Set auto resize mode
		lastDeathsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	}

	private int calculateLatest()
	{
		List<KilledNylo> killedNylos = dataSource.getKilledNylos();
		int lastRoomTick = killedNylos.stream().mapToInt(KilledNylo::getDeathTick).max().orElse(0);
		int latestMultipleOf4 = ((lastRoomTick + 3) / 4) * 4; // Next multiple of 4
		return latestMultipleOf4;
	}

	// Adjusts the column widths based on content
	private void adjustTableColumnWidths(JTable table, Set<Integer> flexibleColumns)
	{
		TableColumnModel columnModel = table.getColumnModel();

		// First, calculate total width needed for fixed columns
		int totalFixedWidth = 0;
		for (int i = 0; i < table.getColumnCount(); i++)
		{
			if (!flexibleColumns.contains(i))
			{
				int width = getMaxColumnWidth(table, i);
				TableColumn column = columnModel.getColumn(i);
				column.setPreferredWidth(width);
				column.setMinWidth(width);
				column.setMaxWidth(width);
				totalFixedWidth += width;
			}
		}

		// Get the container's width
		Container viewport = SwingUtilities.getAncestorOfClass(JViewport.class, table);
		int availableWidth;
		if (viewport != null)
		{
			availableWidth = viewport.getWidth();
		}
		else
		{
			availableWidth = table.getPreferredSize().width;
		}

		// Calculate width for flexible columns
		int remainingWidth = Math.max(0, availableWidth - totalFixedWidth);
		if (flexibleColumns.size() > 0)
		{
			int flexibleColumnWidth = remainingWidth / flexibleColumns.size();

			// Set minimum width for flexible columns
			int minFlexibleWidth = 100; // Adjust this value as needed
			flexibleColumnWidth = Math.max(flexibleColumnWidth, minFlexibleWidth);

			// Apply the width to flexible columns
			for (int i : flexibleColumns)
			{
				TableColumn column = columnModel.getColumn(i);
				column.setPreferredWidth(flexibleColumnWidth);
				column.setMinWidth(minFlexibleWidth);
				column.setMaxWidth(2000); // Set a reasonable maximum
			}
		}

		// Update table size
		Dimension tableSize = new Dimension(
			totalFixedWidth + (remainingWidth / flexibleColumns.size()) * flexibleColumns.size(),
			table.getPreferredSize().height
		);
		table.setPreferredScrollableViewportSize(tableSize);

		// Force immediate revalidation
		table.revalidate();
		table.repaint();

		// Add a component listener to handle resize events
		if (viewport != null)
		{
			ComponentListener[] listeners = viewport.getComponentListeners();
			boolean hasResizeListener = false;
			for (ComponentListener listener : listeners)
			{
				if (listener instanceof ResizeListener)
				{
					hasResizeListener = true;
					break;
				}
			}
			if (!hasResizeListener)
			{
				viewport.addComponentListener(new ResizeListener(table, flexibleColumns));
			}
		}
	}

	// Separate class to handle resize events
	private class ResizeListener extends ComponentAdapter
	{
		private final JTable table;
		private final Set<Integer> flexibleColumns;
		private Timer resizeTimer;

		public ResizeListener(JTable table, Set<Integer> flexibleColumns)
		{
			this.table = table;
			this.flexibleColumns = flexibleColumns;
			this.resizeTimer = new Timer(150, e -> adjustTableColumnWidths(table, flexibleColumns));
			this.resizeTimer.setRepeats(false);
		}

		@Override
		public void componentResized(ComponentEvent e)
		{
			// Use a timer to prevent multiple rapid adjustments
			if (resizeTimer.isRunning())
			{
				resizeTimer.restart();
			}
			else
			{
				resizeTimer.start();
			}
		}
	}


	// Helper method to get the maximum preferred width of a column
	private int getMaxColumnWidth(JTable table, int columnIndex)
	{
		int maxWidth = 0;
		TableModel model = table.getModel();

		// Consider header width
		TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
		Component headerComp = headerRenderer.getTableCellRendererComponent(table, table.getColumnName(columnIndex), false, false, -1, columnIndex);
		maxWidth = Math.max(maxWidth, headerComp.getPreferredSize().width);

		// Consider cell widths
		for (int row = 0; row < model.getRowCount(); row++)
		{
			TableCellRenderer renderer = table.getCellRenderer(row, columnIndex);
			Component comp = renderer.getTableCellRendererComponent(table, model.getValueAt(row, columnIndex), false, false, row, columnIndex);
			maxWidth = Math.max(maxWidth, comp.getPreferredSize().width);
		}

		maxWidth += 5; // Add 5 pixels of padding
		return maxWidth;
	}

	// Extract wave number from description, looking for digits following 'W'
	private int extractWaveNumberFromDescription(String description)
	{
		int waveNumber = -1;
		Pattern pattern = Pattern.compile(".*[wW](\\d+).*");
		Matcher matcher = pattern.matcher(description);
		if (matcher.matches())
		{
			try
			{
				waveNumber = Integer.parseInt(matcher.group(1));
			}
			catch (NumberFormatException e)
			{
				// Ignore parsing errors
			}
		}
		return waveNumber;
	}

	private String extractWaveNumberFromDescriptionAsString(String description)
	{
		Pattern pattern = Pattern.compile(".*[wW](\\d+).*");
		Matcher matcher = pattern.matcher(description);
		if (matcher.matches())
		{
			return matcher.group(1);
		}
		return "";
	}

	// Implementation of StallEventsTableModel
	class StallEventsTableModel extends AbstractTableModel
	{
		private final String[] columnNames = {"Wave", "Alive", "Oldest Nylos", "Age Distribution", "Statistics"};
		private final List<StallEvent> stalls;

		public StallEventsTableModel(List<StallEvent> stalls)
		{
			this.stalls = stalls;
		}

		@Override
		public int getRowCount()
		{
			return stalls.size();
		}

		@Override
		public int getColumnCount()
		{
			return columnNames.length;
		}

		@Override
		public String getColumnName(int column)
		{
			return columnNames[column];
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			StallEvent stallEvent = stalls.get(rowIndex);

			switch (columnIndex)
			{
				case 0:
					return stallEvent.getWaveStalled();
				case 1:
					return stallEvent.getAgeMap().size();
				case 2:
					return stallEvent.getAgeMap();
				case 3:
					return stallEvent.getAgeMap();
				case 4:
					return stallEvent.getAgeMap();
				default:
					return null;
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0:
				case 1:
					return Integer.class;
				case 2:
				case 3:
				case 4:
					return Map.class;
				default:
					return Object.class;
			}
		}
	}

	// Renderer for the 4 oldest nylos ages in a 2x2 panel
	class OldestNylosRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
													   boolean isSelected, boolean hasFocus, int row, int column)
		{
			Map<String, Integer> ageMap = (Map<String, Integer>) value;
			// Get the ages
			List<Integer> ages = new ArrayList<>(ageMap.values());
			ages.sort(Collections.reverseOrder()); // Sort in descending order

			// Get the 4 oldest ages
			List<Integer> oldestAges = ages.subList(0, Math.min(4, ages.size()));

			// Create a panel to display the ages in 2x2 grid
			JPanel panel = getThemedPanel(new GridLayout(2, 2));
			panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

			for (int i = 0; i < 4; i++)
			{
				JLabel label;
				if (i < oldestAges.size())
				{
					label = getThemedLabel(String.valueOf(oldestAges.get(i)), SwingConstants.CENTER);
				}
				else
				{
					label = getThemedLabel("", SwingConstants.CENTER);
				}
				label.setHorizontalAlignment(SwingConstants.CENTER); // Center text
				panel.add(label);
			}

			return panel;
		}
	}

	// Renderer for Age Distribution (mini bar chart)
	class AgeDistributionRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
													   boolean isSelected, boolean hasFocus, int row, int column)
		{
			Map<String, Integer> ageMap = (Map<String, Integer>) value;

			// Create a histogram of the ages
			List<Integer> ages = new ArrayList<>(ageMap.values());
			int[] bins = new int[53];
			for (int age : ages)
			{
				bins[age]++;
			}

			AgeDistributionComponent component = new AgeDistributionComponent(bins);
			component.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

			return component;
		}
	}

	class AgeDistributionComponent extends JComponent
	{
		private int[] bins;
		private Color backgroundColor = config.primaryDark();
		private Color dataColor = UISwingUtility.getTransparentColor(config.markerColor(), 255);
		private Color axisColor = config.boxColor();

		private static final int MARGIN = 3;        // 3-pixel margin on all sides
		private static final int INNER_MARGIN = 2;  // 2-pixel margin between axis (box) and the data

		public AgeDistributionComponent(int[] bins)
		{
			this.bins = bins;
			setPreferredSize(new Dimension(100, 50)); // Adjust size as needed
		}

		// Setters for customization
		public void setBackgroundColor(Color color)
		{
			this.backgroundColor = color;
		}

		public void setDataColor(Color color)
		{
			this.dataColor = color;
		}

		public void setAxisColor(Color color)
		{
			this.axisColor = color;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;

			int width = getWidth();
			int height = getHeight();

			// Fill background
			g2d.setColor(backgroundColor);
			g2d.fillRect(0, 0, width, height);

			// Draw axis box
			g2d.setColor(axisColor);
			int axisX = MARGIN;
			int axisY = MARGIN;
			int axisWidth = width - 2 * MARGIN - 1;  // Adjust for drawRect specifics
			int axisHeight = height - 2 * MARGIN - 1; // Adjust for drawRect specifics
			g2d.drawRect(axisX, axisY, axisWidth, axisHeight);

			// Define the graph drawing area
			int graphX = MARGIN + INNER_MARGIN;
			int graphY = MARGIN + INNER_MARGIN;
			int graphWidth = width - 2 * (MARGIN + INNER_MARGIN);
			int graphHeight = height - 2 * (MARGIN + INNER_MARGIN);

			// Draw data
			int maxBin = Arrays.stream(bins).max().getAsInt();
			if (maxBin == 0)
			{
				return; // Nothing to draw
			}

			// Now draw the bars inside the graph area
			int barWidth = (int) Math.ceil((double) graphWidth / bins.length);
			for (int i = 0; i < bins.length; i++)
			{
				int barHeight = (int) ((bins[i] / (double) maxBin) * (graphHeight));
				if (barHeight == 0)
				{
					continue;
				}
				int x = graphX + i * barWidth;
				int y = graphY + (graphHeight - barHeight);

				g2d.setColor(dataColor);
				g2d.fillRect(x, y, barWidth, barHeight);
			}

			// Reset the paint to default
			g2d.setPaint(null);
		}
	}

	// Renderer for Statistics (avg, median)
	class StatisticsRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
													   boolean isSelected, boolean hasFocus, int row, int column)
		{
			Map<String, Integer> ageMap = (Map<String, Integer>) value;
			List<Integer> ages = new ArrayList<>(ageMap.values());
			double avg = ages.stream().mapToInt(Integer::intValue).average().orElse(0);
			Collections.sort(ages);
			int median;
			int size = ages.size();
			if (size % 2 == 0)
			{
				median = (ages.get(size / 2 - 1) + ages.get(size / 2)) / 2;
			}
			else
			{
				median = ages.get(size / 2);
			}

			JPanel panel = getThemedPanel(new GridLayout(2, 1));
			panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

			JLabel avgLabel = getThemedLabel("Avg: " + String.format("%.1f", avg), SwingConstants.CENTER);
			JLabel medianLabel = getThemedLabel("Median: " + median, SwingConstants.CENTER);

			// Center text in labels
			avgLabel.setHorizontalAlignment(SwingConstants.CENTER);
			medianLabel.setHorizontalAlignment(SwingConstants.CENTER);

			panel.add(avgLabel);
			panel.add(medianLabel);

			return panel;
		}
	}

	// Implementation of NaturalKillTableModel
	class NaturalKillTableModel extends AbstractTableModel
	{
		private final String[] columnNames = {"Time", "Expired", "Killed", "Expired vs Killed"};
		private final List<Integer> times;
		private final Map<Integer, Integer> naturallyExpiredCounts;
		private final Map<Integer, Integer> artificiallyKilledCounts;
		private final Map<Integer, List<String>> naturallyExpiredDescriptions;
		private final Map<Integer, List<String>> artificiallyKilledDescriptions;
		private final List<Integer> displayedTimes;

		public NaturalKillTableModel(List<KilledNylo> killedNylos, int earliest, int latest)
		{
			times = new ArrayList<>();
			displayedTimes = new ArrayList<>();
			naturallyExpiredCounts = new HashMap<>();
			artificiallyKilledCounts = new HashMap<>();
			naturallyExpiredDescriptions = new HashMap<>();
			artificiallyKilledDescriptions = new HashMap<>();
			int highestTime = 0;
			for (KilledNylo nylo : killedNylos)
			{
				int deathTick = nylo.getDeathTick();
				int time = ((deathTick + 3) / 4) * 4;
				if (time > highestTime)
				{
					highestTime = time;
				}
			}

			// Initialize times
			for (int time = earliest; time <= highestTime; time += 4)
			{
				times.add(time);
				naturallyExpiredCounts.put(time, 0);
				artificiallyKilledCounts.put(time, 0);
				naturallyExpiredDescriptions.put(time, new ArrayList<>());
				artificiallyKilledDescriptions.put(time, new ArrayList<>());
			}

			// For each KilledNylo, determine which time it belongs to
			for (KilledNylo nylo : killedNylos)
			{
				int deathTick = nylo.getDeathTick();
				int time = ((deathTick + 3) / 4) * 4; // Next multiple of 4 after deathTick

				if (time >= earliest)
				{
					if (nylo.getAge() == 52)
					{
						naturallyExpiredCounts.put(time, naturallyExpiredCounts.get(time) + 1);
						naturallyExpiredDescriptions.get(time).add(nylo.getDescription());
					}
					else
					{
						artificiallyKilledCounts.put(time, artificiallyKilledCounts.get(time) + 1);
						artificiallyKilledDescriptions.get(time).add(nylo.getDescription());
					}
				}
			}

			// Prepare displayed times (omit times with zero total counts)
			for (int time : times)
			{
				int natural = naturallyExpiredCounts.get(time);
				int artificial = artificiallyKilledCounts.get(time);
				if (natural + artificial > 0)
				{
					displayedTimes.add(time);
				}
			}
		}

		@Override
		public int getRowCount()
		{
			return displayedTimes.size();
		}

		@Override
		public int getColumnCount()
		{
			return columnNames.length;
		}

		@Override
		public String getColumnName(int column)
		{
			return columnNames[column];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			if (columnIndex == 0)
			{
				return String.class;
			}
			else if (columnIndex == 1 || columnIndex == 2)
			{
				return Integer.class;
			}
			else
			{
				return Object.class;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			int time = displayedTimes.get(rowIndex);
			switch (columnIndex)
			{
				case 0:
					return RoomUtil.time(time + 16);
				case 1:
					return naturallyExpiredCounts.get(time);
				case 2:
					return artificiallyKilledCounts.get(time);
				case 3:
					return new NaturalKillProgressData(naturallyExpiredCounts.get(time), artificiallyKilledCounts.get(time), time + 16);
				default:
					return null;
			}
		}

		// Add methods to get descriptions for tooltips
		public List<String> getNaturallyExpiredDescriptions(int time)
		{
			return naturallyExpiredDescriptions.get(time);
		}

		public List<String> getArtificiallyKilledDescriptions(int time)
		{
			return artificiallyKilledDescriptions.get(time);
		}

		public int getDisplayedTimeAtRow(int rowIndex)
		{
			return displayedTimes.get(rowIndex);
		}
	}

	class NaturalKillProgressData
	{
		int naturallyExpired;
		int artificiallyKilled;
		int timePlus16;

		public NaturalKillProgressData(int naturallyExpired, int artificiallyKilled, int timePlus16)
		{
			this.naturallyExpired = naturallyExpired;
			this.artificiallyKilled = artificiallyKilled;
			this.timePlus16 = timePlus16;
		}

		public int getNaturallyExpired()
		{
			return naturallyExpired;
		}

		public int getArtificiallyKilled()
		{
			return artificiallyKilled;
		}

		public int getTimePlus16()
		{
			return timePlus16;
		}
	}

	// Renderer for the progress bar
	class ProgressBarRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
													   boolean isSelected, boolean hasFocus, int row, int column)
		{
			NaturalKillProgressData data = (NaturalKillProgressData) value;
			NaturalKillProgressComponent component = new NaturalKillProgressComponent(data.getNaturallyExpired(), data.getArtificiallyKilled(), data.getTimePlus16(), targetTime);
			component.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
			return component;
		}
	}

	class NaturalKillProgressComponent extends JComponent
	{
		private int naturallyExpired;
		private int artificiallyKilled;
		private int timePlus16;
		private int targetTime;
		private Color backgroundColor = config.primaryDark();
		private Color artificialColor;
		private Color naturalColor;
		private static final int MARGIN = 10; // 10 pixels margin on left and right

		public NaturalKillProgressComponent(int naturallyExpired, int artificiallyKilled, int timePlus16, int targetTime)
		{
			this.naturallyExpired = naturallyExpired;
			this.artificiallyKilled = artificiallyKilled;
			this.timePlus16 = timePlus16;
			this.targetTime = targetTime;

			// Set natural color based on condition
			if (timePlus16 <= targetTime)
			{
				naturalColor = Color.GREEN;
				artificialColor = Color.RED;
			}
			else
			{
				naturalColor = Color.RED;
				artificialColor = Color.GREEN;
			}

			// Make color semi-transparent
			naturalColor = UISwingUtility.getTransparentColor(naturalColor, 100);
			artificialColor = UISwingUtility.getTransparentColor(artificialColor, 100);

			setPreferredSize(new Dimension(100, 20));
		}

		public void setBackgroundColor(Color color)
		{
			this.backgroundColor = color;
		}

		public void setArtificialColor(Color color)
		{
			this.artificialColor = color;
		}

		public void setNaturalColor(Color color)
		{
			this.naturalColor = color;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			int total = naturallyExpired + artificiallyKilled;
			if (total == 0)
			{
				return;
			}

			int width = getWidth() - (2 * MARGIN);
			int height = getHeight();
			int xPosition = MARGIN;

			// Calculate the dimensions of the stadium
			int stadiumHeight = (int) (height * 0.25);
			int yPosition = (int) (height * 0.50);
			int arcDiameter = stadiumHeight;

			// Adjust width to account for the semicircular ends
			int innerWidth = width - arcDiameter;

			// Progress calculation
			double naturalPercent = (double) naturallyExpired / total;
			int naturalWidth = (int) (innerWidth * naturalPercent) + arcDiameter / 2;

			Graphics2D g2d = (Graphics2D) g;

			// Set anti-aliasing for smoother rendering
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// Draw background
			g2d.setColor(backgroundColor);
			g2d.fillRect(0, 0, getWidth(), getHeight());

			// Create the full stadium shape for clipping
			Shape fullStadium = new RoundRectangle2D.Double(xPosition, yPosition, width, stadiumHeight,
				stadiumHeight, stadiumHeight);

			// Handle 100% cases
			if (naturallyExpired == total)
			{
				g2d.setColor(naturalColor);
				g2d.fill(fullStadium);
				return;
			}
			else if (artificiallyKilled == total)
			{
				g2d.setColor(artificialColor);
				g2d.fill(fullStadium);
				return;
			}

			// Draw the artificial portion (right side)
			if (artificiallyKilled > 0)
			{
				Shape artificialPortion = new Rectangle2D.Double(
					xPosition + naturalWidth,
					yPosition,
					width - naturalWidth,
					stadiumHeight
				);

				// Create a clip of the intersection between the stadium and artificial portion
				Area clipArea = new Area(fullStadium);
				clipArea.intersect(new Area(artificialPortion));

				g2d.setColor(artificialColor);
				g2d.fill(clipArea);
			}

			// Draw the natural portion (left side)
			if (naturallyExpired > 0)
			{
				Shape naturalPortion = new Rectangle2D.Double(
					xPosition,
					yPosition,
					naturalWidth,
					stadiumHeight
				);

				// Create a clip of the intersection between the stadium and natural portion
				Area clipArea = new Area(fullStadium);
				clipArea.intersect(new Area(naturalPortion));

				g2d.setColor(naturalColor);
				g2d.fill(clipArea);
			}
		}


	}

	// Implementation of LastDeathsTableModel
	class LastDeathsTableModel extends AbstractTableModel
	{
		private final String[] columnNames = {"Time", "Nylo", "Age", "Natural Expiration", "Origin", "Type"};
		private final List<KilledNylo> lastDeaths;

		public LastDeathsTableModel(List<KilledNylo> killedNylos)
		{
			this.lastDeaths = killedNylos;
		}

		@Override
		public int getRowCount()
		{
			return lastDeaths.size();
		}

		@Override
		public int getColumnCount()
		{
			return columnNames.length;
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			return columnNames[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0:
					return String.class; // Time
				case 1:
					return ImageIcon.class; // Nylo (image)
				case 2:
					return Integer.class; // Age
				case 3:
				case 4:
				case 5:
					return String.class; // Natural Expiration, Origin, Type
				default:
					return Object.class;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			KilledNylo nylo = lastDeaths.get(rowIndex);
			switch (columnIndex)
			{
				case 0:
					int ticks = nylo.getRoomTick();
					return RoomUtil.time(ticks);
				case 1:
					return getNyloImage(nylo);
				case 2:
					return nylo.getAge();
				case 3:
					return RoomUtil.time(nylo.getRoomTick() + 52 - nylo.getAge());
				case 4:
					return getNyloOrigin(nylo.getDescription());
				case 5:
					return nylo.getDescription().toLowerCase().contains("split") ? "Split" : "Lane";
				default:
					return null;
			}
		}

		private ImageIcon getNyloImage(KilledNylo nylo)
		{
			String description = nylo.getDescription().toLowerCase();
			boolean isBig = description.contains("big") && !description.contains("split");
			int size = isBig ? 40 : 20;
			ImageIcon icon = null;

			int meleeIndex = description.indexOf("melee");
			int rangeIndex = description.indexOf("range");
			int mageIndex = description.indexOf("mage");

			int firstIndex = -1;
			String type = null;

			if (meleeIndex >= 0 && (firstIndex == -1 || meleeIndex < firstIndex))
			{
				firstIndex = meleeIndex;
				type = "melee";
			}
			if (rangeIndex >= 0 && (firstIndex == -1 || rangeIndex < firstIndex))
			{
				firstIndex = rangeIndex;
				type = "range";
			}
			if (mageIndex >= 0 && (firstIndex == -1 || mageIndex < firstIndex))
			{
				firstIndex = mageIndex;
				type = "mage";
			}

			if (type != null)
			{
				switch (type)
				{
					case "melee":
						icon = IconManager.getIschyros();
						break;
					case "range":
						icon = IconManager.getToxobolos();
						break;
					case "mage":
						icon = IconManager.getHagios();
						break;
				}

				if (icon != null)
				{
					Image image = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
					icon = new ImageIcon(image);
				}
			}

			return icon;
		}

		private String getNyloOrigin(String description)
		{
			String descLower = description.toLowerCase();
			Pattern pattern = Pattern.compile("[w](\\d+)");
			Matcher matcher = pattern.matcher(descLower);
			String origin = "";
			while (matcher.find())
			{
				origin = matcher.group(1);
			}
			return origin;
		}
	}

	// Renderer for Nylo image
	class NyloImageRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
													   boolean isSelected, boolean hasFocus, int row, int column)
		{
			JLabel label = getThemedLabel();
			label.setHorizontalAlignment(SwingConstants.CENTER);
			if (value instanceof ImageIcon)
			{
				label.setIcon((ImageIcon) value);
			}
			if (isSelected)
			{
				label.setBackground(table.getSelectionBackground());
			}
			else
			{
				label.setBackground(table.getBackground());
			}
			label.setOpaque(true);
			return label;
		}
	}

	// BigNyloTileGrid component
	class BigNyloTileGrid extends JPanel
	{
		public BigNyloTileGrid(List<KilledNylo> bigNylos)
		{
			int columns = 6; // Adjust as needed
			int rows = (int) Math.ceil(bigNylos.size() / (double) columns);
			setLayout(new GridLayout(rows, columns, 5, 5)); // 5 px gaps

			// Create tiles
			for (KilledNylo nylo : bigNylos)
			{
				BigNyloTile tile = new BigNyloTile(nylo);
				add(tile);
			}
		}
	}

	// BigNyloTile component
	class BigNyloTile extends JPanel
	{
		private KilledNylo nylo;

		public BigNyloTile(KilledNylo nylo)
		{
			this.nylo = nylo;
			setPreferredSize(new Dimension(50, 50));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D) g;

			// Fill background
			g2d.setColor(config.primaryDark());
			g2d.fillRect(0, 0, getWidth(), getHeight());

			// Draw the 40x40 image centered
			ImageIcon nyloIcon = getNyloImage(nylo);
			if (nyloIcon != null)
			{
				Image image = nyloIcon.getImage();
				int imgX = (getWidth() - 40) / 2;
				int imgY = (getHeight() - 40) / 2;
				g2d.drawImage(image, imgX, imgY, 40, 40, this);
			}

			// Set rendering hints for text
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			// Set font
			g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
			FontMetrics fm = g2d.getFontMetrics();
			g2d.setColor(Color.WHITE);

			// Draw the wave number at bottom right
			String waveString = "W" + extractWaveNumberFromDescriptionAsString(nylo.getDescription());
			int waveWidth = fm.stringWidth(waveString);
			int waveHeight = fm.getHeight();
			g2d.drawString(waveString, getWidth() - waveWidth - 2, getHeight() - 2);

			// Draw the age at bottom left
			String ageString = String.valueOf(nylo.getAge());
			// int ageWidth = fm.stringWidth(ageString);
			g2d.drawString(ageString, 2, getHeight() - 2);

			// Draw the direction first letter at top left
			String direction = extractDirection(nylo.getDescription());
			if (!direction.isEmpty())
			{
				g2d.drawString(direction, 2, fm.getAscent() + 2);
			}

			// Draw the icon at top right
			ImageIcon icon;
			if (nylo.getAge() == 52)
			{
				icon = IconManager.getCross(); // Died naturally
			}
			else
			{
				icon = IconManager.getCheck(); // Did not die naturally
			}
			if (icon != null)
			{
				Image image = icon.getImage();
				int iconSize = 16;
				int iconX = getWidth() - iconSize - 2;
				int iconY = 2;
				g2d.drawImage(image, iconX, iconY, iconSize, iconSize, this);
			}
		}

		private ImageIcon getNyloImage(KilledNylo nylo)
		{
			String description = nylo.getDescription().toLowerCase();
			ImageIcon icon = null;

			// Find the style in the description
			if (description.contains("melee"))
			{
				icon = IconManager.getIschyros();
			}
			else if (description.contains("range"))
			{
				icon = IconManager.getToxobolos();
			}
			else if (description.contains("mage"))
			{
				icon = IconManager.getHagios();
			}

			return icon;
		}

		private String extractWaveNumberFromDescriptionAsString(String description)
		{
			Pattern pattern = Pattern.compile(".*[wW](\\d+).*");
			Matcher matcher = pattern.matcher(description);
			if (matcher.matches())
			{
				return matcher.group(1);
			}
			return "";
		}

		private String extractDirection(String description)
		{
			// Description format: W<wave number> <direction> big <style>

			String descriptionLower = description.toLowerCase();
			if (descriptionLower.contains("south"))
			{
				return "S";
			}
			else if (descriptionLower.contains("west"))
			{
				return "W";
			}
			else if (descriptionLower.contains("east"))
			{
				return "E";
			}
			else
			{
				return "";
			}
		}
	}
}

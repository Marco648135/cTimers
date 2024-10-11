package com.advancedraidtracker.ui.charts;

import com.advancedraidtracker.AdvancedRaidTrackerConfig;
import com.advancedraidtracker.AdvancedRaidTrackerPlugin;
import com.advancedraidtracker.constants.RaidRoom;
import com.advancedraidtracker.constants.TobIDs;
import static com.advancedraidtracker.constants.TobIDs.MAGE_THRALL;
import com.advancedraidtracker.ui.PresetManager;
import static com.advancedraidtracker.ui.charts.ChartActionType.ADD_ELEMENT;
import static com.advancedraidtracker.ui.charts.ChartActionType.REMOVE_ELEMENT;
import static com.advancedraidtracker.ui.charts.ChartObjectType.ATTACK;
import static com.advancedraidtracker.ui.charts.ChartObjectType.AUTO;
import static com.advancedraidtracker.ui.charts.ChartObjectType.LINE;
import static com.advancedraidtracker.ui.charts.ChartObjectType.TEXT;
import static com.advancedraidtracker.ui.charts.ChartObjectType.THRALL;
import com.advancedraidtracker.ui.charts.chartcreator.ChartCreatorFrame;
import com.advancedraidtracker.ui.charts.chartcreator.ChartStatusBar;
import com.advancedraidtracker.ui.charts.chartcreator.CustomPanel;
import com.advancedraidtracker.ui.charts.chartelements.ChartAuto;
import com.advancedraidtracker.ui.charts.chartelements.ChartLine;
import com.advancedraidtracker.ui.charts.chartelements.ChartTextBox;
import com.advancedraidtracker.ui.charts.chartelements.OutlineBox;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.getReplacement;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.getSpellIcon;
import com.advancedraidtracker.ui.charts.chartelements.ThrallOutlineBox;
import com.advancedraidtracker.ui.dpsanalysis.DPSWindow;
import com.advancedraidtracker.ui.dpsanalysis.EquipmentData;
import com.advancedraidtracker.ui.dpsanalysis.NPCData;
import com.advancedraidtracker.ui.dpsanalysis.Preset;
import com.advancedraidtracker.utility.*;
import com.advancedraidtracker.utility.Point;
import com.advancedraidtracker.utility.probability.ProbabilityCalculator;
import com.advancedraidtracker.utility.probability.ProbabilityUtility;
import com.advancedraidtracker.utility.weapons.PlayerAnimation;
import com.advancedraidtracker.utility.weapons.AnimationDecider;
import com.advancedraidtracker.utility.wrappers.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.swing.Timer;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Prayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

import static com.advancedraidtracker.ui.charts.ChartConstants.*;
import static com.advancedraidtracker.ui.charts.ChartIO.loadChartFromFile;
import static com.advancedraidtracker.ui.charts.ChartIO.saveChart;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.getSpellSpecificIcon;
import static com.advancedraidtracker.utility.UISwingUtility.*;
import static com.advancedraidtracker.utility.datautility.DataWriter.PLUGIN_DIRECTORY;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.getIcon;
import static com.advancedraidtracker.utility.weapons.PlayerAnimation.*;
import net.runelite.client.util.ImageUtil;

@Slf4j
public class ChartPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, FocusListener, KeyEventDispatcher
{
	@Setter
	private ChartStatusBar statusBar;
	private final int TITLE_BAR_PLUS_TAB_HEIGHT = 63;
	private boolean shouldWrap;
	private BufferedImage img;
	boolean changesSaved = false;

	private List<PlayerAnimation> playerAnimationsInUse = new ArrayList<>();
	private Map<PlayerAnimation, Rectangle> actionIconPositions = new HashMap<>();
	private PlayerAnimation hoveredAction = null;
	Map<PlayerAnimation, BufferedImage> iconMap = new HashMap<>();
	Map<Integer, Double> probabilityMap = new ConcurrentHashMap<>();

	// Dimensions for the action icons box

	private int sidebarWidth = 200; // Initial width, will adjust dynamically
	private int sidebarPadding = 10;
	private int iconSize = 32;
	private int iconSpacing = 10;
	private int buttonHeight = 40;
	private int buttonSpacing = 10;
	private int sidebarX;
	private int sidebarY;

	String associatedFileName = "";
	int scale;
	int boxCount;
	int boxHeight;
	int boxWidth;
	private int windowHeight = 600;
	private int windowWidth = 1470;
	int RIGHT_MARGIN = 30;
	int TOP_MARGIN = 30;
	int NAME_MARGIN = 6;
	int LEFT_MARGIN = 100;
	int instanceTime = 0;
	int ticksToShow = 50;

	private int actionIconsBoxWidth = 50;
	int hoveredTick = -1;

	List<ChartTick> selectedTicks = new ArrayList<>();
	boolean selectionDragActive = false;

	String hoveredPlayer = "";

	private List<SidebarButton> sidebarButtons = new ArrayList<>();
	int hoveredColumn = -1;


	// Configuration booleans for the new features
	private boolean showSpells = true;
	private boolean showSubIcons = true;
	private boolean showThrallBoxes = true;
	private boolean highlightIdleTicks = false;


	public int startTick;
	public int endTick;

	List<String> attackers = new ArrayList<>();

	String room;
	@Setter
	String roomSpecificText = "";
	private int fontHeight;
	public boolean finished = false;
	private boolean enforceCD = false;
	private final boolean live;
	private final List<ChartAuto> autos = new ArrayList<>();
	private Map<Integer, String> NPCMap = new HashMap<>();
	private final List<DawnSpec> dawnSpecs = new ArrayList<>();
	private final List<ThrallOutlineBox> thrallOutlineBoxes = new ArrayList<>();
	@Getter
	private final List<OutlineBox> outlineBoxes = new ArrayList<>();
	private final Map<Integer, String> specific = new HashMap<>();
	@Getter
	private final List<ChartLine> lines = new ArrayList<>();
	private final List<String> crabDescriptions = new ArrayList<>();

	@Setter
	private Map<Integer, Integer> roomHP = new HashMap<>();
	Map<String, Integer> playerOffsets = new LinkedHashMap<>();

	private PlayerAnimation selectedPrimary = PlayerAnimation.NOT_SET;
	private PlayerAnimation selectedSecondary = PlayerAnimation.NOT_SET;
	@Setter
	private NPCData target;

	private Rectangle sidebarToggleButtonBounds;
	private ConfigManager configManager;
	@Getter
	private boolean isActive = false;
	List<ChartListener> listeners = new ArrayList<>();

	public void addChartListener(ChartListener listener)
	{
		listeners.add(listener);
	}

	public void removeChartListener(ChartListener listener)
	{
		listeners.remove(listener);
	}

	public void postChartChange(ChartChangedEvent event)
	{
		for (ChartListener listener : listeners)
		{
			listener.onChartChanged(event);
		}
	}

	public void setActive(boolean state)
	{
		isActive = state;
		if (!isActive)
		{
			img = null;
			removeMouseListener(this);
			removeMouseMotionListener(this);
			removeMouseWheelListener(this);
		}
		else
		{
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
			createImage(true);
		}
	}

	private Map<PlayerAnimation, Integer> animationCounts = new HashMap<>();

	private void updatePlayerAnimationsInUse()
	{
		Map<PlayerAnimation, Integer> countsMap = new HashMap<>();
		synchronized (outlineBoxes)
		{
			for (OutlineBox box : outlineBoxes)
			{
				PlayerAnimation animation = box.playerAnimation;
				countsMap.put(animation, countsMap.getOrDefault(animation, 0) + 1);
			}
		}

		// Sort the animations by occurrence counts in descending order
		playerAnimationsInUse = countsMap.entrySet().stream()
			.sorted(Map.Entry.<PlayerAnimation, Integer>comparingByValue(Comparator.reverseOrder()))
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());

		// Update the animationCounts map
		animationCounts = countsMap;
	}

	int sidebarHeight;
	int availableHeight;

	private int iconsYStart;
	private int iconsAreaHeight;
	private int buttonsYStart;
	private int buttonsAreaHeight;

	private boolean isSidebarCollapsed = false;

	private void drawSidebar(Graphics2D g)
	{
		// Toggle button width
		int toggleButtonWidth = 25;

		// Calculate the sidebar's X position based on its state
		sidebarX = isSidebarCollapsed
			? getWidth() - RIGHT_MARGIN / 2 - toggleButtonWidth
			: getWidth() - sidebarWidth - RIGHT_MARGIN / 2;

		// Calculate the sidebar's Y position and total height
		sidebarY = 10; // Start a little below the top, adjust as needed
		sidebarHeight = getHeight() - sidebarY - 10; // Leave some space at the bottom

		// Calculate the height available for content inside the sidebar
		availableHeight = sidebarHeight - 2 * sidebarPadding;

		// Determine Y positions and heights for icons and buttons areas
		iconsYStart = sidebarY + sidebarPadding;
		iconsAreaHeight = (int) (availableHeight * 0.6); // 60% of available height for icons
		buttonsYStart = iconsYStart + iconsAreaHeight + sidebarPadding;
		buttonsAreaHeight = (int) (availableHeight * 0.4); // 40% for buttons

		// Draw the sidebar background only if not collapsed
		if (!isSidebarCollapsed)
		{
			// Draw the sidebar background
			g.setColor(config.primaryMiddle());
			g.fillRect(sidebarX, sidebarY, sidebarWidth, sidebarHeight);

			// Draw the sidebar border
			g.setColor(config.boxColor());
			g.drawRect(sidebarX, sidebarY, sidebarWidth, sidebarHeight);

			// Draw action icons and buttons
			drawActionIcons(g, sidebarX, iconsYStart, iconsAreaHeight);
			drawSidebarButtons(g, sidebarX, buttonsYStart, buttonsAreaHeight);
		}

		// Draw the toggle button
		drawSidebarToggleButton(g, sidebarX, toggleButtonWidth);
	}

	private void drawSidebarToggleButton(Graphics2D g, int sidebarX, int toggleButtonWidth)
	{
		int toggleButtonHeight = 20;
		int toggleButtonX = isSidebarCollapsed
			? getWidth() - RIGHT_MARGIN / 2 - toggleButtonWidth
			: sidebarX - toggleButtonWidth;
		int toggleButtonY = sidebarY; // Position it near the top of the sidebar

		// Update the bounds for click detection
		sidebarToggleButtonBounds = new Rectangle(toggleButtonX, toggleButtonY, toggleButtonWidth, toggleButtonHeight);

		// Draw the chevrons
		drawChevrons(g, toggleButtonX, toggleButtonY, toggleButtonWidth, toggleButtonHeight);
	}

	private void drawChevrons(Graphics2D g, int x, int y, int width, int height)
	{
		int chevronCount = 3;
		int chevronSize = Math.min(width, height) / 3; // Size of each chevron
		int chevronSpacing = chevronSize / 4; // Spacing between chevrons

		// Calculate total width of all chevrons and spacing
		int totalChevronsWidth = chevronCount * chevronSize + (chevronCount - 1) * chevronSpacing;

		// Starting X position to center chevrons horizontally
		int startX = x + (width - totalChevronsWidth) / 2;

		// Y position to center chevrons vertically
		int chevronY = y + (height - chevronSize) / 2;

		g.setColor(config.fontColor());
		g.setStroke(new BasicStroke(1)); // Set line thickness

		for (int i = 0; i < chevronCount; i++)
		{
			int chevronX = startX + i * (chevronSize + chevronSpacing);

			if (isSidebarCollapsed)
			{
				// Draw chevron pointing left (`<`)
				drawChevronLeft(g, chevronX, chevronY, chevronSize);
			}
			else
			{
				// Draw chevron pointing right (`>`)
				drawChevronRight(g, chevronX, chevronY, chevronSize);
			}
		}

		// Reset stroke to default if needed elsewhere
		g.setStroke(new BasicStroke());
	}

	private void drawChevronRight(Graphics2D g, int x, int y, int size)
	{
		int x1 = x;
		int y1 = y;
		int x2 = x + size / 2;
		int y2 = y + size / 2;
		int x3 = x;
		int y3 = y + size;

		// Draw two lines to form a chevron `>`
		g.drawLine(x1, y1, x2, y2);
		g.drawLine(x3, y3, x2, y2);
	}

	private void drawChevronLeft(Graphics2D g, int x, int y, int size)
	{
		int x1 = x + size / 2;
		int y1 = y;
		int x2 = x;
		int y2 = y + size / 2;
		int x3 = x + size / 2;
		int y3 = y + size;

		// Draw two lines to form a chevron `<`
		g.drawLine(x1, y1, x2, y2);
		g.drawLine(x3, y3, x2, y2);
	}

	private void drawActionIcons(Graphics2D g, int x, int yStart, int iconsAreaHeight)
	{
		actionIconPositions.clear();

		// Calculate number of icons per row based on sidebarWidth
		int iconsPerRow = (sidebarWidth - 2 * sidebarPadding + iconSpacing) / (iconSize + iconSpacing);
		if (iconsPerRow < 1)
		{
			iconsPerRow = 1;
		}

		// Now, calculate the number of rows that can fit in iconsAreaHeight
		int maxRows = (iconsAreaHeight + iconSpacing) / (iconSize + iconSpacing);
		if (maxRows < 1)
		{
			maxRows = 1;
		}

		int iconsNeeded = playerAnimationsInUse.size();
		int totalRows = (int) Math.ceil((double) iconsNeeded / iconsPerRow);

		// Adjust iconsPerRow if totalRows exceeds maxRows
		if (totalRows > maxRows)
		{
			totalRows = maxRows;
			iconsPerRow = (int) Math.ceil((double) iconsNeeded / totalRows);
		}

		int iconIndex = 0;

		// Define the expanded size for hover areas
		int hoverExpansion = iconSpacing / 2;

		for (int row = 0; row < totalRows; row++)
		{
			int iconY = yStart + row * (iconSize + iconSpacing);

			for (int col = 0; col < iconsPerRow; col++)
			{
				if (iconIndex >= playerAnimationsInUse.size())
				{
					break;
				}
				int iconX = x + sidebarPadding + col * (iconSize + iconSpacing);

				PlayerAnimation animation = playerAnimationsInUse.get(iconIndex);

				BufferedImage icon = iconMap.get(animation);
				if (icon != null)
				{
					BufferedImage scaledIcon = getScaledImage(icon, iconSize, iconSize);

					g.drawImage(scaledIcon, iconX, iconY, null);

					// Store the expanded position for hover detection
					// Adjust the rectangle to fill the gap spaces
					int hoverX = iconX - hoverExpansion;
					int hoverY = iconY - hoverExpansion;
					int hoverWidth = iconSize + iconSpacing;
					int hoverHeight = iconSize + iconSpacing;

					// Ensure the hover rectangle stays within the sidebar bounds
					hoverX = Math.max(hoverX, x + sidebarPadding);
					hoverY = Math.max(hoverY, yStart);
					if (hoverX + hoverWidth > x + sidebarWidth - sidebarPadding)
					{
						hoverWidth = x + sidebarWidth - sidebarPadding - hoverX;
					}
					if (hoverY + hoverHeight > yStart + iconsAreaHeight)
					{
						hoverHeight = yStart + iconsAreaHeight - hoverY;
					}

					Rectangle rect = new Rectangle(hoverX, hoverY, hoverWidth, hoverHeight);
					actionIconPositions.put(animation, rect);

					int count = animationCounts.getOrDefault(animation, 0);
					drawCountOnIcon(g, count, iconX, iconY, iconSize, iconSize);

					if (hoveredAction == animation)
					{
						g.setColor(config.markerColor()); // Choose a color for the hover box
						g.drawRect(iconX - 2, iconY - 2, iconSize + 4, iconSize + 4);
					}
				}

				iconIndex++;
			}
		}

		// Draw the summary box if hovering over an icon
		if (hoveredAction != null)
		{
			drawActionSummaryBox(g);
		}
	}

	private int minButtonWidth = 80;

	private void drawSidebarButtons(Graphics2D g, int x, int yStart, int buttonsAreaHeight)
	{
		int buttonsNeeded = sidebarButtons.size();

		// Calculate number of buttons per row based on sidebarWidth
		int buttonsPerRow = (sidebarWidth - 2 * sidebarPadding + buttonSpacing) / (minButtonWidth + buttonSpacing);
		if (buttonsPerRow < 1)
		{
			buttonsPerRow = 1;
		}

		// Now, calculate the number of rows that can fit in buttonsAreaHeight
		int maxRows = (buttonsAreaHeight + buttonSpacing) / (buttonHeight + buttonSpacing);
		if (maxRows < 1)
		{
			maxRows = 1;
		}

		int totalRows = (int) Math.ceil((double) buttonsNeeded / buttonsPerRow);

		// Adjust buttonsPerRow if totalRows exceeds maxRows
		if (totalRows > maxRows)
		{
			totalRows = maxRows;
			buttonsPerRow = (int) Math.ceil((double) buttonsNeeded / totalRows);
		}

		// Calculate button width to fit the number of buttons per row
		int buttonWidth = (sidebarWidth - (buttonsPerRow + 1) * buttonSpacing) / buttonsPerRow;

		int buttonIndex = 0;

		for (int row = 0; row < totalRows; row++)
		{
			int buttonY = yStart + row * (buttonHeight + buttonSpacing);

			for (int col = 0; col < buttonsPerRow; col++)
			{
				if (buttonIndex >= sidebarButtons.size())
				{
					break;
				}

				int buttonX = x + buttonSpacing + col * (buttonWidth + buttonSpacing);

				SidebarButton button = sidebarButtons.get(buttonIndex);
				button.setPosition(buttonX, buttonY, buttonWidth, buttonHeight);
				button.draw(g);

				buttonIndex++;
			}
		}
	}

	private void drawCountOnIcon(Graphics2D g, int count, int iconX, int iconY, int iconWidth, int iconHeight)
	{
		// Prepare the text to draw
		String countText = String.valueOf(count);

		// Set the font and color
		Font originalFont = g.getFont();
		Font countFont = originalFont.deriveFont(Font.BOLD, 12f); // Adjust font size as needed
		g.setFont(countFont);
		FontMetrics fm = g.getFontMetrics();


		int textX = iconX + 2; // Slight padding from the left edge
		int textY = iconY + iconHeight - 2; // Slight padding from the bottom edge


		// Draw the count text over the icon
		g.setColor(Color.WHITE);
		g.drawString(countText, textX, textY);

		// Reset the font and color
		g.setFont(originalFont);
		g.setColor(config.fontColor());
	}

	private void drawActionSummaryBox(Graphics2D g)
	{
		// Get the mouse position
		Rectangle iconRect = actionIconPositions.get(hoveredAction);

		if (iconRect == null)
		{
			return; // No icon position found for the hovered action
		}

		// Collect the counts per player
		Map<String, Integer> playerActionCounts = new HashMap<>();
		int totalCount = 0;
		synchronized (outlineBoxes)
		{
			for (OutlineBox box : outlineBoxes)
			{
				if (box.playerAnimation == hoveredAction)
				{
					int count = playerActionCounts.getOrDefault(box.player, 0) + 1;
					playerActionCounts.put(box.player, count);
					totalCount++;
				}
			}
		}

		// Prepare the summary text
		List<String> lines = new ArrayList<>();
		lines.add(hoveredAction.name + " (" + totalCount + ")");
		for (String player : playerOffsets.keySet())
		{
			int count = playerActionCounts.getOrDefault(player, 0);
			lines.add(player + ": " + count);
		}

		// Determine the size of the summary box
		FontMetrics fm = g.getFontMetrics();
		int boxWidth = 0;
		int boxHeight = 0;
		for (String line : lines)
		{
			int lineWidth = fm.stringWidth(line);
			boxWidth = Math.max(boxWidth, lineWidth);
			boxHeight += fm.getHeight();
		}
		boxWidth += 10; // Padding
		boxHeight += 10;

		// Decide where to position the summary box
		int summaryX;
		int summaryY = iconRect.y;

		// Check if there is enough space to the right of the cursor
		if (iconRect.x + boxWidth + 20 > getWidth())
		{
			// Not enough space to the right, position to the left
			summaryX = iconRect.x - boxWidth - 20;
		}
		else
		{
			// Position to the right of the cursor
			summaryX = iconRect.x + iconRect.width;
		}

		// Adjust summaryY if the box goes off the bottom edge
		if (summaryY + boxHeight > getHeight())
		{
			summaryY = getHeight() - boxHeight - 10;
		}

		// Draw the summary box background
		g.setColor(config.primaryDark()); // Semi-transparent black
		g.fillRoundRect(summaryX, summaryY, boxWidth, boxHeight, 5, 5);

		// Draw the border
		g.setColor(config.boxColor());
		g.drawRoundRect(summaryX, summaryY, boxWidth, boxHeight, 5, 5);

		g.setColor(config.fontColor());
		// Draw the text
		int textY = summaryY + fm.getAscent() + 5;
		for (String line : lines)
		{
			g.drawString(line, summaryX + 5, textY);
			textY += fm.getHeight();
		}
	}

	public boolean shouldDraw()
	{
		return !live || isActive;
	}

	private final ItemManager itemManager;
	private final SpriteManager spriteManager;

	public void enableWrap()
	{
		shouldWrap = true;
		recalculateSize();
	}

	private int boxesToShow = 1;

	public void setSize(int x, int y)
	{
		windowWidth = x;
		windowHeight = y;
		createImage(false);
	}

	public void setSize()
	{
		windowWidth = getWidth();
		windowHeight = getHeight();
		createImage(false);
	}

	public void createImage(boolean sendToBottom)
	{
		if (isActive || !live)
		{
			recalculateSize();
			boxesToShow = Math.min(1 + ((windowHeight - TITLE_BAR_PLUS_TAB_HEIGHT - scale) / boxHeight), boxCount);
			if (img != null)
			{
				img.flush();
			}
			img = new BufferedImage(windowWidth, windowHeight, BufferedImage.TYPE_INT_ARGB);
			if (sendToBottom)
			{
				sendToBottom();
			}
			drawGraph();
		}
	}

	public void setPrimaryTool(PlayerAnimation tool)
	{
		selectedPrimary = tool;
	}

	public void setSecondaryTool(PlayerAnimation tool)
	{
		selectedSecondary = tool;
	}

	private final AdvancedRaidTrackerConfig config;

	public void addRoomSpecificData(int tick, String data)
	{
		specific.put(tick, data);
		if (specific.size() == 1)
		{
			recalculateSize();
		}
	}

	public void addRoomSpecificDatum(Map<Integer, String> specificData)
	{
		specific.putAll(specificData);
		recalculateSize();
	}

	public void addLine(int tick, String lineInfo)
	{
		ChartLine line = new ChartLine(lineInfo, tick);
		lines.add(line);
		postChartChange(new ChartChangedEvent(ADD_ELEMENT, LINE, line));
		changesSaved = false;
	}

	public void addLines(Map<Integer, String> lineData)
	{
		for (Integer tick : lineData.keySet())
		{
			addLine(tick, lineData.get(tick));
		}
	}

	public void addLines(List<ChartLine> lines)
	{
		for (ChartLine line : lines)
		{
			addLine(line.tick, line.text);
		}
	}

	public void addRoomHP(int tick, int hp)
	{
		roomHP.put(tick, hp);
	}

	public void addAuto(int autoTick)
	{
		ChartAuto auto = new ChartAuto(autoTick);
		autos.add(auto);
		postChartChange(new ChartChangedEvent(ADD_ELEMENT, AUTO, auto));
		changesSaved = false;
	}

	public void addAutos(List<Integer> autos)
	{
		for (Integer auto : autos)
		{
			addAuto(auto);
		}
	}

	public void addAutosFromExisting(List<ChartAuto> autos)
	{
		for (ChartAuto auto : autos)
		{
			addAuto(auto.tick);
		}
	}

	private ThrallOutlineBox hoveredThrallBox;

	public void addThrallBox(ThrallOutlineBox thrallOutlineBox)
	{
		synchronized (thrallOutlineBoxes)
		{

			postChartChange(new ChartChangedEvent(ADD_ELEMENT, THRALL, thrallOutlineBox));
			thrallOutlineBoxes.add(thrallOutlineBox);
			if (room.equals("Creator"))
			{
				computeProbability2();
			}
		}
	}

	public void addThrallBoxes(List<ThrallOutlineBox> outlineBoxes)
	{
		for (ThrallOutlineBox box : outlineBoxes)
		{
			addThrallBox(box);
		}
	}

	public void addDawnSpec(DawnSpec dawnSpec)
	{
		synchronized (outlineBoxes)
		{
			for (OutlineBox outlineBox : outlineBoxes)
			{
				//based on projectile time dawn spec will always be applied between 2 and 4 ticks after the animation, and since there is only one
				//dawnbringer its impossible for the next spec to overlap this
				if ((dawnSpec.tick - outlineBox.tick <= 4 && dawnSpec.tick - outlineBox.tick >= 2) && (outlineBox.playerAnimation.equals(PlayerAnimation.DAWN_SPEC) || outlineBox.playerAnimation.equals(DAWN_AUTO)))
				{
					outlineBox.additionalText = String.valueOf(dawnSpec.getDamage());
				}
			}
		}
	}

	public void addDawnSpecs(List<DawnSpec> dawnSpecs)
	{
		for (DawnSpec dawnSpec : dawnSpecs)
		{
			addDawnSpec(dawnSpec);
		}
		drawGraph();
	}

	public void addNPC(int index, String name)
	{
		attackers.add(0, String.valueOf(index));
		redraw();
	}

	public void setRoomFinished(int tick)
	{
		finished = true;
		if (tick - endTick < 10) //todo what is the purpose?
		{
			endTick = tick;
		}
		drawGraph();
	}

	int baseStartTick = 0;
	int baseEndTick = 0;

	public void resetGraph()
	{
		playerWasOnCD.clear();
		currentBox = 0;
		currentScrollOffsetY = 0;
		currentScrollOffsetX = 0;
		endTick = 0;
		startTick = 0;
		baseEndTick = 0;
		baseStartTick = 0;
		hoveredColumn = -1;
		hoveredTick = -1;
		hoveredPlayer = "";
		synchronized (outlineBoxes)
		{
			outlineBoxes.clear();
		}
		autos.clear();
		lines.clear();
		specific.clear();
		dawnSpecs.clear();
		thrallOutlineBoxes.clear();
		attackers.clear();
		playerOffsets.clear();
		crabDescriptions.clear();
		roomHP.clear();
		NPCMap.clear();
		playerInThrownBloodList.clear();
		playerInSpawnedBloodList.clear();
		playerChancedDrainList.clear();
		playersHandedList.clear();
		playerDataManager.clear();
		finished = false;
		recalculateSize();
	}

	public void addMaidenCrabs(List<String> crabDescriptions)
	{
		this.crabDescriptions.addAll(crabDescriptions);
	}

	private List<StringInt> playerInThrownBloodList = new ArrayList<>();
	private List<StringInt> playerInSpawnedBloodList = new ArrayList<>();
	private List<StringInt> playerChancedDrainList = new ArrayList<>();
	private List<StringInt> playersHandedList = new ArrayList<>();

	private final PlayerDataManager playerDataManager = new PlayerDataManager();

	public void addPlayerDataChanged(PlayerDataChanged playerDataChanged)
	{
		playerDataManager.addPlayerDataChanged(playerDataChanged);
	}

	public void addPlayerDatumChanged(List<PlayerDataChanged> playerDatumChanged)
	{
		if (playerDatumChanged != null)
		{
			for (PlayerDataChanged playerDataChanged : playerDatumChanged)
			{
				playerDataManager.addPlayerDataChanged(playerDataChanged);
			}
		}
	}

	public void addPlayerStoodInSpawnedBlood(String player, int tick)
	{
		playerInSpawnedBloodList.add(new StringInt(player, tick));
	}

	public void addPlayerStoodInThrownBlood(String player, int tick)
	{
		playerInThrownBloodList.add(new StringInt(player, tick));
	}

	public void addPlayerChancedDrain(String player, int tick)
	{
		playerChancedDrainList.add(new StringInt(player, tick));
	}

	public void addPlayerHanded(String player, int tick)
	{
		playersHandedList.add(new StringInt(player, tick));
	}

	public void addPlayerStoodInThrownBloods(List<StringInt> playerInThrownBloodList)
	{
		this.playerInThrownBloodList = playerInThrownBloodList;
	}

	public void addPlayersHanded(List<StringInt> playersHandedList)
	{
		this.playersHandedList = playersHandedList;
	}

	public void addPlayerStoodInSpawnedBloods(List<StringInt> playerInSpawnedBloodList)
	{
		this.playerInSpawnedBloodList = playerInSpawnedBloodList;
	}

	public void addPlayerChancedDrains(List<StringInt> playerChancedDrainList)
	{
		this.playerChancedDrainList = playerChancedDrainList;
	}

	public void addMaidenCrab(String description)
	{
		crabDescriptions.add(description);
	}

	public void addNPCMapping(int index, String name)
	{
		NPCMap.put(index, name);
	}

	public void setNPCMappings(Map<Integer, String> mapping)
	{
		this.NPCMap = mapping;
	}

	public void redraw()
	{
		recalculateSize();
	}


	public void addLiveAttack(PlayerDidAttack attack)
	{
		if (room.equals("Maiden"))
		{
			attack.tick += 2; //I do not understand why this must be done but the attacks are on the wrong tick otherwise
		}
		attack.tick += baseEndTick;
		PlayerDidAttack shiftedAttack = new PlayerDidAttack(attack.itemManager, attack.player, attack.animation, attack.tick, attack.weapon, attack.projectile, attack.spotAnims, attack.targetedIndex, attack.targetedID, attack.targetName, attack.wornItems);
		OutlineBox box = ChartUtility.convertToOutlineBox(shiftedAttack, NOT_SET, this.room, roomHP, NPCMap);
		if(box != null)
		{
			SwingUtilities.invokeLater(() -> addAttack(box));
		}
	}

	public void addAttacks(Collection<PlayerDidAttack> attacks)
	{
		for (PlayerDidAttack attack : attacks)
		{
			OutlineBox box = ChartUtility.convertToOutlineBox(attack, NOT_SET, this.room, roomHP, NPCMap);
			if(box != null)
			{
				addAttack(box);
			}
		}
	}

	public void incrementTick()
	{
		endTick++;
		baseEndTick++;
		if (endTick % ticksToShow < 2 || endTick == 1)
		{
			recalculateSize();
			sendToBottom();
		}
		else
		{
			drawGraph();
		}
	}

	public void setEndTick(int tick)
	{
		endTick = tick;
		baseEndTick = tick;
		recalculateSize();
	}

	public void setStartTick(int tick)
	{
		startTick = tick;
		baseStartTick = tick;
		recalculateSize();
	}

	public void recalculateSize()
	{
		if (!shouldDraw())
		{
			return;
		}
		try
		{
			scale = config.chartScaleSize();
			setBackground(config.primaryDark());
		}
		catch (Exception ignored)
		{
		}

		int effectiveSidebarWidth = isSidebarCollapsed ? 0 : sidebarWidth;
		ticksToShow = ((windowWidth - LEFT_MARGIN - RIGHT_MARGIN - effectiveSidebarWidth) / scale) - 1;

		if (ticksToShow < 1)
		{
			ticksToShow = 1;
		}

		int length = endTick - startTick;
		boxCount = (length / ticksToShow);
		if (length % ticksToShow != 0)
		{
			boxCount++;
		}
		if (boxCount < 1)
		{
			boxCount = 1;
		}
		int additionalRows = 2 + getAdditionalRow();
		boxHeight = ((attackers.size() + additionalRows) * scale);
		boxWidth = LEFT_MARGIN + (scale * (ticksToShow + 1));
		boxesToShow = Math.min(1 + ((windowHeight - TITLE_BAR_PLUS_TAB_HEIGHT - scale) / boxHeight), boxCount);
		drawGraph();
	}

	public void sendToBottom()
	{
		if (TITLE_BAR_PLUS_TAB_HEIGHT + scale + boxCount * boxHeight > windowHeight)
		{
			currentBox = boxCount - 1 - boxesToShow + 1;
			int lastBoxEnd = (boxesToShow * boxHeight) + scale + TITLE_BAR_PLUS_TAB_HEIGHT;
			currentScrollOffsetY = (currentBox * boxHeight) + (lastBoxEnd - windowHeight);
		}
		drawGraph();
	}

	int draggedTextOffsetX = 0;
	int draggedTextOffsetY = 0;

	private final ClientThread clientThread;

	private static volatile boolean isShiftPressed = false;

	public static boolean isShiftPressed()
	{
		synchronized (ChartPanel.class)
		{
			return isShiftPressed;
		}
	}

	private static volatile boolean isCtrlPressed = false;

	public static boolean isCtrlPressed()
	{
		synchronized (ChartPanel.class)
		{
			return isCtrlPressed;
		}
	}

	public void release()
	{
		resetGraph();
		img = null;
		removeMouseListener(this);
		removeMouseWheelListener(this);
		removeMouseMotionListener(this);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
		removeFocusListener(this);
		for (PropertyChangeListener cl : getPropertyChangeListeners())
		{
			removePropertyChangeListener(cl);
		}
	}

	public void appendToSelected(char c)
	{
		for (ChartTick tick : selectedTicks)
		{
			synchronized (outlineBoxes)
			{
				for (OutlineBox box : outlineBoxes)
				{
					if (box.tick == tick.getTick() && Objects.equals(box.player, tick.getPlayer()))
					{
						box.additionalText += c;
					}
				}
			}
		}
		redraw();
	}

	public void removeLastCharFromSelected()
	{
		if (currentTool == SELECTION_TOOL)
		{
			for (ChartTick tick : selectedTicks)
			{
				synchronized (outlineBoxes)
				{
					for (OutlineBox box : outlineBoxes)
					{
						if (box.tick == tick.getTick() && Objects.equals(box.player, tick.getPlayer()))
						{
							if (!box.additionalText.isEmpty())
							{
								box.additionalText = box.additionalText.substring(0, box.additionalText.length() - 1);
							}
						}
					}
				}
			}
		}
		else if (currentTool == ADD_TEXT_TOOL)
		{
			if (currentlyBeingEdited != null)
			{
				String editedText = currentlyBeingEdited.text;
				if (!editedText.isEmpty())
				{
					currentlyBeingEdited.text = editedText.substring(0, editedText.length() - 1);
					changesSaved = false;
				}
			}
		}
		drawGraph();
	}

	public static boolean isEditingBoxText = false;
	private boolean showVengApplied = true;
	private static final Font BUTTON_FONT = new Font("Arial", Font.PLAIN, 12);
	private static final int BUTTON_VERTICAL_PADDING = 5; // Padding at top and bottom
	private static final int BUTTON_HORIZONTAL_PADDING = 10; // Padding on left and right within the button
	private static final int BUTTON_HALF_WIDTH_PADDING = 5; // Padding within left half

	private List<DinhsSpec> dinhsSpecs = new ArrayList<>();

	public void addDinhsSpec(DinhsSpec dinhsSpec)
	{
		dinhsSpecs.add(dinhsSpec);
	}

	public void addDinhsSpecs(List<DinhsSpec> dinhsSpecs)
	{
		this.dinhsSpecs = dinhsSpecs;
	}

	private List<StringInt> badChins = new ArrayList<>();

	public void addBadChin(String name, int i)
	{
		badChins.add(new StringInt(name, i));
	}

	public void addBadChins(List<StringInt> badChins)
	{
		this.badChins = badChins;
	}

	private class SidebarButton
	{
		String id;
		String label;
		boolean isChecked;
		boolean isHovered;
		boolean hoverEffectEnabled = true;
		Rectangle bounds;
		Runnable onClick;

		public SidebarButton(String id, String label, boolean isChecked, Runnable onClick)
		{
			this.id = id;
			this.label = label;
			this.isChecked = isChecked;
			this.onClick = onClick;
			this.bounds = new Rectangle();
		}

		public void setPosition(int x, int y, int width, int height)
		{
			bounds.setBounds(x, y, width, height);
		}

		public void draw(Graphics2D g)
		{
			// Draw background
			if (isHovered)
			{
				g.setColor(config.boxColor().brighter());
			}
			else
			{
				g.setColor(config.primaryDark());
			}
			g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);

			// Draw border
			g.setColor(config.boxColor());
			g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);

			// Split the bounds into left and right halves
			int halfWidth = (bounds.width - 2 * BUTTON_HORIZONTAL_PADDING) / 2;
			Rectangle leftHalf = new Rectangle(bounds.x + BUTTON_HORIZONTAL_PADDING, bounds.y, halfWidth, bounds.height);
			Rectangle rightHalf = new Rectangle(bounds.x + BUTTON_HORIZONTAL_PADDING + halfWidth, bounds.y, halfWidth, bounds.height);

			// Use consistent font
			Font originalFont = g.getFont();
			g.setFont(BUTTON_FONT);
			FontMetrics fm = g.getFontMetrics();

			// Split the label into words
			String[] words = label.split(" ");

			// Calculate positions for drawing
			int lineHeight = fm.getHeight();
			int totalTextHeight = lineHeight * words.length;
			int textY = leftHalf.y + (leftHalf.height - totalTextHeight) / 2 + fm.getAscent();

			// Draw each word on a new line
			g.setColor(config.fontColor());
			for (String word : words)
			{
				int wordWidth = fm.stringWidth(word);
				int textX = leftHalf.x + (leftHalf.width - wordWidth) / 2;
				g.drawString(word, textX, textY);
				textY += lineHeight;
			}

			// Draw the check mark on the right half if the button is checked
			if (isChecked)
			{
				int checkSize = Math.min(rightHalf.width, rightHalf.height) / 2;
				int checkX = 10 + rightHalf.x + (rightHalf.width - checkSize) / 2;
				int checkY = rightHalf.y + (rightHalf.height - checkSize) / 2;

				g.setColor(config.fontColor());
				g.setStroke(new BasicStroke(2));

				// Draw the check mark
				g.drawLine(checkX, checkY + checkSize / 2, checkX + checkSize / 2, checkY + checkSize);
				g.drawLine(checkX + checkSize / 2, checkY + checkSize, checkX + checkSize, checkY);
			}

			// Reset font
			g.setFont(originalFont);
		}

		public boolean contains(int x, int y)
		{
			return bounds.contains(x, y);
		}
	}

	private BufferedImage spawnedBlood;
	private BufferedImage thrownBlood;
	private BufferedImage drainSymbol;
	private BufferedImage hand;
	private BufferedImage xSymbol;
	private BufferedImage checkSymbol;

	public ChartPanel(String room, boolean isLive, AdvancedRaidTrackerConfig config, ClientThread clientThread, ConfigManager configManager, ItemManager itemManager, SpriteManager spriteManager)
	{
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
		this.configManager = configManager;
		this.config = config;
		this.clientThread = clientThread;
		OutlineBox.spriteManager = spriteManager;
		OutlineBox.itemManager = itemManager;
		OutlineBox.clientThread = clientThread;
		OutlineBox.useUnkitted = config.useUnkitted();
		setBackground(config.primaryDark());
		setOpaque(true);
		scale = 26;
		live = isLive;
		this.room = room;
		startTick = 0;
		endTick = 0;
		shouldWrap = true;
		boxWidth = LEFT_MARGIN + scale * (ticksToShow + 1);
		windowWidth = boxWidth + 10;
		windowHeight = 600;
		drainSymbol = ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/drain.png");
		spawnedBlood = ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/spawnedblood.png");
		thrownBlood = ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/thrownblood.png");
		hand = ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/hand.png");
		xSymbol = ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/x.png");
		checkSymbol = ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/check.png");


		if (!isLive)
		{
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
			addFocusListener(this);
			img = new BufferedImage(windowWidth, windowHeight, BufferedImage.TYPE_INT_ARGB);
		}

		clientThread.invoke(() ->
		{
			for (PlayerAnimation playerAnimation : PlayerAnimation.values())
			{
				if (playerAnimation.attackTicks > 0)
				{
					int weaponID = 0;
					if (playerAnimation.weaponIDs.length > 0)
					{
						weaponID = playerAnimation.weaponIDs[0];
					}
					if (config.useUnkitted())
					{
						weaponID = getReplacement(weaponID);
					}
					iconMap.put(playerAnimation, itemManager.getImage(weaponID, 1, false));
				}
				else
				{
					try
					{
						int animation = 0;
						if (playerAnimation.animations.length > 0)
						{
							animation = playerAnimation.animations[0];
						}
						iconMap.put(playerAnimation, spriteManager.getSprite(getSpellIcon(animation), 0));
					}
					catch (Exception e)
					{

					}
				}
			}
		});


		sidebarButtons.add(new SidebarButton("useIcons", "Use Icons", config.useIconsOnChart(), () -> {
			configManager.setConfiguration("Advanced Raid Tracker", "useIconsOnChart", !config.useIconsOnChart());
			SidebarButton button = getButtonById("useIcons");
			if (button != null)
			{
				button.isChecked = config.useIconsOnChart();
			}
			drawGraph();
		}));

		sidebarButtons.add(new SidebarButton("useTime", "Use Time", config.useTimeOnChart(), () -> {
			configManager.setConfiguration("Advanced Raid Tracker", "useTimeOnChart", !config.useTimeOnChart());
			SidebarButton button = getButtonById("useTime");
			if (button != null)
			{
				button.isChecked = config.useTimeOnChart();
			}
			drawGraph();
		}));

		sidebarButtons.add(new SidebarButton("showVengApplied", "Show Veng", showVengApplied, () -> {
			showVengApplied = !showVengApplied;
			SidebarButton button = getButtonById("showVengApplied");
			if (button != null)
			{
				button.isChecked = showVengApplied;
				button.hoverEffectEnabled = false;
			}
			drawGraph();
		}));

		sidebarButtons.add(new SidebarButton("showSpells", "Show Spells", showSpells, () -> {
			showSpells = !showSpells;
			SidebarButton button = getButtonById("showSpells");
			if (button != null)
			{
				button.isChecked = showSpells;
				button.hoverEffectEnabled = false;
			}
			drawGraph();
		}));

		sidebarButtons.add(new SidebarButton("showSecondaryIcons", "Show Subicon", showSubIcons, () -> {
			showSubIcons = !showSubIcons;
			SidebarButton button = getButtonById("showSecondaryIcons");
			if (button != null)
			{
				button.isChecked = showSubIcons;
				button.hoverEffectEnabled = false;
			}
			drawGraph();
		}));

		sidebarButtons.add(new SidebarButton("showThrallBoxes", "Show Thralls", showThrallBoxes, () -> {
			showThrallBoxes = !showThrallBoxes;
			SidebarButton button = getButtonById("showThrallBoxes");
			if (button != null)
			{
				button.isChecked = showThrallBoxes;
				button.hoverEffectEnabled = false;
			}
			drawGraph();
		}));

		sidebarButtons.add(new SidebarButton("showAutos", "Show Autos", showAutos, () -> {
			showAutos = !showAutos;
			SidebarButton button = getButtonById("showAutos");
			if (button != null)
			{
				button.isChecked = showAutos;
				button.hoverEffectEnabled = false;
			}
			drawGraph();
		}));

		sidebarButtons.add(new SidebarButton("highlightIdleTicks", "Show Idle", highlightIdleTicks, () -> {
			highlightIdleTicks = !highlightIdleTicks;
			SidebarButton button = getButtonById("highlightIdleTicks");
			if (button != null)
			{
				button.isChecked = highlightIdleTicks;
				button.hoverEffectEnabled = false;
			}
			drawGraph();
		}));


		setFocusable(true);
		requestFocus();
		recalculateSize();
		if(room.equals("Creator"))
		{
			NPCData npcData = DPSWindow.getNPCFromName("Verzik Vitur, Normal mode, Phase 1 (1040)");
			setTarget(npcData); //set default
		}
	}

	private SidebarButton getButtonById(String id)
	{
		for (SidebarButton button : sidebarButtons)
		{
			if (button.id.equals(id))
			{
				return button;
			}
		}
		return null;
	}

	public void openFile()
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(new File(PLUGIN_DIRECTORY + "/misc-dir/"));
		fileChooser.setFileFilter(new FileNameExtensionFilter("Chart Files", "json"));
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			File file = fileChooser.getSelectedFile();
			ChartIOData data = loadChartFromFile(file.getAbsolutePath());
			associatedFileName = file.getAbsolutePath();
			if (data != null)
			{
				applyFromSave(data);
			}
		}
	}

	public void saveFile()
	{
		if (associatedFileName.isEmpty())
		{
			JFileChooser saveFile = new JFileChooser();
			saveFile.setCurrentDirectory(new File(PLUGIN_DIRECTORY + "/misc-dir/"));
			saveFile.setFileFilter(new FileNameExtensionFilter("Chart Files", "json"));
			if (saveFile.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				File file = saveFile.getSelectedFile();
				associatedFileName = file.getAbsolutePath();
				saveChart(this, associatedFileName);
				changesSaved = true;
			}
		}
		else
		{
			saveChart(this, associatedFileName);
		}
	}

	public void exportImage()
	{

	}

	public void saveAs()
	{
		JFileChooser saveFile = new JFileChooser();
		saveFile.setCurrentDirectory(new File(PLUGIN_DIRECTORY + "/misc-dir/"));
		saveFile.setFileFilter(new FileNameExtensionFilter("Chart Files", "json"));
		if (saveFile.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			File file = saveFile.getSelectedFile();
			associatedFileName = file.getAbsolutePath();
			saveChart(this, associatedFileName);
			changesSaved = true;
		}
	}

	public void applyFromSave(ChartIOData data)
	{
		playerSBSCoolDown.clear();
		playerWasOnCD.clear();
		playerVengCoolDown.clear();
		playerThrallCoolDown.clear();
		playerDCCoolDown.clear();

		changesSaved = true;
		setStartTick(data.getStartTick());
		setEndTick(data.getEndTick());
		room = data.getRoomName();
		roomSpecificText = data.getRoomSpecificText();

		autos.clear();
		addAutosFromExisting(data.getAutos());

		specific.clear();
		addRoomSpecificDatum(data.getRoomSpecificTextMapping());

		lines.clear();
		addLines(data.getLines());

		outlineBoxes.clear();
		addAttacks(data.getOutlineBoxes());

		textBoxes.clear();
		addTextBoxes(data.getTextMapping());

		thrallOutlineBoxes.clear();
		addThrallBoxes(data.getThrallOutlineBoxes());

		recalculateSize();
	}

	private void addTextBoxes(List<ChartTextBox> textBoxes)
	{
		for (ChartTextBox textBox : textBoxes)
		{
			this.textBoxes.add(textBox);
			postChartChange(new ChartChangedEvent(ADD_ELEMENT, TEXT, textBox));
		}
	}

	public void newFile()
	{
		if (!changesSaved)
		{
			int result = JOptionPane.showConfirmDialog(this, "You have unsaved changes. Would you like to save?");
			if (result == JOptionPane.YES_OPTION)
			{
				saveFile();
			}
			else if (result == JOptionPane.CANCEL_OPTION)
			{
				return;
			}
		}
		applyFromSave(new ChartIOData(1, 50, "Creator", "", new ArrayList<>(), new HashMap<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "", new ArrayList<>()));
		changesSaved = true;
		associatedFileName = "";
	}

	public void addAttacks(List<OutlineBox> outlineBoxes)
	{
		for (OutlineBox box : outlineBoxes)
		{
			addAttack(box);
		}
	}

	private List<OutlineBox> getSelectedOutlineBoxes()
	{
		List<OutlineBox> selectedBoxes = new ArrayList<>();
		for (ChartTick tick : selectedTicks)
		{
			for (OutlineBox box : outlineBoxes)
			{
				if (box.tick == tick.getTick() && Objects.equals(box.player, tick.getPlayer()))
				{
					selectedBoxes.add(box);
					removeAttack(box);
				}
			}
		}
		return selectedBoxes;
	}

	private void processChartAction(ChartAction action)
	{
		for (OutlineBox box : action.getBoxes())
		{
			if (action.getActionType().equals(ADD_ELEMENT))
			{
				removeAttack(box);
			}
			else if (action.getActionType().equals(ChartActionType.REMOVE_ELEMENT))
			{
				addAttack(box);
			}
		}
	}

	@Override
	protected synchronized void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		if (img != null)
		{
			g.drawImage(img, 0, 0, null);
		}
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(windowWidth, windowHeight);
	}

	private Rectangle getStringBounds(Graphics2D g2, String str)
	{
		FontRenderContext frc = g2.getFontRenderContext();
		GlyphVector gv = g2.getFont().createGlyphVector(frc, str);
		return gv.getPixelBounds(null, (float) 0, (float) 0);
	}

	private int getStringWidth(Graphics2D g, String str)
	{
		FontRenderContext frc = g.getFontRenderContext();
		GlyphVector gv = g.getFont().createGlyphVector(frc, str);
		return gv.getPixelBounds(null, 0, 0).width;
	}

	int getYOffset(int tick)
	{
		return ((((tick - startTick) / ticksToShow) * boxHeight) + TOP_MARGIN) - (currentScrollOffsetY);
	}

	int getXOffset(int tick)
	{
		return LEFT_MARGIN + (((tick - startTick) % ticksToShow) * scale) - (currentScrollOffsetX % scale);
	}

	private void drawBoxStyleAccordingToConfig(Graphics2D g, int x, int y, int width, int height, int roundX, int roundY)
	{
		if (config.useRounded())
		{
			g.drawRoundRect(x, y, width, height, roundX, roundY);
		}
		else
		{
			g.drawRect(x - 1, y - 1, width + 3, height + 3);
		}
	}

	private void fillBoxStyleAccordingToConfig(Graphics2D g, int x, int y, int width, int height, int roundX, int roundY)
	{
		if (config.useRounded())
		{
			g.fillRoundRect(x, y, width, height, roundX, roundY);
		}
		else
		{
			g.fillRect(x - 1, y - 1, width + 3, height + 3);
		}
	}


	private void drawTicks(Graphics2D g)
	{
		if (room.equals("Nylocas")) //todo make generic, use existing methods
		{
			for (ChartLine line : lines)
			{
				if (line.text.equals("W1"))
				{
					instanceTime = line.tick % 4;
				}
			}
		}
		for (int i = startTick; i < endTick; i++)
		{
			if (shouldTickBeDrawn(i))
			{
				int xOffset = getXOffset(i);
				int yOffset = getYOffset(i) + scale;
				g.setColor(config.fontColor());
				Font oldFont = g.getFont();
				if (!config.useAlternateFont())
				{
					g.setFont(oldFont.deriveFont(10.0f));
				}
				int strWidth = getStringBounds(g, String.valueOf(i)).width;
				int stallsUntilThisPoint = 0;
				if (room.equals("Nylocas"))
				{
					for (ChartLine line : lines)
					{
						if (line.tick < (i - 3))
						{
							if (line.text.equals("Stall"))
							{
								stallsUntilThisPoint++;
							}
						}
					}
				}
				int ticks = i - (stallsUntilThisPoint * 4);
				boolean autosContains = autos.stream().anyMatch(a -> a.tick == ticks);
				boolean potentialAutosContains = potentialAutos.contains(ticks);
				if (autosContains && potentialAutosContains)
				{
					g.setColor(new Color(40, 140, 235));
				}
				else if (autosContains || potentialAutosContains)
				{
					g.setColor(Color.RED);
				}
				String tick = (config.useTimeOnChart()) ? RoomUtil.time(ticks) : String.valueOf(ticks);
				if (tick.endsWith("s")) //strip scuffed indicator from crab description because 5 letters is too many to draw
				{
					tick = tick.substring(0, tick.length() - 1);
				}
				int textPosition = (config.wrapAllBoxes()) ? xOffset + scale - strWidth - (scale / 4) : xOffset + (scale / 2) - (strWidth / 2);
				if (yOffset - (fontHeight / 2) > scale + 5 && xOffset > LEFT_MARGIN - 5)
				{
					if (config.useTimeOnChart())
					{
						drawStringRotated(g, tick, textPosition, yOffset - (fontHeight) - 5, config.fontColor());
					}
					else
					{
						g.drawString(tick, textPosition, yOffset - (fontHeight / 2));
					}
					if (config.wrapAllBoxes())
					{
						g.setColor(config.boxColor());
						g.drawRect(xOffset, yOffset - scale, scale, scale);
					}
				}
				g.setFont(oldFont);
			}
		}
	}

	List<Integer> potentialAutos = new ArrayList<>();

	private void drawAuto(Graphics2D g, int tick, int opacity)
	{
		g.setColor(new Color(255, 80, 80, opacity));
		int xOffset = getXOffset(tick);
		int yOffset = getYOffset(tick);
		g.fillRect(xOffset, yOffset + scale, scale, boxHeight - (scale * (2 + getAdditionalRow())));
	}

	private void drawAutos(Graphics2D g)
	{
		for (ChartAuto auto : autos)
		{
			if (shouldTickBeDrawn(auto.tick))
			{
				int opacity;
				if (showAutos)
				{
					opacity = 40; // Normal opacity when autos are shown
				}
				else if (isHoveredAndEffectEnabled("showAutos"))
				{
					opacity = 25; // 10% opacity when button is hovered
				}
				else
				{
					// Skip drawing autos when they are hidden and button is not hovered
					continue;
				}
				drawAuto(g, auto.tick, opacity);
			}
		}
	}

	private void drawPotentialAutos(Graphics2D g)
	{
		for (Integer i : potentialAutos)
		{
			if (shouldTickBeDrawn(i))
			{
				drawAuto(g, i, 20);
			}
		}
	}

	private void drawGraphBoxes(Graphics2D g)
	{
		for (int i = 0; i < boxesToShow; i++)
		{
			int startX = LEFT_MARGIN;
			int startY = boxHeight * i + TOP_MARGIN - (currentScrollOffsetY - (currentBox * boxHeight));
			int endX = boxWidth - scale;
			int endY = startY + boxHeight;
			g.setColor(config.boxColor());

			if (startY > 5)
			{
				g.drawLine(startX, startY + scale, endX, startY + scale);
			}
			g.drawLine(startX, (startY > 5) ? startY + scale : scale + 5, startX, endY - scale);
			if (endY - scale > 5 + scale)
			{
				g.drawLine(startX, endY - scale, endX, endY - scale);
			}
			g.drawLine(endX, endY - scale, endX, (startY > 5) ? startY + scale : scale + 5);
		}
	}


	private void setConfigFont(Graphics2D g)
	{
		if (config.useAlternateFont())
		{
			g.setFont(new Font("Arial", Font.PLAIN, 12));
		}
		else
		{
			g.setFont(FontManager.getRunescapeBoldFont());
		}
	}

	private void drawYChartColumn(Graphics2D g)
	{
		g.setColor(config.fontColor());
		for (int i = 0; i < attackers.size(); i++)
		{
			playerOffsets.put(attackers.get(i), i);
			for (int j = currentBox; j < currentBox + boxesToShow; j++)
			{
				g.setColor(config.primaryLight());
				int nameRectsY = (j * boxHeight) + ((i + 1) * scale) + TOP_MARGIN - currentScrollOffsetY;
				if (nameRectsY > scale + 5)
				{
					g.fillRoundRect(5, nameRectsY + (NAME_MARGIN / 2), 90, scale - NAME_MARGIN, 10, 10);

				}
				g.setColor(config.fontColor());
				Font oldFont = g.getFont();
				setConfigFont(g);
				int width = getStringWidth(g, attackers.get(i));
				int margin = 5;
				int subBoxWidth = LEFT_MARGIN - (margin * 2);
				int textPosition = margin + (subBoxWidth - width) / 2;
				int yPosition = ((j * boxHeight) + ((i + 1) * scale) + (fontHeight) / 2) + (scale / 2) + TOP_MARGIN - (currentScrollOffsetY);
				if (yPosition > scale + 5)
				{
					String attackerName = attackers.get(i);
					for (int r : NPCMap.keySet())
					{
						if (String.valueOf(r).equals(attackerName))
						{
							attackerName = NPCMap.get(r).split(" ")[0];
						}
					}
					if (config.chartTheme().equals(ChartTheme.EXCEL))
					{
						if (attackerName.startsWith("Player"))
						{
							attackerName = "P" + attackerName.substring(attackerName.length() - 1);
						}
						int strWidth = getStringWidth(g, attackerName);
						textPosition = LEFT_MARGIN - strWidth - (scale / 2) + 2;
						g.setColor(config.boxColor());
						g.drawRect(LEFT_MARGIN - (int) (scale * 1.5), yPosition - (fontHeight / 2) - (scale / 2), (int) (scale * 1.5), scale);
					}
					g.setColor(config.fontColor());
					if (lateDroppers.contains(attackerName))
					{
						g.setColor(Color.RED);
					}
					g.drawString(attackerName, textPosition, yPosition + margin);
				}

				if (i == 0)
				{
					if (room.equals("Nylocas"))
					{
						roomSpecificText = "Instance Time";
					}
					int textYPosition = getYOffset((j * ticksToShow) + 1) + ((playerOffsets.size() + 2) * scale) - (scale / 2) + (fontHeight / 2);
					if (textYPosition > scale + 5 && !specific.isEmpty() && textPosition > LEFT_MARGIN - 5)
					{
						g.drawString(roomSpecificText, 5, textYPosition);
					}
					if (config.showBoldTick())
					{
						int yPos = getYOffset(0);
						Font oldF = g.getFont();
						g.setFont(new Font("Arial", Font.BOLD, 12));
						g.setColor(config.fontColor());
						String tickString = "Tick";
						int stringWidth = getStringWidth(g, tickString);
						g.drawString(tickString, LEFT_MARGIN - stringWidth - 3, yPos + scale - (getStringHeight(g) / 2));
						g.setFont(oldF);
						g.setColor(config.boxColor());
						g.drawRect(LEFT_MARGIN - (int) (scale * 1.5), yPos, (int) (scale * 1.5), scale);
					}
				}
				g.setFont(oldFont);
			}
		}

	}

	private List<String> lateDroppers = new ArrayList<>();

	private void drawRoomSpecificData(Graphics2D g)
	{
		if (!specific.isEmpty() || room.equals("Nylocas")) //todo make generic
		{
			if (room.equals("Nylocas"))
			{
				for (int i = startTick; i < endTick; i++)
				{
					if (shouldTickBeDrawn(i))
					{
						int xOffset = getXOffset(i);
						int yOffset = getYOffset(i);
						yOffset += (playerOffsets.size() + 2) * scale;
						g.setColor(config.fontColor());
						String time = String.valueOf(((i + instanceTime) % 4) + 1);
						int strWidth = getStringBounds(g, time).width;
						if (yOffset - (scale / 2) > scale + 5 && xOffset > LEFT_MARGIN - 5)
						{
							g.drawString(time, xOffset + (scale / 2) - (strWidth / 2), yOffset - (scale / 2));
						}
					}
				}
			}
			else
			{
				lateDroppers.clear();
				for (Integer i : specific.keySet())
				{
					int xOffset = getXOffset(i);
					int yOffset = getYOffset(i);
					yOffset += (playerOffsets.size() + 2) * scale;
					g.setColor(config.fontColor());
					int strWidth = getStringBounds(g, specific.get(i)).width;
					if (yOffset > scale + 5 && xOffset > LEFT_MARGIN - 5)
					{
						if (room.contains("Verzik"))
						{
							boolean lateDrop = true;
							int lastSpecTick = 0;
							String lastSpecPlayer = "";
							for (OutlineBox box : outlineBoxes)
							{
								if (box.playerAnimation.equals(DAWN_SPEC) || box.playerAnimation.equals(DAWN_AUTO))
								{
									if (box.tick > lastSpecTick && box.tick < i)
									{
										lastSpecTick = box.tick;
										lastSpecPlayer = box.player;
									}
								}
								if (box.tick == i - 2)
								{
									if (box.playerAnimation.equals(DAWN_SPEC))
									{
										lateDrop = false;
									}
								}
							}
							if (lateDrop)
							{
								lateDroppers.add(lastSpecPlayer);
							}
							int sixth = scale / 8;
							int twoThird = scale * 3 / 4;
							g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
								RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
							g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
								RenderingHints.VALUE_INTERPOLATION_BILINEAR);
							g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
								RenderingHints.VALUE_ANTIALIAS_ON);
							if (lateDrop)
							{
								BufferedImage xSymbolScaled = getSmoothScaledIcon(xSymbol, twoThird, twoThird);
								g.drawImage(xSymbolScaled, xOffset + sixth, yOffset - scale + sixth, null);
								g.setColor(config.fontColor());
								int stringHeight = getStringHeight(g);
								g.drawString(lastSpecPlayer, xOffset + scale, yOffset - scale / 2 + stringHeight / 2);
							}
							else
							{
								BufferedImage checkSymbolScaled = getSmoothScaledIcon(checkSymbol, twoThird, twoThird);
								g.drawImage(checkSymbolScaled, xOffset + sixth, yOffset - scale + sixth, null);
							}
							g.setStroke(new BasicStroke(2));
							g.setColor(config.primaryDark());
							g.drawOval(xOffset + sixth, yOffset - scale + sixth, twoThird, twoThird);
						}
						else
						{
							g.drawString(specific.get(i), xOffset + (scale / 2) - (strWidth / 2), yOffset - (scale / 2) + (fontHeight / 2));
						}
					}
				}
			}
		}
	}

	private boolean showAutos = true;

	private void drawDawnSpecs(Graphics2D g)
	{
		for (DawnSpec dawnSpec : dawnSpecs)
		{
			String damage = String.valueOf(dawnSpec.getDamage());
			if (dawnSpec.getDamage() != -1)
			{
				int xOffset = getXOffset(dawnSpec.tick - 2);
				int yOffset = getYOffset(dawnSpec.tick);
				yOffset += (playerOffsets.size() + 1) * scale;
				g.setColor(config.fontColor());
				int textOffset = (scale / 2) - (getStringBounds(g, damage).width) / 2;
				if (yOffset > scale + 5 && xOffset + textOffset > LEFT_MARGIN - 5)
				{
					g.drawString(damage, xOffset + textOffset, yOffset + (scale / 2) - (fontHeight / 2));
				}
			}
		}
	}

	public static BufferedImage createDropShadow(BufferedImage image)
	{
		BufferedImage shadow = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2 = shadow.createGraphics();
		g2.drawImage(image, 0, 0, null);

		g2.setComposite(AlphaComposite.SrcIn);
		g2.setColor(new Color(0, 0, 0, 128));
		g2.fillRect(0, 0, shadow.getWidth(), shadow.getHeight());

		g2.dispose();
		return shadow;
	}

	private void drawPrimaryBoxes(Graphics2D g)
	{
		synchronized (outlineBoxes)
		{
			for (OutlineBox box : outlineBoxes)
			{
				if (shouldTickBeDrawn(box.tick))
				{
					int xOffset = getXOffset(box.tick);
					if (playerOffsets.get(box.player) == null)
					{
						continue;
					}
					int yOffset = ((playerOffsets.get(box.player) + 1) * scale) + getYOffset(box.tick);
					if (yOffset > scale + 5 && xOffset > LEFT_MARGIN - 5)
					{
						// Save the original composite
						Composite originalComposite = g.getComposite();

						// Adjust opacity if not the hovered action
						if (hoveredAction != null && box.playerAnimation != hoveredAction)
						{
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
						}

						// Check for Vengeance Applied
						boolean isVengApplied = (box.playerAnimation == PlayerAnimation.VENG_APPLIED);

						// Handle the visibility of Veng Applied indicators
						if (isVengApplied)
						{
							if (!showVengApplied)
							{
								g.setComposite(originalComposite);
								// Skip drawing Veng Applied boxes if not shown
								continue;
							}

							// Apply hover effect
							SidebarButton button = getButtonById("showVengApplied");
							if (button != null && button.isHovered && button.hoverEffectEnabled)
							{
								g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
							}
						}

						// Determine if this box is a spell or 0-tick action (excluding VENG_APPLIED)
						boolean isSpellOrZeroTick = box.playerAnimation.attackTicks < 1 && box.playerAnimation != PlayerAnimation.VENG_APPLIED;

						// Decide whether to draw this box
						boolean shouldDrawSpell = showSpells || isHoveredAndEffectEnabled("showSpells");

						if (!shouldDrawSpell && isSpellOrZeroTick)
						{
							g.setComposite(originalComposite);
							continue; // Skip drawing this box
						}

						if (config != null && config.useIconsOnChart())
						{
							try
							{
								if (box.playerAnimation.attackTicks != -1)
								{
									int opacity = config.iconBackgroundOpacity();
									opacity = Math.min(255, opacity);
									opacity = Math.max(0, opacity);
									if (box.playerAnimation.color.getAlpha() == 0)
									{
										opacity = 0;
									}
									if (config.attackBoxColor().equals(Color.WHITE))
									{
										g.setColor(new Color(box.color.getRed(), box.color.getGreen(), box.color.getBlue(), opacity));
									}
									else
									{
										g.setColor(config.attackBoxColor());
									}
									fillBoxStyleAccordingToConfig(g, xOffset + 2, yOffset + 2, scale - 3, scale - 3, 5, 5);
									BufferedImage icon = getIcon(box.playerAnimation, box.weapon);
									if (icon == null)
									{
										continue;
									}
									BufferedImage scaled = getScaledImage(icon, (scale - 2), (scale - 2));

									// Apply 30% opacity to spells when hovering over the "Show Spells" button
									Composite originalComposite2 = g.getComposite();
									if (getButtonById("showSpells").hoverEffectEnabled && getButtonById("showSpells").isHovered && isSpellOrZeroTick)
									{
										g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
									}

									if (box.playerAnimation.shouldFlip)
									{
										g.drawImage(createFlipped(createDropShadow(scaled)), xOffset + 3, yOffset + 3, null);
										g.drawImage(createFlipped(scaled), xOffset + 2, yOffset + 1, null);
									}
									else
									{
										g.drawImage(createDropShadow(scaled), xOffset + 3, yOffset + 3, null);
										g.drawImage(scaled, xOffset + 2, yOffset + 1, null);
									}

									// Restore the original composite
									g.setComposite(originalComposite2);

									// Draw secondary icons if enabled
									// Update for secondary icons
									if (showSubIcons && box.secondaryID != -2)
									{
										BufferedImage secondary = getSpellSpecificIcon(box.secondaryID);
										if (secondary != null && secondary != icon)
										{
											BufferedImage scaledSecondary = getScaledImage(secondary, scale / 2, scale / 2);
											// Apply temporary opacity if hovered
											Composite originalCompositeSec = g.getComposite();
											if (isHoveredAndEffectEnabled("showSubIcons"))
											{
												g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
											}
											g.drawImage(scaledSecondary, xOffset + (scale / 2), yOffset + (scale / 2), null);
											g.setComposite(originalCompositeSec);
										}
									}

// Update for tertiary icons
									if (showSubIcons && box.tertiaryID != -2)
									{
										BufferedImage tertiary = getSpellSpecificIcon(box.tertiaryID);
										if (tertiary != null)
										{
											BufferedImage scaledTertiary = getScaledImage(tertiary, scale / 2, scale / 2);
											// Apply temporary opacity if hovered
											Composite originalCompositeTert = g.getComposite();
											if (isHoveredAndEffectEnabled("showSubIcons"))
											{
												g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
											}
											g.drawImage(scaledTertiary, xOffset + (scale / 2), yOffset, null);
											g.setComposite(originalCompositeTert);
										}
									}
									if (!box.additionalText.isEmpty())
									{
										int textOffset;
										Font f = g.getFont();
										g.setFont(f.deriveFont(9.0f));
										textOffset = (scale / 2) - (getStringWidth(g, box.additionalText) / 2);
										g.setColor(config.fontColor());
										{
											int damage = 100;
											try
											{
												damage = Integer.parseInt(box.additionalText);
											}
											catch (Exception e)
											{

											}
											if (damage > 135)
											{
												g.setColor(Color.GREEN);
											}
										}
										g.drawString(box.additionalText, xOffset + textOffset, yOffset + scale - 3);
										g.setFont(f);
									}
									if (box.damage > -1)
									{
										Font f = g.getFont();
										g.setFont(f.deriveFont(9.0f));
										int textOffset = (scale) - (getStringWidth(g, String.valueOf(box.damage))) - 5;
										g.setColor(config.fontColor());
										g.drawString(String.valueOf(box.damage), xOffset + textOffset, yOffset + 3 + getStringHeight(g));
										g.setFont(f);
									}

								}
							}
							catch (Exception e)
							{
							}
						}
						else
						{
							int opacity = 100;
							if (config != null)
							{
								opacity = config.letterBackgroundOpacity();
								opacity = Math.min(255, opacity);
								opacity = Math.max(0, opacity);
							}
							g.setColor(new Color(box.color.getRed(), box.color.getGreen(), box.color.getBlue(), opacity));
							fillBoxStyleAccordingToConfig(g, xOffset + 2, yOffset + 2, scale - 3, scale - 3, 5, 5);
							g.setColor((box.primaryTarget) ? config.fontColor() : new Color(0, 190, 255));
							int textOffset = (scale / 2) - (getStringWidth(g, box.letter) / ((config.rightAlignTicks()) ? 4 : 2));
							int primaryOffset = yOffset + (box.additionalText.isEmpty() ? (fontHeight / 2) : 0);
							g.drawString(box.letter, xOffset + textOffset - 1, primaryOffset + (scale / 2) + 1);
							if (!box.additionalText.isEmpty())
							{
								Font f = g.getFont();
								g.setFont(f.deriveFont(10.0f));
								textOffset = (scale / 2) - (getStringWidth(g, box.additionalText) / 2);
								g.setColor(Color.WHITE);
								g.drawString(box.additionalText, xOffset + textOffset, yOffset + scale - 3);
								g.setFont(f);
							}
						}
						box.createOutline();
						g.setColor(box.outlineColor);
						drawBoxStyleAccordingToConfig(g, xOffset + 1, yOffset + 1, scale - 2, scale - 2, 5, 5);

						g.setComposite(originalComposite);
					}
				}
			}
		}
	}

	public boolean isHoveredAndEffectEnabled(String button)
	{
		SidebarButton button1 = getButtonById(button);
		if (button1 != null)
		{
			return button1.isHovered && button1.hoverEffectEnabled;
		}
		else
		{
			return false;
		}
	}

	private void drawDrainSymbols(Graphics2D g)
	{
		if (room.equals("Maiden"))
		{
			for (StringInt si : playerChancedDrainList)
			{
				if (shouldTickBeDrawn(si.val))
				{
					int xOffset = getXOffset(si.val);
					Integer playerOffset = playerOffsets.get(si.string);
					if (playerOffset != null)
					{
						int yOffset = ((playerOffset + 1) * scale) + getYOffset(si.val);
						if (yOffset > scale + 5 && xOffset > LEFT_MARGIN - 5)
						{
							BufferedImage scaledDrainSymbol = getScaledImage(drainSymbol, scale / 2, scale / 2);
							if (scaledDrainSymbol != null)
							{
								g.drawImage(scaledDrainSymbol, xOffset, yOffset, null);
							}
						}
					}
				}
			}
		}
	}

	private void drawHandSymbols(Graphics2D g)
	{
		if (room.equals("Bloat"))
		{
			for (StringInt si : playersHandedList)
			{
				if (shouldTickBeDrawn(si.val))
				{
					int xOffset = getXOffset(si.val);
					Integer playerOffset = playerOffsets.get(si.string);
					if (playerOffset != null)
					{
						int yOffset = ((playerOffset + 1) * scale) + getYOffset(si.val);
						if (yOffset > scale + 5 && xOffset > LEFT_MARGIN - 5)
						{
							BufferedImage handSymbol = getScaledImage(hand, scale / 2, scale / 2);
							if (handSymbol != null)
							{
								g.drawImage(handSymbol, xOffset, yOffset + (scale / 2), null);
							}
						}
					}
				}
			}
		}
	}

	private void drawBloodSymbols(Graphics2D g)
	{
		if (room.equals("Maiden"))
		{
			for (StringInt si : playerInThrownBloodList)
			{
				if (shouldTickBeDrawn(si.val))
				{
					int xOffset = getXOffset(si.val);
					Integer playerOffset = playerOffsets.get(si.string);
					if (playerOffset != null)
					{
						int yOffset = ((playerOffset + 1) * scale) + getYOffset(si.val);
						if (yOffset > scale + 5 && xOffset > LEFT_MARGIN - 5)
						{
							BufferedImage scaledBloodSymbol = getScaledImage(thrownBlood, scale / 2, scale / 2);
							if (scaledBloodSymbol != null)
							{
								g.drawImage(scaledBloodSymbol, xOffset, yOffset + (scale / 2), null);
							}
						}
					}
				}
			}
			for (StringInt si : playerInSpawnedBloodList)
			{
				if (shouldTickBeDrawn(si.val))
				{
					int xOffset = getXOffset(si.val);
					Integer playerOffset = playerOffsets.get(si.string);
					if (playerOffset != null)
					{
						int yOffset = ((playerOffset + 1) * scale) + getYOffset(si.val);
						if (yOffset > scale + 5 && xOffset > LEFT_MARGIN - 5)
						{
							BufferedImage scaledBloodSymbol = getScaledImage(spawnedBlood, scale / 2, scale / 2);
							if (scaledBloodSymbol != null)
							{
								g.drawImage(scaledBloodSymbol, xOffset, yOffset + (scale / 2), null);
							}
						}
					}
				}
			}
		}
	}


	private void drawMarkerLines(Graphics2D g)
	{
		if (!live || finished)
		{
			for (ChartLine line : lines)
			{
				if (shouldTickBeDrawn(line.tick))
				{
					int xOffset = getXOffset(line.tick);
					int yOffset = getYOffset(line.tick);
					g.setColor(config.markerColor());
					if (currentTool == ADD_LINE_TOOL && hoveredTick == line.tick)
					{
						g.setColor(new Color(40, 140, 235));
					}
					g.drawLine(xOffset, yOffset + (scale / 2), xOffset, yOffset + boxHeight - scale);
					int stringLength = getStringBounds(g, line.text).width;
					g.setColor(config.fontColor());
					if (yOffset + (scale / 2) > scale + 5 && xOffset - (stringLength / 2) > LEFT_MARGIN - 5)
					{
						g.drawString(line.text, xOffset - (stringLength / 2), yOffset + (scale / 2)); //todo
					}
				}
			}
		}
	}

	private boolean hoveredThrallIntersectsExisting()
	{
		if (hoveredThrallBox == null)
		{
			return false;
		}
		synchronized (thrallOutlineBoxes)
		{
			for (ThrallOutlineBox box : thrallOutlineBoxes)
			{
				if (box.owner.equals(hoveredThrallBox.owner) && box.spawnTick == hoveredThrallBox.spawnTick)
				{
					return true;
				}
			}
		}
		return false;
	}

	private void drawThrallBoxes(Graphics2D g)
	{
		boolean shouldDrawThralls = showThrallBoxes || isHoveredAndEffectEnabled("showThrallBoxes");

		if (shouldDrawThralls)
		{
			if (hoveredThrallBox != null)
			{
				int opacity = (hoveredThrallIntersectsExisting()) ? 60 : 10;
				drawThrallBox(g, hoveredThrallBox, opacity);
			}
			synchronized (thrallOutlineBoxes)
			{
				for (ThrallOutlineBox box : thrallOutlineBoxes)
				{
					int opacity;

					if (isHoveredAndEffectEnabled("showThrallBoxes"))
					{
						opacity = 25;
					}
					else if (showThrallBoxes)
					{
						opacity = 15;
					}
					else
					{
						continue;
					}

					drawThrallBox(g, box, opacity);
				}
			}
		}
	}


	private void drawThrallBox(Graphics2D g, ThrallOutlineBox box, int opacity)
	{
		g.setColor(new Color(box.getColor().getRed(), box.getColor().getGreen(), box.getColor().getBlue(), opacity));

		int maxTick = getMaxTick(box.owner, box.spawnTick);
		int lastEndTick = box.spawnTick;
		while (lastEndTick < maxTick && shouldTickBeDrawn(lastEndTick))
		{
			int yOffset = getYOffset(lastEndTick);
			try
			{
				yOffset += (playerOffsets.get(box.owner) + 1) * scale;
			}
			catch (Exception e)
			{
				break;
			}
			int currentEndTick = (shouldWrap) ? lastEndTick + (ticksToShow - (lastEndTick % ticksToShow) + (startTick % ticksToShow)) : maxTick;
			if (currentEndTick > maxTick)
			{
				currentEndTick = maxTick;
			}
			int xOffsetStart = getXOffset(lastEndTick);
			int xOffsetEnd = getXOffset(currentEndTick - 1);
			lastEndTick = currentEndTick;
			if (yOffset > scale + 5 && xOffsetStart > 100)
			{
				g.fillRect(xOffsetStart, yOffset + 1, xOffsetEnd - xOffsetStart + scale, scale - 2);
			}
		}
	}

	/**
	 * Finds the highest tick that doesn't overlap if they summoned a thrall in the future before this one would naturally expire
	 *
	 * @param owner     player who summoned this thrall
	 * @param startTick tick the thrall was summoned
	 * @return
	 */
	private int getMaxTick(String owner, int startTick)
	{
		int maxTick = startTick + 99;
		synchronized (thrallOutlineBoxes)
		{
			for (ThrallOutlineBox boxCompare : thrallOutlineBoxes)
			{
				if (owner.equalsIgnoreCase(boxCompare.owner))
				{
					if (boxCompare.spawnTick > startTick && boxCompare.spawnTick < (startTick + 99))
					{
						maxTick = boxCompare.spawnTick;
					}
				}
			}
		}
		if (endTick < maxTick)
		{
			maxTick = endTick;
		}
		return maxTick;
	}

	private void drawSelectedOutlineBox(Graphics2D g)
	{
		if (hoveredTick != -1 && !hoveredPlayer.equalsIgnoreCase(""))
		{
			g.setColor(config.fontColor());
			if (enforceCD)
			{
				if (playerWasOnCD.containsEntry(hoveredPlayer, hoveredTick))
				{
					g.setColor(Color.RED);
				}
			}
			int xOffset = getXOffset(hoveredTick);
			int yOffset = ((playerOffsets.get(hoveredPlayer) + 1) * scale) + getYOffset(hoveredTick);
			if (yOffset > scale + 5 && xOffset > LEFT_MARGIN - 5)
			{
				g.drawRect(xOffset, yOffset, scale, scale);
			}
		}
	}

	private Point getPoint(int tick, String player)
	{
		return new Point(getXOffset(tick), ((playerOffsets.get(player) + 1) * scale) + getYOffset(tick));
	}

	private int getAdditionalRow()
	{
		return (!specific.isEmpty() || room.equals("Nylocas")) ? 1 : 0;
	}

	private void drawSelectedRow(Graphics2D g)
	{
		if (hoveredColumn != -1 && shouldTickBeDrawn(hoveredColumn))
		{
			g.setColor(config.fontColor());
			int xOffset = getXOffset(hoveredColumn);
			int yOffset = getYOffset(hoveredColumn);
			int additionalRows = 1 + getAdditionalRow();
			g.drawRect(xOffset, yOffset, scale, scale * (attackers.size() + additionalRows));

			int selectedTickHP = -1;
			try
			{
				selectedTickHP = roomHP.getOrDefault(hoveredColumn + 1, -1);
			}
			catch (Exception ignored)
			{
			}
			int offset = -1;
			switch (room) //todo map?
			{
				case "Maiden":
				case "Verzik P3":
					offset = 7;
					break;
				case "Bloat":
				case "Sotetseg":
				case "Xarpus":
					offset = 3;
					break;
				case "Nylocas":
					offset = 5;
					offset += (4 - ((offset + hoveredColumn) % 4));
					offset -= 2;
					break;
			}
			String bossWouldHaveDied = (offset != -1) ? "Melee attack on this tick killing would result in: " + RoomUtil.time(hoveredColumn + 1 + offset + 1) + " (Quick death: " + RoomUtil.time(hoveredColumn + offset + 1) + ")" : "";
			String HPString = "Boss HP: " + ((selectedTickHP == -1) ? "-" : RoomUtil.varbitHPtoReadable(selectedTickHP));
			HoverBox hoverBox = new HoverBox(HPString, config);
			if (offset != -1)
			{
				hoverBox.addString(bossWouldHaveDied);
			}
			int xPosition = xOffset + scale;
			if (xPosition + hoverBox.getWidth(g) > windowWidth)
			{
				xPosition = xPosition - hoverBox.getWidth(g) - 3 * scale;
			}
			int yPosition = yOffset;
			if (yPosition + hoverBox.getHeight(g) > windowHeight)
			{
				yPosition = yPosition - hoverBox.getHeight(g) - 3 * scale;
			}
			hoverBox.setPosition(xPosition, yPosition);
			hoverBox.draw(g);
		}
	}

	public void chartSelectionChanged(List<OutlineBox> boxes)
	{
		List<ChartTick> newSelection = new ArrayList<>();
		for (OutlineBox box : boxes)
		{
			newSelection.add(new ChartTick(box.tick, box.player));
		}
		selectedTicks = newSelection;
		drawGraph();
	}

	private void drawHoverBox(Graphics2D g)
	{
		if(isMousePressed)
		{
			return;
		}
		synchronized (outlineBoxes)
		{
			for (OutlineBox action : outlineBoxes)
			{
				if (action.tick == hoveredTick && action.player.equals(hoveredPlayer) && shouldTickBeDrawn(action.tick))
				{
					Point location = getPoint(action.tick, action.player);
					HoverBox hoverBox = new HoverBox(action.getTooltip(), config);
					if (action.cd > 0)
					{
						hoverBox.addString("");
						for (String item : action.getWornItemNames())
						{
							hoverBox.addString("." + item);
						}
					}
					int xPosition = location.getX() + 10;
					if (xPosition + hoverBox.getWidth(g) > windowWidth - RIGHT_MARGIN - sidebarWidth) //box would render off screen
					{
						xPosition = xPosition - hoverBox.getWidth(g) - scale * 2; //render to the left side of the selected action
					}
					int yPosition = location.getY() - 10;
					if (yPosition + hoverBox.getHeight(g) > (windowHeight - 2 * TITLE_BAR_PLUS_TAB_HEIGHT)) //box would render off screen
					{
						yPosition -= (yPosition + hoverBox.getHeight(g) - (windowHeight - TITLE_BAR_PLUS_TAB_HEIGHT - 10)); //render bottom aligned to window+10
					}
					hoverBox.setPosition(xPosition, yPosition);
					hoverBox.draw(g);
				}
			}
		}
	}

	private void drawMaidenCrabs(Graphics2D g)
	{
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setColor(new Color(230, 20, 20, 200));
		if (room.equals("Maiden"))
		{
			for (ChartLine line : lines)
			{
				if (line.text.contains("Dead"))
				{
					continue;
				}
				String proc = line.text.split(" ")[0];
				int xOffset = getXOffset(line.tick + 1);
				int yOffset = 10 + getYOffset(line.tick + 1);
				if (yOffset <= scale + 5)
				{
					continue;
				}
				yOffset -= scale;
				int crabOffsetX = 0;
				int crabOffsetY;

				crabOffsetY = 11;
				g.drawOval(xOffset + crabOffsetX, yOffset + crabOffsetY, 7, 7);

				crabOffsetY = 20;
				g.drawOval(xOffset + crabOffsetX, yOffset + crabOffsetY, 7, 7);

				crabOffsetX = 9;
				crabOffsetY = 11;
				g.drawOval(xOffset + crabOffsetX, yOffset + crabOffsetY, 7, 7);

				crabOffsetX = 9;
				crabOffsetY = 20;
				g.drawOval(xOffset + crabOffsetX, yOffset + crabOffsetY, 7, 7);

				crabOffsetX = 18;
				crabOffsetY = 11;
				g.drawOval(xOffset + crabOffsetX, yOffset + crabOffsetY, 7, 7);

				crabOffsetX = 18;
				crabOffsetY = 20;
				g.drawOval(xOffset + crabOffsetX, yOffset + crabOffsetY, 7, 7);

				crabOffsetX = 27;
				crabOffsetY = 2;
				g.drawOval(xOffset + crabOffsetX, yOffset + crabOffsetY, 7, 7);

				crabOffsetX = 27;
				crabOffsetY = 20;
				g.drawOval(xOffset + crabOffsetX, yOffset + crabOffsetY, 7, 7);

				crabOffsetX = 27;
				crabOffsetY = 11;
				g.drawOval(xOffset + crabOffsetX, yOffset + crabOffsetY, 7, 7);

				crabOffsetX = 27;
				crabOffsetY = 29;
				g.drawOval(xOffset + crabOffsetX, yOffset + crabOffsetY, 7, 7);


				for (String crab : crabDescriptions)
				{
					if (crab.contains(proc))
					{
						xOffset = getXOffset(line.tick + 1);
						yOffset = 10 + getYOffset(line.tick + 1);
						crabOffsetX = 0;
						crabOffsetY = 0;
						if (crab.contains("N1"))
						{
							crabOffsetY = 11;
						}
						else if (crab.contains("S1"))
						{
							crabOffsetY = 20;
						}
						else if (crab.contains("N2"))
						{
							crabOffsetX = 9;
							crabOffsetY = 11;
						}
						else if (crab.contains("S2"))
						{
							crabOffsetX = 9;
							crabOffsetY = 20;
						}
						else if (crab.contains("N3"))
						{
							crabOffsetX = 18;
							crabOffsetY = 11;
						}
						else if (crab.contains("S3"))
						{
							crabOffsetX = 18;
							crabOffsetY = 20;
						}
						else if (crab.contains("N4 (1)"))
						{
							crabOffsetX = 27;
							crabOffsetY = 2;
						}
						else if (crab.contains("S4 (1)"))
						{
							crabOffsetX = 27;
							crabOffsetY = 20;
						}
						else if (crab.contains("N4 (2)"))
						{
							crabOffsetX = 27;
							crabOffsetY = 11;
						}
						else if (crab.contains("S4 (2)"))
						{
							crabOffsetX = 27;
							crabOffsetY = 29;
						}
						crabOffsetY -= scale;
						if (crab.startsWith("s"))
						{
							g.setColor(new Color(220, 200, 0, 200));
						}
						else
						{
							g.setColor(new Color(230, 20, 20, 200));
						}
						g.fillOval(xOffset + crabOffsetX, yOffset + crabOffsetY, 7, 7);
						g.setColor(new Color(230, 20, 20, 200));
					}
				}
			}
		}
	}

	private void drawRoomTime(Graphics2D g)
	{
		Font oldFont = g.getFont();
		g.setColor(config.fontColor());
		setConfigFont(g);
		g.drawString("Time " + RoomUtil.time(endTick - startTick), 5, 20);
		g.setFont(oldFont);
	}

	private void drawBaseBoxes(Graphics2D g)
	{
		for (int i = startTick; i < endTick; i++)
		{
			if (shouldTickBeDrawn(i))
			{
				for (int j = 0; j < playerOffsets.size(); j++)
				{
					String player = attackers.get(j);
					int xOffset = getXOffset(i);
					if (playerOffsets.get(player) == null)
					{
						continue;
					}
					shouldWrap = true;
					int yOffset = ((playerOffsets.get(player) + 1) * scale) + getYOffset(i);

					// Determine if the current tick is an idle tick (no attack)
					boolean isIdleTick = !playerWasOnCD.get(player).contains(i);

					// Set the color based on whether the tick is idle and the highlightIdleTicks toggle or hover
					if (isIdleTick)
					{
						if (highlightIdleTicks)
						{
							// If the feature is enabled, draw idle ticks in solid red
							g.setColor(Color.RED);
						}
						else if (isHoveredAndEffectEnabled("highlightIdleTicks"))
						{
							// If hovering over the button, draw idle ticks in red with 30% opacity
							g.setColor(new Color(255, 0, 0, 77)); // 30% opacity red
						}
						else
						{
							// Otherwise, use the idle color from the config
							g.setColor(config.idleColor());
						}
					}
					else
					{
						// For non-idle ticks (ticks with attacks), use the primary middle color
						g.setColor(config.primaryMiddle());
					}

					if (yOffset > scale + 5 && xOffset > LEFT_MARGIN - 5)
					{
						fillBoxStyleAccordingToConfig(g, xOffset + 2, yOffset + 2, scale - 3, scale - 3, 5, 5);
					}
				}
			}
		}
	}

	@Setter
	private String manualLineText = "";

	private final Multimap<String, Integer> playerWasOnCD = ArrayListMultimap.create();

	public boolean shouldTickBeDrawn(int tick) //is tick visible, > start tick, < end tick
	{
		return tick >= (startTick + currentBox * ticksToShow) && tick < (startTick + ((currentBox + boxesToShow) * ticksToShow)) && tick >= startTick && tick <= endTick;
	}

	private void drawLinePlacement(Graphics2D g) //chart creator
	{
		if (shouldTickBeDrawn(hoveredTick) && lines.stream().noneMatch(o -> o.tick == hoveredTick))
		{
			int xOffset = getXOffset(hoveredTick);
			int yOffset = getYOffset(hoveredTick);
			g.setColor(config.markerColor());
			g.drawLine(xOffset, yOffset + (scale / 2), xOffset, yOffset + boxHeight - scale);
		}
	}

	private void drawSelectedTicks(Graphics2D g)
	{
		for (ChartTick tick : selectedTicks)
		{
			if (playerOffsets.containsKey(tick.getPlayer()))
			{
				int xOffset = getXOffset(tick.getTick());
				int yOffset = getYOffset(tick.getTick());
				yOffset += (playerOffsets.get(tick.getPlayer()) + 1) * scale;
				if (isEditingBoxText)
				{
					g.setColor(config.markerColor());
				}
				else
				{
					g.setColor(getTransparentColor(config.fontColor(), 128));
				}
				g.drawRect(xOffset, yOffset, scale, scale);
			}
		}
	}

	private void drawSelectionRegion(Graphics2D g)
	{
		if (selectionDragActive && playerOffsets.containsKey(activeDragPlayer) && playerOffsets.containsKey(dragStartPlayer))
		{
			int beginTick = Math.min(dragStartTick, activeDragTick);
			int stopTick = Math.max(dragStartTick, activeDragTick);
			int xStart = getXOffset(beginTick);
			int xEnd = getXOffset(stopTick) + scale;
			int lowOffset = Math.min(playerOffsets.get(activeDragPlayer), playerOffsets.get(dragStartPlayer));
			int highOffset = Math.max(playerOffsets.get(activeDragPlayer), playerOffsets.get(dragStartPlayer));
			int yStart = getYOffset(beginTick) + scale + (lowOffset * scale);
			int yEnd = getYOffset(stopTick) + scale + scale + (highOffset * scale);
			g.setColor(getTransparentColor(config.primaryDark(), 180));
			g.fillRect(xStart, yStart, xEnd - xStart, yEnd - yStart);
			g.setColor(config.fontColor());
			g.drawRect(xStart, yStart, xEnd - xStart, yEnd - yStart);
		}
	}

	private long lastRefresh = 0;

	private Timer redrawTimer;

	private void scheduleRedraw()
	{
		if (redrawTimer == null || !redrawTimer.isRunning())
		{
			long delay = (20 * 1000000 - (System.nanoTime() - lastRefresh)) / 1000000;
			if (delay < 0)
			{
				delay = 0;
			}
			redrawTimer = new Timer((int) delay, e ->
			{
				drawGraph();
				redrawTimer.stop();
			});
			redrawTimer.setRepeats(false);
			redrawTimer.start();
		}
		else if (redrawTimer.isRunning())
		{
			redrawTimer.restart();
		}
	}

	private synchronized void drawGraph()
	{
		if (!shouldDraw() || img == null)
		{
			return;
		}
		if (System.nanoTime() - lastRefresh < 20 * 1000000) //don't redraw more than once every 20ms
		{
			scheduleRedraw();
			return;
		}
		lastRefresh = System.nanoTime();
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		RenderingHints qualityHints = new RenderingHints(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		qualityHints.put(
			RenderingHints.KEY_RENDERING,
			RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHints(qualityHints);
		Color oldColor = g.getColor();

		g.setColor(config.primaryDark());
		g.fillRect(0, 0, img.getWidth(), img.getHeight());

		fontHeight = getStringBounds(g, "a").height; //todo is "a" really the best option here?

		int chartWidth = img.getWidth() - actionIconsBoxWidth;

		drawTicks(g);
		drawGraphBoxes(g);
		drawBaseBoxes(g);
		drawYChartColumn(g);
		drawRoomSpecificData(g);
		drawDawnSpecs(g);
		drawThrallBoxes(g);
		drawPrimaryBoxes(g);
		if (isExtendingAttack)
		{
			drawPreviewBoxes(g);
		}
		drawDrainSymbols(g);
		drawBloodSymbols(g);
		drawHandSymbols(g);
		drawBadChins(g);
		drawDinhsSpecs(g);
		drawAutos(g);
		drawPotentialAutos(g);
		drawMarkerLines(g);
		drawMaidenCrabs(g);
		drawPotionPrayerMarkers(g);
		drawProbabilities(g);

		if (currentTool != ADD_LINE_TOOL && currentTool != ADD_TEXT_TOOL)
		{
			drawSelectedOutlineBox(g);
			drawSelectedRow(g);
		}

		drawHoverBox(g);
		drawRoomTime(g);

		if (currentTool == ADD_LINE_TOOL)
		{
			drawLinePlacement(g);
		}

		drawSelectedTicks(g);
		drawSelectionRegion(g);

		drawMappedText(g);
		if (currentTool == ADD_TEXT_TOOL)
		{
			drawCurrentlyEditedTextBox(g);
			drawAlignmentMarkers(g);
		}

		drawSidebar(g);

		updateStatus();
		g.setColor(oldColor);
		g.dispose();
		repaint();
	}

	private void drawPreviewBoxes(Graphics2D g)
	{
		if (extensionPreviewTicks != null && !extensionPreviewTicks.isEmpty())
		{
			for (int tick : extensionPreviewTicks)
			{
				if (shouldTickBeDrawn(tick))
				{
					int xOffset = getXOffset(tick);
					int yOffset = ((playerOffsets.get(extensionOriginPlayer) + 1) * scale) + getYOffset(tick);

					if (yOffset > scale + 5 && xOffset > LEFT_MARGIN - 5)
					{
						// 50% opacity
						Composite originalComposite = g.getComposite();
						g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

						// Draw the box
						if (config != null && config.useIconsOnChart())
						{
							try
							{
								BufferedImage icon = getIcon(extensionOriginBox.playerAnimation, extensionOriginBox.weapon);
								if (icon != null)
								{
									BufferedImage scaled = getScaledImage(icon, (scale - 2), (scale - 2));
									if (extensionOriginBox.playerAnimation.shouldFlip)
									{
										g.drawImage(createFlipped(scaled), xOffset + 2, yOffset + 1, null);
									}
									else
									{
										g.drawImage(scaled, xOffset + 2, yOffset + 1, null);
									}
								}
							}
							catch (Exception exception)
							{
								// Handle exception
							}
						}
						else
						{
							g.setColor(extensionOriginBox.color);
							fillBoxStyleAccordingToConfig(g, xOffset + 2, yOffset + 2, scale - 3, scale - 3, 5, 5);
							g.setColor(extensionOriginBox.outlineColor);
							drawBoxStyleAccordingToConfig(g, xOffset + 1, yOffset + 1, scale - 2, scale - 2, 5, 5);
							int textOffset = (scale / 2) - (getStringWidth(g, extensionOriginBox.letter) / ((config.rightAlignTicks()) ? 4 : 2));
							int primaryOffset = yOffset + (extensionOriginBox.additionalText.isEmpty() ? (fontHeight / 2) : 0);
							g.drawString(extensionOriginBox.letter, xOffset + textOffset - 1, primaryOffset + (scale / 2) + 1);
						}

						// Restore original composite
						g.setComposite(originalComposite);
					}
				}
			}
		}
	}

	private boolean isMousePressed = false;

	private void drawProbabilities(Graphics2D g)
	{
		if (!room.equals("Creator") || target == null)
		{
			return;
		}
		for (int i = startTick; i < endTick; i++)
		{
			if (shouldTickBeDrawn(i))
			{
				Font oldFont = g.getFont();
				int xOffset = getXOffset(i);
				int yOffset = getYOffset(i);

				g.setFont(oldFont.deriveFont(10.0f));
				if (probabilityMap.get(i) != null)
				{
					double p = probabilityMap.get(i);
					// Ensure p is within the range [0.0, 1.0]
					p = Math.max(0.0, Math.min(1.0, p));

					// Interpolate between red and green
					int redComponent = (int) ((1 - p) * 255);
					int greenComponent = (int) (p * 255);
					int blueComponent = 0;
					int alpha = (int) (0.4 * 255); // 40% opacity

					Color interpolatedColor = new Color(redComponent, greenComponent, blueComponent, alpha);
					g.setColor(interpolatedColor);

					g.fillRoundRect(xOffset + 2, yOffset, scale - 4, scale / 2, 5, 5);

					g.setColor(config.fontColor());
					String probabilityString = String.format("%.1f", probabilityMap.get(i) * 100);

					int fontHeight = getStringHeight(g);
					int fontWidth = getStringWidth(g, probabilityString);

					g.drawString(probabilityString, xOffset + (scale / 2) - (fontWidth / 2), yOffset + (scale / 2) - (fontHeight / 2));

				}
				g.setFont(oldFont);
			}
		}
	}


	private void drawPotionPrayerMarkers(Graphics2D g)
	{
		for (OutlineBox box : outlineBoxes)
		{
			if (shouldTickBeDrawn(box.tick) && box.playerAnimation.attackTicks > 1)
			{
				PlayerData playerData = playerDataManager.getPlayerData(box.player, box.tick);
				Boolean isPietyActive = playerData.getPrayers().get(Prayer.PIETY);
				if (playerData.getAttackLevel() != 118 || playerData.getStrengthLevel() != 118 || Boolean.FALSE.equals(isPietyActive))
				{
					if (playerData.getStrengthLevel() != -1 && playerData.getAttackLevel() != -1)
					{
						if (box.playerAnimation.isMelee())
						{
							int xOffset = getXOffset(box.tick);
							Integer playerOffset = playerOffsets.get(box.player);
							if (playerOffset != null)
							{
								int yOffset = ((playerOffset + 1) * scale) + getYOffset(box.tick);
								if (yOffset > scale + 5 && xOffset > LEFT_MARGIN - 5)
								{
									BufferedImage xSymbolScaled = getScaledImage(xSymbol, scale / 2, scale / 2);

									g.setColor(config.primaryDark());
									g.fillOval(xOffset, yOffset, scale / 2, scale / 2);
									if (xSymbolScaled != null)
									{
										g.drawImage(xSymbolScaled, xOffset, yOffset, null);
									}
									g.setStroke(new BasicStroke(2));
									g.drawOval(xOffset, yOffset, scale / 2, scale / 2);

								}
							}
						}
					}
				}
			}
		}
	}

	private void drawBadChins(Graphics2D g)
	{
		if (room.equals("Maiden"))
		{
			for (StringInt si : badChins)
			{
				if (shouldTickBeDrawn(si.val))
				{
					int xOffset = getXOffset(si.val);
					Integer playerOffset = playerOffsets.get(si.string);
					if (playerOffset != null)
					{
						int yOffset = ((playerOffset + 1) * scale) + getYOffset(si.val);
						if (yOffset > scale + 5 && xOffset > LEFT_MARGIN - 5)
						{
							BufferedImage xSymbolScaled = getScaledImage(xSymbol, scale / 2, scale / 2);

							g.setColor(config.primaryDark());
							g.fillOval(xOffset, yOffset, scale / 2, scale / 2);
							if (xSymbolScaled != null)
							{
								g.drawImage(xSymbolScaled, xOffset, yOffset, null);
							}
							g.setStroke(new BasicStroke(2));
							g.drawOval(xOffset, yOffset, scale / 2, scale / 2);
						}
					}
				}
			}
		}
	}

	private void drawDinhsSpecs(Graphics2D g)
	{
		for (DinhsSpec dinhsSpec : dinhsSpecs)
		{
			for (OutlineBox box : outlineBoxes)
			{
				if (dinhsSpec.getPlayer().equals(box.player) && dinhsSpec.getTick() == box.tick)
				{
					if (shouldTickBeDrawn(dinhsSpec.getTick()))
					{
						int xOffset = getXOffset(dinhsSpec.getTick());
						Integer playerOffset = playerOffsets.get(dinhsSpec.getPlayer());
						if (playerOffset != null)
						{
							int yOffset = ((playerOffset + 1) * scale) + getYOffset(dinhsSpec.getTick());
							if (yOffset > scale + 5 && xOffset > LEFT_MARGIN - 5)
							{
								g.setColor(config.fontColor());
								g.drawString(String.valueOf(dinhsSpec.getTargets()), xOffset, yOffset + scale / 2);
							}
						}
					}
				}
			}
		}
	}

	private void drawAlignmentMarkers(Graphics2D g)
	{
		if (isDragging && currentTool == ADD_TEXT_TOOL && currentlyBeingEdited == null)
		{
			g.setColor(config.markerColor());
			for (Line l : alignmentMarkers)
			{
				g.drawLine(l.getP1().getX(), l.getP1().getY(), l.getP2().getX(), l.getP2().getY());
			}
		}
	}


	private int draggedTextPosX = 0;
	private int draggedTextPosY = 0;
	private boolean isDraggingText = false;

	private void drawMappedText(Graphics2D g)
	{
		g.setColor(config.fontColor());
		FontMetrics fm = g.getFontMetrics();

		// Reduce line spacing between lines
		int lineHeight = fm.getAscent() + 2; // Adjust as needed for desired spacing

		for (ChartTextBox p : textBoxes)
		{
			int xPos, yPos;
			if (currentlyBeingHovered != null && p == currentlyBeingHovered && isDragging)
			{
				xPos = draggedTextPosX - currentScrollOffsetX;
				yPos = draggedTextPosY - currentScrollOffsetY;
			}
			else
			{
				xPos = p.getPoint().getX() - currentScrollOffsetX;
				yPos = p.getPoint().getY() - currentScrollOffsetY;
			}

			String[] lines = p.text.split("\n", -1);
			int yOffset = yPos;

			for (String line : lines)
			{
				// Draw each line
				g.drawString(line, xPos, yOffset);
				yOffset += lineHeight; // Advance yOffset by lineHeight
			}
		}
	}


	private Rectangle getTextBoxBounds(Graphics2D g, ChartTextBox textBox, int xOffset, int yOffset)
	{
		FontMetrics fm = g.getFontMetrics();

		// Use the same lineHeight as in drawMappedText
		int lineHeight = fm.getAscent() + 2;
		String[] lines = textBox.text.split("\n", -1);
		int totalHeight = lineHeight * lines.length;

		// Compute maximum width
		int maxWidth = 0;
		for (String line : lines)
		{
			int lineWidth = fm.stringWidth(line);
			maxWidth = Math.max(maxWidth, lineWidth);
		}

		int x = textBox.getPoint().getX() + xOffset;
		int y = textBox.getPoint().getY() + yOffset - fm.getAscent();

		return new Rectangle(x, y, maxWidth, totalHeight);
	}


	private void drawCurrentlyEditedTextBox(Graphics2D g)
	{
		if (currentlyBeingEdited != null)
		{
			g.setColor(new Color(40, 140, 235));
			Rectangle textBounds = getTextBoxBounds(g, currentlyBeingEdited, -currentScrollOffsetX, -currentScrollOffsetY);
			int padding = 5;
			g.drawRect(textBounds.x - padding, textBounds.y - padding, textBounds.width + 2 * padding, textBounds.height + 2 * padding);
		}
		else if (currentlyBeingHovered != null && !isDragging)
		{
			g.setColor(config.boxColor());
			Rectangle textBounds = getTextBoxBounds(g, currentlyBeingHovered, -currentScrollOffsetX, -currentScrollOffsetY);
			int padding = 5;
			g.drawRect(textBounds.x - padding, textBounds.y - padding, textBounds.width + 2 * padding, textBounds.height + 2 * padding);
		}
	}


	public void updateStatus()
	{
		switch (currentTool)
		{
			case ADD_ATTACK_TOOL:
				setStatus("Left Click: " + selectedPrimary.name + ", Right Click: " + selectedSecondary.name);
				break;
			case ADD_AUTO_TOOL:
				setStatus("Adding " + potentialAutos.size() + " autos. Spacing: " + autoScrollAmount);
				break;
			case ADD_LINE_TOOL:
				setStatus("Placing Lines");
				break;
			case ADD_TEXT_TOOL:
				setStatus("Setting Text");
				break;
			case SELECTION_TOOL:
				setStatus(selectedTicks.size() + " ticks selected. ");
				if (isEditingBoxText)
				{
					appendStatus("Editing Box Text");
				}
				break;
		}
	}

	public void setAttackers(List<String> attackers)
	{
		this.attackers.clear();
		this.attackers.addAll(attackers); //don't do this.attackers = because the reference changes on plugin end
		playerOffsets.clear();
		recalculateSize();
	}

	private long lastHoverRefresh = 0;

	public void setTickHovered(int x, int y)
	{
		if (System.nanoTime() - lastHoverRefresh < 20 * 1000000) //don't recalculate hover more than once per 20ms
		{
			return;
		}
		lastHoverRefresh = System.nanoTime();
		if (boxHeight > 0 && scale > 0)
		{
			y = y + currentScrollOffsetY;
			x = x + (currentScrollOffsetX % scale);
			if (y > 20) //todo why do I use 20 here when TOP_MARGIN is 30?
			{
				int boxNumber = (y - 20) / boxHeight;
				if (x > LEFT_MARGIN - 5 && x < windowWidth - sidebarWidth - RIGHT_MARGIN)
				{
					int tick = startTick + (ticksToShow * boxNumber + ((x - LEFT_MARGIN) / scale));
					int playerOffsetPosition = (((y - TOP_MARGIN - scale) % boxHeight) / scale);
					if (playerOffsetPosition >= 0 && playerOffsetPosition < attackers.size() && (y - TOP_MARGIN - scale > 0))
					{
						hoveredTick = tick;
						hoveredPlayer = attackers.get(playerOffsetPosition);
						hoveredColumn = -1;
					}
					else if (y % boxHeight < TOP_MARGIN + scale)
					{
						hoveredColumn = tick;
						hoveredPlayer = "";
						hoveredTick = -1;
					}
					else
					{
						hoveredPlayer = "";
						hoveredTick = -1;
						hoveredColumn = -1;
					}
					currentlyBeingHovered = getNearByText(new Point(x, y));
				}
				else
				{
					hoveredPlayer = "";
					hoveredTick = -1;
					hoveredColumn = -1;
					currentlyBeingHovered = null;
				}
				if (currentTool == ADD_THRALL_TOOL)
				{
					if (!hoveredPlayer.isEmpty() && hoveredTick != -1)
					{
						hoveredThrallBox = new ThrallOutlineBox(hoveredPlayer, hoveredTick, MAGE_THRALL);
					}
					else
					{
						hoveredThrallBox = null;
					}
				}
				else
				{
					hoveredThrallBox = null;
				}
				drawGraph();
			}
		}
	}

	private int currentBox = 0;
	private int currentScrollOffsetY = 0;
	private int currentScrollOffsetX = 0;

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) //implement scrolling
	{
		if (!isCtrlPressed())
		{
			if (e.getWheelRotation() < 0) //top of the first box aligns to top if you scroll up
			{
				currentBox = Math.max(0, currentBox - 1);
				currentScrollOffsetY = currentBox * boxHeight;
			}
			else //bottom of the bottom box aligns to the bottom if you scroll down
			{
				if (TITLE_BAR_PLUS_TAB_HEIGHT + scale + boxCount * boxHeight > windowHeight) //no need to scroll at all if all boxes fit on screen, boxes would jump to bottom and leave dead space
				{
					int lastBox = currentBox + boxesToShow - 1;
					lastBox = Math.min(lastBox + 1, boxCount - 1);
					currentBox = lastBox - boxesToShow + 1;
					int lastBoxEnd = (boxesToShow * boxHeight) + scale + TITLE_BAR_PLUS_TAB_HEIGHT;
					currentScrollOffsetY = (currentBox * boxHeight) + (lastBoxEnd - windowHeight);
				}
			}
		}
		else //ctrl + scroll behavior for adding autos to add them every x ticks
		{
			if (e.getWheelRotation() < 0)
			{
				if (currentTool == ADD_AUTO_TOOL)
				{
					autoScrollAmount = Math.max(0, --autoScrollAmount);
					setPotentialAutos();
				}
				else
				{
					if (startTick > 0)
					{
						startTick--;
						endTick--;
					}
				}
			}
			else
			{
				if (currentTool == ADD_AUTO_TOOL)
				{
					autoScrollAmount++;
					setPotentialAutos();
				}
				else
				{
					startTick++;
					endTick++;
				}
			}
		}
		recalculateSize();
	}

	private int autoScrollAmount;

	@Override
	public void mouseClicked(MouseEvent e)
	{
			checkRelease(e, false);
	}

	private boolean isExtendingAttack = false;
	private int extensionOriginTick = -1;
	private String extensionOriginPlayer = "";
	private int extensionCD = 0;
	private OutlineBox extensionOriginBox = null;
	private List<Integer> extensionPreviewTicks = new ArrayList<>();
	private boolean isPossibleExtension = false;
	private OutlineBox possibleExtensionBox = null;

	@Override
	public void mousePressed(MouseEvent e)
	{
		if (currentTool == ADD_ATTACK_TOOL && (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)))
		{
			if (hoveredTick != -1 && !hoveredPlayer.isEmpty())
			{
				OutlineBox existingBox = null;
				synchronized (outlineBoxes)
				{
					for (OutlineBox box : outlineBoxes)
					{
						if (box.tick == hoveredTick && box.player.equals(hoveredPlayer))
						{
							existingBox = box;
							break;
						}
					}
				}

				if (existingBox != null)
				{
					// Possible start of drag operation
					possibleExtensionBox = existingBox;
					isPossibleExtension = true;
					//dragButton = e.getButton();
					isMousePressed = true; // For suppressing hover box
					dragStartX = e.getX();
					dragStartY = e.getY();
				}
				else
				{
					isPossibleExtension = false;
					possibleExtensionBox = null;
					isMousePressed = true; // For suppressing hover box
				}
			}
		}
		else if (SwingUtilities.isMiddleMouseButton(e))
		{
			if (!isDragging)
			{
				currentScrollOffsetX = 0;
				currentScrollOffsetY = 0;
				startTick = baseStartTick;
				endTick = baseEndTick;
				redraw();
			}
		}
	}





	private boolean isDragging = false;

	@Override
	public void mouseReleased(MouseEvent e)
	{
		if (isExtendingAttack)
		{
			if (!extensionPreviewTicks.isEmpty())
			{
				// Add the OutlineBoxes at the extensionPreviewTicks
				PlayerAnimation selectedAnimation = extensionOriginBox.playerAnimation; // Use the same animation as the origin box

				if (selectedAnimation != null && selectedAnimation != PlayerAnimation.NOT_SET)
				{
					for (int tick : extensionPreviewTicks)
					{
						// Remove existing box at this tick/player
						removeAttack(tick, extensionOriginPlayer, false);

						// Create new OutlineBox based on selected animation
						int weaponId = extensionOriginBox.weapon;

						OutlineBox newBox = new OutlineBox(selectedAnimation.shorthand, selectedAnimation.color, true, extensionOriginBox.additionalText, selectedAnimation, selectedAnimation.attackTicks, tick, extensionOriginPlayer, RaidRoom.getRoom(this.room), weaponId);
						newBox.setPreset(extensionOriginBox.getPreset());
						newBox.setWornItems(extensionOriginBox.getWornItems());
						newBox.setWornItemNames(extensionOriginBox.getWornItemNames());
						newBox.setTooltip(extensionOriginBox.getTooltip());

						addAttack(newBox);
					}
				}
			}
			// Reset variables
			isExtendingAttack = false;
			extensionPreviewTicks.clear();
			drawGraph();
		}
		else
		{
			// Handle other mouse release actions
			checkRelease(e, true);
		}
		isPossibleExtension = false;
		possibleExtensionBox = null;
		draggedTextOffsetX = 0;
		draggedTextOffsetY = 0;
		isDragging = false;
		isMousePressed = false; // Mouse is no longer pressed
	}




	private final Stack<ChartAction> actionHistory = new Stack<>();

	private void reverseAction(ChartAction action)
	{

	}

	private void checkRelease(MouseEvent e, boolean wasDragging)
	{
		if (wasDragging)
		{
			if (currentlyBeingHovered != null && isDraggingText)
			{
				currentlyBeingHovered.point = new Point(draggedTextPosX + alignmentOffsetX, draggedTextPosY + alignmentOffsetY);
			}
			alignmentOffsetX = 0;
			alignmentOffsetY = 0;
			isDragging = false;
			isDraggingText = false;
			drawGraph();
			return;
		}

		if (sidebarToggleButtonBounds != null && sidebarToggleButtonBounds.contains(e.getPoint()))
		{
			isSidebarCollapsed = !isSidebarCollapsed;
			recalculateSize();
			return;
		}
		if (SwingUtilities.isLeftMouseButton(e))
		{
			for (SidebarButton button : sidebarButtons)
			{
				if (button.contains(e.getX(), e.getY()))
				{
					button.isChecked = !button.isChecked;
					button.hoverEffectEnabled = false;
					if (button.onClick != null)
					{
						button.onClick.run(); // Execute the button's action
					}
					drawGraph();
					return; // Exit as we've handled the click
				}
			}
		}

		// Handle action icons click if needed (optional)
		if (hoveredAction != null && actionIconPositions.get(hoveredAction).contains(e.getPoint()))
		{
			// Handle action icon click if needed
		}
		{
			switch (currentTool)
			{
				case ADD_LINE_TOOL:
					if (hoveredTick != -1)
					{
						if (SwingUtilities.isLeftMouseButton(e))
						{
							addLine(hoveredTick, manualLineText);
						}
						else if (SwingUtilities.isRightMouseButton(e))
						{
							List<ChartLine> removedLines = lines.stream().filter(o -> o.tick == hoveredTick).collect(Collectors.toList());
							removedLines.forEach(l -> postChartChange(new ChartChangedEvent(REMOVE_ELEMENT, LINE, l)));
							lines.removeAll(removedLines);
						}
					}
					break;
				case SELECTION_TOOL:
					if (SwingUtilities.isLeftMouseButton(e))
					{
						if (isCtrlPressed())
						{
							ChartTick tick = new ChartTick(hoveredTick, hoveredPlayer);
							if (!selectedTicks.contains(tick))
							{
								selectedTicks.add(tick);
							}
						}
						else
						{
							selectedTicks.clear();
							selectedTicks.add(new ChartTick(hoveredTick, hoveredPlayer));
						}
					}
					else if (SwingUtilities.isRightMouseButton(e))
					{

					}
					break;
				case ADD_ATTACK_TOOL:
					if (SwingUtilities.isLeftMouseButton(e))
					{
						if (hoveredTick != -1 && !selectedPrimary.equals(PlayerAnimation.NOT_SET))
						{
							int weapon = 0;
							if (selectedPrimary.weaponIDs.length > 0)
							{
								weapon = selectedPrimary.weaponIDs[0];
							}
							String worn = "";
							if (ChartCreatorFrame.selectedPrimaryPreset != null)
							{
								Map<String, EquipmentData> equipment = ChartCreatorFrame.selectedPrimaryPreset.getEquipment();

								List<String> slots = Arrays.asList("head", "cape", "neck", "weapon", "body", "shield", "legs", "hands", "feet");

								List<String> ids = slots.stream()
									.map(slot -> {
										EquipmentData item = equipment.get(slot);
										return (item != null) ? String.valueOf(item.getId()) : "";
									})
									.collect(Collectors.toList());

								worn = String.join("~", ids);
							}
							PlayerDidAttack attack = new PlayerDidAttack(itemManager, hoveredPlayer, String.valueOf(selectedPrimary.animations[0]), hoveredTick, weapon, "", "", 0, 0, "", worn);
							attack.setPreset(ChartCreatorFrame.selectedPrimaryPreset);
							OutlineBox box = ChartUtility.convertToOutlineBox(attack, selectedPrimary, this.room, roomHP, NPCMap);
							if (box != null)
							{
								addAttack(box);
							}

						}
						else if (selectedPrimary.equals(PlayerAnimation.NOT_SET))
						{
							removeAttack(hoveredTick, hoveredPlayer, true);
						}
						if (hoveredTick != -1)
						{
							if (isDragging)
							{
								if (dragStartTick > 0 && activeDragTick > 0 && !dragStartPlayer.isEmpty() && !activeDragPlayer.isEmpty())
								{
									selectionDragActive = false;
								}
							}
						}
					}
					else if (SwingUtilities.isRightMouseButton(e))
					{
						if (hoveredTick != -1 && !selectedSecondary.equals(PlayerAnimation.NOT_SET))
						{
							int weapon = 0;
							if (selectedSecondary.weaponIDs.length > 0)
							{
								weapon = selectedSecondary.weaponIDs[0];
							}
							PlayerDidAttack attack = (new PlayerDidAttack(itemManager, hoveredPlayer, String.valueOf(selectedPrimary.animations[0]), hoveredTick, weapon, "", "", 0, 0, "", ""));
							OutlineBox box = ChartUtility.convertToOutlineBox(attack, selectedSecondary, this.room, roomHP, NPCMap);
							if(box != null)
							{
								addAttack(box);
							}
						}
						else if (selectedSecondary.equals(PlayerAnimation.NOT_SET))
						{
							removeAttack(hoveredTick, hoveredPlayer, true);
						}
					}
					break;
				case ADD_AUTO_TOOL:
					if (SwingUtilities.isLeftMouseButton(e))
					{
						for (Integer potential : potentialAutos)
						{
							if (autos.stream().noneMatch(a -> a.tick == potential))
							{
								addAuto(potential);
							}
						}
						potentialAutos.clear();
					}
					else if (SwingUtilities.isRightMouseButton(e))
					{
						List<ChartAuto> toRemove = autos.stream().filter(a -> potentialAutos.contains(a.tick)).collect(Collectors.toList());
						toRemove.forEach(ca -> postChartChange(new ChartChangedEvent(REMOVE_ELEMENT, AUTO, ca)));
						autos.removeAll(toRemove);
						potentialAutos.clear();
					}
					break;
				case ADD_TEXT_TOOL:
					if (SwingUtilities.isLeftMouseButton(e) && !wasDragging)
					{
						ChartTextBox nearby = getNearByText(new Point(e.getX(), e.getY()));
						if (currentlyBeingEdited != null) //currently editing a text box
						{
							if (nearby != currentlyBeingEdited) //clicked away from current box
							{
								stoppedEditingTextBox();
							}
						}
						else //not currently editing a text box
						{
							if (nearby != null) //clicked an existing text box
							{
								currentlyBeingEdited = nearby;
							}
							else //create a new text box
							{
								currentlyBeingEdited = new ChartTextBox("", new Point(e.getX(), e.getY()));
								postChartChange(new ChartChangedEvent(ADD_ELEMENT, TEXT, currentlyBeingEdited));
								textBoxes.add(currentlyBeingEdited);
							}
						}
					}
					else if (wasDragging)
					{
						if (currentlyBeingHovered != null) //if stopped dragging a text box, move it to align with nearest textbox axis
						{
							currentlyBeingHovered.point = new Point(currentlyBeingHovered.point.getX() + alignmentOffsetX, currentlyBeingHovered.point.getY() + alignmentOffsetY);
						}
						alignmentOffsetX = 0;
						alignmentOffsetY = 0;
					}
					drawGraph();
					break;
				case ADD_THRALL_TOOL:
					if (SwingUtilities.isLeftMouseButton(e))
					{
						addThrallBox(new ThrallOutlineBox(hoveredPlayer, hoveredTick, MAGE_THRALL)); //todo maybe give options for other color thralls?
					}
					else if (SwingUtilities.isRightMouseButton(e))
					{
						if (hoveredThrallIntersectsExisting())
						{
							synchronized (thrallOutlineBoxes)
							{
								List<ThrallOutlineBox> toRemove = thrallOutlineBoxes.stream().filter
									(o -> o.spawnTick == hoveredThrallBox.spawnTick && o.owner.equals(hoveredThrallBox.owner)).collect(Collectors.toList());
								toRemove.forEach(o -> postChartChange(new ChartChangedEvent(REMOVE_ELEMENT, THRALL, o)));
								thrallOutlineBoxes.removeAll(toRemove);
								if (room.equals("Creator"))
								{
									computeProbability2();
								}
							}
						}
					}
					drawGraph();
					break;
			}
			if (SwingUtilities.isLeftMouseButton(e))
			{
				if (isShiftPressed())
				{
					selectionDragActive = false;
					int lowestPlayer = Math.min(playerOffsets.get(activeDragPlayer), playerOffsets.get(dragStartPlayer));
					int highestPlayer = Math.max(playerOffsets.get(activeDragPlayer), playerOffsets.get(dragStartPlayer));
					int lowestTick = Math.min(activeDragTick, dragStartTick);
					int highestTick = Math.max(activeDragTick, dragStartTick);
					for (OutlineBox box : outlineBoxes)
					{
						if (box.tick >= lowestTick && box.tick <= highestTick && playerOffsets.get(box.player) >= lowestPlayer && playerOffsets.get(box.player) <= highestPlayer)
						{
							selectedTicks.add(new ChartTick(box.tick, box.player));
						}
					}
				}
				else if (selectedTicks.size() > 1 && !isCtrlPressed())
				{
					selectedTicks.clear();
				}
			}
		}
	}

	private void stoppedEditingTextBox()
	{
		if (currentlyBeingEdited != null && textBoxes.contains(currentlyBeingEdited))
		{
			if (currentlyBeingEdited.text.isEmpty())
			{
				postChartChange(new ChartChangedEvent(REMOVE_ELEMENT, TEXT, currentlyBeingEdited));
				textBoxes.remove(currentlyBeingEdited);
			}
		}
		currentlyBeingEdited = null;
	}

	boolean isOuterEdgeOfNearbyText = false;

	public boolean inRectangle(Rectangle rect, int x, int y)
	{
		return x > rect.x && x < rect.x + rect.width && y > rect.y && y < rect.y + rect.height;
	}

	private void setIsOuterEdgeOfNearbyText(boolean state)
	{
		if (currentTool == ADD_TEXT_TOOL && currentlyBeingEdited == null) //don't change cursor if actively editing
		{
			isOuterEdgeOfNearbyText = state;
			setCursor(Cursor.getPredefinedCursor(state ? Cursor.MOVE_CURSOR : Cursor.TEXT_CURSOR));
		}
	}

	private ChartTextBox getNearByText(Point current)
	{
		for (ChartTextBox p : textBoxes)
		{
			// Use getTextBoxBounds to correctly calculate the bounds for multi-line text
			Rectangle bounds = getTextBoxBounds((Graphics2D) img.getGraphics(), p, -currentScrollOffsetX, -currentScrollOffsetY);

			Rectangle boundsExtruded = new Rectangle(bounds.x - 5, bounds.y - 5, bounds.width + 10, bounds.height + 10);
			if (inRectangle(boundsExtruded, current.getX(), current.getY()))
			{
				setIsOuterEdgeOfNearbyText(!inRectangle(bounds, current.getX(), current.getY()));
				return p;
			}
		}
		setIsOuterEdgeOfNearbyText(false);
		return null;
	}


	List<ChartTextBox> textBoxes = new ArrayList<>();
	public static ChartTextBox currentlyBeingEdited = null;
	private ChartTextBox currentlyBeingHovered = null;
	Map<String, Integer> playerSBSCoolDown = new HashMap<>();
	Map<String, Integer> playerVengCoolDown = new HashMap<>();
	Map<String, Integer> playerThrallCoolDown = new HashMap<>();
	Map<String, Integer> playerDCCoolDown = new HashMap<>();


	private static Map<String, Double> probabilityCache = new ConcurrentHashMap<>();

	// ExecutorService for background computations
	private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	// Set to keep track of cumulative counts currently being computed
	private static Set<String> pendingComputations = Collections.synchronizedSet(new HashSet<>());

	// Map cumulative counts key to ticks that use them
	private final Map<String, List<Integer>> cumulativeCountsToTicks = new ConcurrentHashMap<>();

	private int hp = Integer.MAX_VALUE;
	public void computeProbability2()
	{
		// Map of tick to Map of presetName to count
		Map<Integer, Map<String, Integer>> attacksPerTick = new HashMap<>();

		// Map of tick to thrall attack count
		Map<Integer, Integer> thrallAttacksPerTick = new HashMap<>();

		// Process OutlineBoxes
		for (OutlineBox outlineBox : outlineBoxes)
		{
			if (outlineBox.getPreset() != null)
			{
				int tick = outlineBox.getTick();
				String presetName = outlineBox.getPreset().getName();

				attacksPerTick.computeIfAbsent(tick, k -> new HashMap<>());
				Map<String, Integer> presetCountsAtTick = attacksPerTick.get(tick);
				presetCountsAtTick.put(presetName,
					presetCountsAtTick.getOrDefault(presetName, 0) + 1);
			}
		}

		// Process ThrallOutlineBoxes
		for (ThrallOutlineBox thrallOutlineBox : thrallOutlineBoxes)
		{
			int summonTick = thrallOutlineBox.spawnTick;
			// Thrall attacks start from tick after summonTick, and then every 4 ticks
			for (int attackTick = summonTick + 1; attackTick <= endTick; attackTick += 4)
			{
				thrallAttacksPerTick.put(attackTick,
					thrallAttacksPerTick.getOrDefault(attackTick, 0) + 1);
			}
		}

		// Maps to maintain cumulative counts
		Map<String, Integer> cumulativePresetCounts = new HashMap<>();
		int cumulativeThrallCount = 0;

		if (target != null && target.getSkills() != null)
		{
			int tempHP;
			tempHP = target.getSkills().getHp();
			int playerCount = playerOffsets.keySet().size();
			if (playerCount == 4)
			{
				tempHP = tempHP * 7 / 8;
			}
			else if (playerCount < 4)
			{
				tempHP = tempHP * 3 / 4;
			}
			if(tempHP != hp)
			{
				hp = tempHP;
				probabilityCache.clear();
				probabilityMap.clear();
			}
		}

		for (int tick = startTick; tick <= endTick; tick++)
		{
			if (attacksPerTick.containsKey(tick))
			{
				Map<String, Integer> attacksAtTick = attacksPerTick.get(tick);
				for (Map.Entry<String, Integer> entry : attacksAtTick.entrySet())
				{
					String presetName = entry.getKey();
					int count = entry.getValue();
					cumulativePresetCounts.put(presetName,
						cumulativePresetCounts.getOrDefault(presetName, 0) + count);
				}
			}

			cumulativeThrallCount += thrallAttacksPerTick.getOrDefault(tick, 0);

			// Create a unique key for the current cumulative counts
			StringBuilder keyBuilder = new StringBuilder();

			// Sort preset names to ensure consistent key generation
			List<String> presets = new ArrayList<>(cumulativePresetCounts.keySet());
			Collections.sort(presets);

			for (String preset : presets)
			{
				keyBuilder.append(preset).append(":")
					.append(cumulativePresetCounts.get(preset)).append("|");
			}
			keyBuilder.append("thrall:").append(cumulativeThrallCount);

			String key = keyBuilder.toString();

			// Map the key to this tick
			cumulativeCountsToTicks.computeIfAbsent(key, k -> new ArrayList<>()).add(tick);

			// Check if the result is already in the cache
			Double probability = probabilityCache.get(key);
			if (probability != null)
			{
				// Use cached probability
				probabilityMap.put(tick, probability);
			}
			else
			{
				// Probability not in cache
				// Check if computation is already pending
				if (!pendingComputations.contains(key))
				{
					pendingComputations.add(key);
					// Submit computation task
					executorService.submit(new ProbabilityComputationTask(key, cumulativePresetCounts, cumulativeThrallCount, hp));
				}
				// Do not update probabilityMap yet
			}
		}

		// Initial UI update with whatever probabilities are available
		SwingUtilities.invokeLater(this::drawGraph);
	}

	private class ProbabilityComputationTask implements Runnable
	{
		private String key;
		private Map<String, Integer> cumulativePresetCountsCopy;
		private int cumulativeThrallCount;
		private int hp;

		public ProbabilityComputationTask(String key, Map<String, Integer> cumulativePresetCounts, int cumulativeThrallCount, int hp)
		{
			this.key = key;
			// Make a copy of cumulativePresetCounts as it can change
			this.cumulativePresetCountsCopy = new HashMap<>(cumulativePresetCounts);
			this.cumulativeThrallCount = cumulativeThrallCount;
			this.hp = hp;
		}

		@Override
		public void run()
		{
			try
			{
				// Compute the probability
				ProbabilityCalculator calculator = new ProbabilityCalculator();
				for (String presetName : cumulativePresetCountsCopy.keySet())
				{
					calculator.add(ProbabilityUtility.convertPreset(
						PresetManager.getPresets().get(presetName),
						target,
						cumulativePresetCountsCopy.get(presetName)));
				}
				// Add thrall attacks
				calculator.add(new ProbabilityCalculator.RandomComparedRollGroup(0, 3, 0, 10, cumulativeThrallCount));

				calculator.setSumToCalculate(hp, ProbabilityCalculator.ComparisonOperator.GREATER_THAN_OR_EQUAL);

				Double probability = calculator.getProbability();

				// Update cache
				probabilityCache.put(key, probability);

				// Remove from pendingComputations
				pendingComputations.remove(key);

				synchronized (cumulativeCountsToTicks)
				{
					// Get the ticks associated with this key
					List<Integer> ticks = cumulativeCountsToTicks.get(key);

					if (ticks != null)
					{
						for (Integer tick : ticks)
						{
							// Update probabilityMap
							probabilityMap.put(tick, probability);
						}

						// Trigger UI update on EDT
						SwingUtilities.invokeLater(() -> drawGraph());
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}


	private void removeAttack(OutlineBox box)
	{
		removeAttack(box.tick, box.player, false);
	}

	private void removeAttack(int tick, String player, boolean shouldRecord)
	{
		if (tick != -1 && room.equals("Creator")) //don't allow attacks to be removed if this chart panel isn't part of the chart creator
		{
			synchronized (outlineBoxes)
			{
				List<OutlineBox> removedBoxes = new ArrayList<>();
				for (OutlineBox box : outlineBoxes)
				{
					if (box.tick == tick && Objects.equals(box.player, player))
					{
						removedBoxes.add(box);
					}
				}
				outlineBoxes.removeAll(removedBoxes);

				updatePlayerAnimationsInUse();
				for (OutlineBox removedBox : removedBoxes)
				{
					for (int i = tick; i < tick + removedBox.cd; i++)
					{
						playerWasOnCD.remove(player, i);
					}
				}
				postChartChange(new ChartChangedEvent(REMOVE_ELEMENT, ATTACK, removedBoxes.toArray()));
				if (shouldRecord)
				{
					actionHistory.push(new ChartAction(removedBoxes, ChartActionType.REMOVE_ELEMENT));
				}
			}
		}
		if (room.equals("Creator"))
		{
			computeProbability2();
		}
		drawGraph();
	}

	public void copyAttackData()
	{
		String attackData = "";
		for (String player : playerOffsets.keySet())
		{
			attackData += player + ",";
			for (OutlineBox box : outlineBoxes)
			{
				if (box.player.equals(player))
				{
					attackData += "{" + box.tick + ":" + box.playerAnimation.ordinal() + "},";
				}
			}
			attackData += "\n";
		}

		StringSelection selection = new StringSelection(attackData);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);
	}

	int currentTool = NO_TOOL;

	public void setToolSelection(int tool)
	{
		if (tool == ADD_TEXT_TOOL)
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		}
		else
		{
			setCursor(Cursor.getDefaultCursor());
		}
		currentTool = tool;
		drawGraph();
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		hoveredAction = null;
		hoveredPlayer = "";
		hoveredTick = -1;
		hoveredColumn = -1;
		drawGraph();
	}

	int dragStartX = 0;
	int dragStartY = 0;
	int lastScrollOffsetX = 0;
	int lastScrollOffsetY = 0;
	int lastStartTick = 0;
	int lastEndTick = 0;
	int dragStartTick = 0;
	String dragStartPlayer = "";
	int activeDragTick = 0;
	String activeDragPlayer = "";
	int activeDragX = 0;
	int activeDragY = 0;
	private static final int DRAG_THRESHOLD = 5;

	@Override
	public void mouseDragged(MouseEvent e)
	{
		setDraggedTickValues(e.getX(), e.getY());
		if (!isDragging)
		{
			dragStartX = e.getX();
			dragStartY = e.getY();
		}

		if (isPossibleExtension && !isExtendingAttack)
		{
			int dx = e.getX() - dragStartX;
			int dy = e.getY() - dragStartY;
			if (Math.hypot(dx, dy) >= DRAG_THRESHOLD)
			{
				isExtendingAttack = true;
				extensionOriginTick = possibleExtensionBox.tick;
				extensionOriginPlayer = possibleExtensionBox.player;
				extensionCD = possibleExtensionBox.cd;
				extensionOriginBox = possibleExtensionBox;
			}
		}

		if (isExtendingAttack)
		{
			// Update hoveredTick and hoveredPlayer
			setDraggedTickValues(e.getX(), e.getY());

			// Check if the drag is in the same player's row and moves beyond the origin tick
			if (extensionOriginPlayer.equals(activeDragPlayer) && activeDragTick != -1)
			{
				// Calculate potential ticks
				extensionPreviewTicks.clear();

				// Ensure CD is valid
				if (extensionCD <= 0)
				{
					return;
				}

				int tick = extensionOriginTick + extensionCD;
				while (tick <= endTick)
				{
					// Check for conflicts in the cooldown period
					boolean conflict = false;
					synchronized (outlineBoxes)
					{
						for (OutlineBox box : outlineBoxes)
						{
							if (box.player.equals(extensionOriginPlayer))
							{
								int boxStart = box.tick;
								int boxEnd = box.tick + box.cd - 1; // -1 because cd includes the starting tick
								int extensionStart = tick;
								int extensionEnd = tick + extensionCD - 1;

								// Check if the cooldown periods overlap
								if (boxEnd >= extensionStart && boxStart <= extensionEnd)
								{
									conflict = true;
									break;
								}
							}
						}
					}
					if (conflict)
					{
						break; // Stop extending if there's a conflict
					}

					extensionPreviewTicks.add(tick);
					tick += extensionCD;
				}

				// Update the graphics to show the preview
				drawGraph();
			}
			else
			{
				// Drag is invalid (not the same player or activeDragTick is invalid)
				extensionPreviewTicks.clear();
				drawGraph();
			}
		}
		if (SwingUtilities.isMiddleMouseButton(e)) //middle mouse drag shifts x and y of viewport
		{
			if (!isDragging) //capture position at start of middle mouse drag
			{
				lastScrollOffsetX = 0;
				lastScrollOffsetY = currentScrollOffsetY;
				lastStartTick = startTick;
				lastEndTick = endTick;
			}
			else //adjust offsets relative to captured drag position
			{
				currentScrollOffsetY = lastScrollOffsetY + (dragStartY - e.getY());
				currentScrollOffsetX = lastScrollOffsetX + (dragStartX - e.getX());
				if (lastStartTick + (currentScrollOffsetX / scale) > 0)
				{
					startTick = lastStartTick + (currentScrollOffsetX / scale);
					endTick = lastEndTick + (currentScrollOffsetX / scale);
				}
				drawGraph();
			}
		}
		else if (SwingUtilities.isLeftMouseButton(e) && isShiftPressed()) //shift drag to select ticks
		{
			if (!isDragging) //capture start player & tick
			{
				selectionDragActive = true;
				dragStartPlayer = hoveredPlayer;
				dragStartTick = hoveredTick;
			}
			else //currently dragging, keep track of position so on release can find the region to select
			{
				activeDragX = e.getX();
				activeDragY = e.getY();
			}
		}
		else if (currentTool == ADD_TEXT_TOOL && currentlyBeingEdited == null && isOuterEdgeOfNearbyText && currentlyBeingHovered != null)
		{
			if (!isDragging)
			{
				draggedTextOffsetX = e.getX() - currentlyBeingHovered.getPoint().getX();
				draggedTextOffsetY = e.getY() - currentlyBeingHovered.getPoint().getY();
			}

			draggedTextPosX = e.getX() - draggedTextOffsetX;
			draggedTextPosY = e.getY() - draggedTextOffsetY;

			setAlignmentMarkers(draggedTextPosX, draggedTextPosY);
			isDraggingText = true;
		}

		isDragging = true;
	}

	public void setDraggedTickValues(int x, int y) //todo migrate generic
	{
		if (boxHeight > 0 && scale > 0)
		{
			y = y + currentScrollOffsetY;
			x = x + (currentScrollOffsetX % scale);
			if (y > 20) //todo why do I use 20 here when TOP_MARGIN is 30?
			{
				int boxNumber = (y - 20) / boxHeight;
				if (x > LEFT_MARGIN - 5)
				{
					int tick = startTick + (ticksToShow * boxNumber + ((x - LEFT_MARGIN) / scale));
					int playerOffsetPosition = (((y - TOP_MARGIN - scale) % boxHeight) / scale);
					if (playerOffsetPosition >= 0 && playerOffsetPosition < attackers.size() && (y - TOP_MARGIN - scale > 0))
					{
						activeDragTick = tick;
						activeDragPlayer = attackers.get(playerOffsetPosition);
					}
					else if (y % boxHeight < TOP_MARGIN + scale)
					{
						activeDragTick = tick;
						activeDragPlayer = "";
					}
					else
					{
						activeDragPlayer = "";
						activeDragTick = -1;
					}
				}
				else
				{
					activeDragPlayer = "";
					activeDragTick = -1;
				}
				drawGraph();
			}
		}
	}

	private final List<OutlineBox> copiedOutlineBoxes = new ArrayList<>();
	int copiedTick = 0;
	int copiedPlayer = 0;

	int alignmentOffsetX = 0;
	int alignmentOffsetY = 0;

	public void copyAttacks()
	{
		if (!selectedTicks.isEmpty())
		{
			int lowestTick = Integer.MAX_VALUE;
			int lowestPlayer = Integer.MAX_VALUE;
			for (ChartTick chartTick : selectedTicks)
			{
				lowestTick = Math.min(lowestTick, chartTick.getTick());
				lowestPlayer = Math.min(lowestPlayer, playerOffsets.get(chartTick.getPlayer()));
			}
			copiedTick = lowestTick;
			copiedPlayer = lowestPlayer;
			copiedOutlineBoxes.clear();
			synchronized (outlineBoxes)
			{
				for (OutlineBox box : outlineBoxes)
				{
					if (selectedTicks.stream().anyMatch(b -> b.getTick() == box.tick && b.getPlayer().equals(box.player)))
					{
						copiedOutlineBoxes.add(box);
					}
				}
			}
		}
	}

	public void pasteAttacks()
	{
		if (selectedTicks.size() == 1)
		{
			List<OutlineBox> boxesToAddToHistory = new ArrayList<>();
			for (OutlineBox box : copiedOutlineBoxes)
			{
				int tickOffset = selectedTicks.get(0).getTick() - copiedTick;
				int playerOffset = playerOffsets.get(selectedTicks.get(0).getPlayer()) - copiedPlayer;
				if (playerOffset + playerOffsets.get(box.player) <= playerOffsets.size() - 1 && tickOffset < endTick)
				{
					synchronized (outlineBoxes)
					{
						String translatedPlayer = "";
						for (String player : playerOffsets.keySet())
						{
							if (playerOffsets.get(player).equals(playerOffset + playerOffsets.get(box.player)))
							{
								translatedPlayer = player;
							}
						}
						OutlineBox outlineBox = new OutlineBox(box.letter, box.color, box.primaryTarget, box.additionalText, box.playerAnimation, box.cd, box.tick + tickOffset, translatedPlayer, RaidRoom.getRoom(this.room), box.weapon);
						addAttack(outlineBox);
						boxesToAddToHistory.add(outlineBox);
					}
				}
			}
			actionHistory.push(new ChartAction(boxesToAddToHistory, ADD_ELEMENT));
		}
		drawGraph();
	}

	public void addAttack(OutlineBox box) {
		changesSaved = false;
		synchronized (outlineBoxes) {
			// Remove any existing OutlineBox at the same tick and player
			removeAttack(box.tick, box.player, false);

			// Handle cooldowns
			if (box.playerAnimation == PlayerAnimation.SBS) {
				playerSBSCoolDown.put(box.player, box.tick + 10);
			} else if (box.playerAnimation == PlayerAnimation.VENG_SELF) {
				playerVengCoolDown.put(box.player, box.tick + 15);
			} else if (box.playerAnimation == PlayerAnimation.DEATH_CHARGE) {
				playerDCCoolDown.put(box.player, box.tick + 15);
			} else if (box.playerAnimation == PlayerAnimation.THRALL_CAST) {
				playerThrallCoolDown.put(box.player, box.tick + 15);
			}

			// Handle spotAnims
			String[] spotAnims = (box.getSpotAnims() != null) ? box.getSpotAnims().split(":") : new String[0];

			if (spotAnims.length > 0) {
				if (!Objects.equals(spotAnims[0], "")) {
					int graphic = Integer.parseInt(spotAnims[0]);
					if (graphic == 1062 && playerSBSCoolDown.getOrDefault(box.player, 0) <= box.tick) { // sbs
						box.secondaryID = graphic;
						playerSBSCoolDown.put(box.player, box.tick + 10);
					} else if (graphic == 726 && playerVengCoolDown.getOrDefault(box.player, 0) <= box.tick) { // veng
						box.secondaryID = graphic;
						playerVengCoolDown.put(box.player, box.tick + 15);
					} else if (graphic == 1854 && playerDCCoolDown.getOrDefault(box.player, 0) <= box.tick) { // death charge
						box.secondaryID = graphic;
						playerDCCoolDown.put(box.player, box.tick + 15);
					} else if ((graphic == 1873 || graphic == 1874 || graphic == 1875) && playerThrallCoolDown.getOrDefault(box.player, 0) <= box.tick) { // thrall
						box.secondaryID = graphic;
						playerThrallCoolDown.put(box.player, box.tick + 15);
					}
					if (graphic != 1062) { // non sbs secondary graphic -> force reset?
						playerSBSCoolDown.put(box.player, 0);
					}
					if (graphic != 1062 && graphic != 726 && graphic != 1854 && !(box.playerAnimation == PlayerAnimation.BARRAGE || box.playerAnimation == PlayerAnimation.BLITZ)) {
						box.secondaryID = graphic;
					}
				}
			}
			if (box.playerAnimation == PlayerAnimation.BARRAGE || box.playerAnimation == PlayerAnimation.BLITZ || box.playerAnimation == PlayerAnimation.THRALL_CAST || box.playerAnimation == PlayerAnimation.VENG_SELF
				|| box.playerAnimation == PlayerAnimation.AID_OTHER || box.playerAnimation == PlayerAnimation.HUMIDIFY || box.playerAnimation == PlayerAnimation.MAGIC_IMBUE) {
				playerSBSCoolDown.put(box.player, 0);
			}

			if (box.playerAnimation != PlayerAnimation.VENG_APPLIED) {
				for (Iterator<OutlineBox> iterator = outlineBoxes.iterator(); iterator.hasNext();) {
					OutlineBox existingBox = iterator.next();
					if (existingBox.playerAnimation == PlayerAnimation.VENG_APPLIED && existingBox.tick == box.tick && existingBox.player.equals(box.player)) {
						iterator.remove();
						box.tertiaryID = -3;
						box.setDamage(existingBox.damage);
						break;
					}
				}
			}

			outlineBoxes.add(box);
			postChartChange(new ChartChangedEvent(ADD_ELEMENT, ATTACK, box));
			updatePlayerAnimationsInUse();
		}
		for (int i = box.tick; i < box.tick + box.cd; i++) {
			playerWasOnCD.put(box.player, i);
		}
		if (room.equals("Creator")) {
			computeProbability2();
		}
		if (!live) {
			drawGraph();
		}
	}


	private void setPotentialAutos()
	{
		potentialAutos.clear();
		if (hoveredTick != -1)
		{
			potentialAutos.add(hoveredTick);
			if (autoScrollAmount > 0)
			{
				for (int i = hoveredTick; i < endTick; i++)
				{
					if (((i - hoveredTick) % autoScrollAmount) == 0)
					{
						potentialAutos.add(i);
					}
				}
			}
		}
	}

	List<Line> alignmentMarkers = new ArrayList<>();

	private static final int ALIGNMENT_THRESHOLD = 5;
	private static final int SPACING_THRESHOLD = 10; // Increased tolerance for spacing alignment

	private void setAlignmentMarkers(int x, int y)
	{
		alignmentMarkers.clear();
		alignmentOffsetX = 0;
		alignmentOffsetY = 0;

		if (currentTool == ADD_TEXT_TOOL && currentlyBeingEdited == null && isDragging)
		{
			// Collect existing spacings
			Set<Integer> verticalSpacings = new HashSet<>();
			for (ChartTextBox p1 : textBoxes)
			{
				if (p1 != currentlyBeingHovered)
				{
					for (ChartTextBox p2 : textBoxes)
					{
						if (p2 != p1 && p2 != currentlyBeingHovered)
						{
							int spacing = Math.abs(p2.getPoint().getY() - p1.getPoint().getY());
							verticalSpacings.add(spacing);
						}
					}
				}
			}

			// Alignment markers
			for (ChartTextBox p : textBoxes)
			{
				if (p != currentlyBeingHovered)
				{
					int dx = Math.abs(p.getPoint().getX() - x);
					int dy = Math.abs(p.getPoint().getY() - y);

					// Alignment on X-axis (vertical line)
					if (dx < ALIGNMENT_THRESHOLD)
					{
						alignmentOffsetX = p.getPoint().getX() - x;
						Point p1 = new Point(p.getPoint().getX(), y - 1000);
						Point p2 = new Point(p.getPoint().getX(), y + 1000);
						alignmentMarkers.add(new Line(p1, p2));
					}

					// Alignment on Y-axis (horizontal line)
					if (dy < ALIGNMENT_THRESHOLD)
					{
						alignmentOffsetY = p.getPoint().getY() - y;
						Point p1 = new Point(x - 1000, p.getPoint().getY());
						Point p2 = new Point(x + 1000, p.getPoint().getY());
						alignmentMarkers.add(new Line(p1, p2));
					}

					// Vertical spacing alignment
					int actualSpacing = p.getPoint().getY() - y;
					for (int spacing : verticalSpacings)
					{
						if (Math.abs(Math.abs(actualSpacing) - spacing) < SPACING_THRESHOLD)
						{
							// Adjust alignmentOffsetY to match the spacing
							alignmentOffsetY = (actualSpacing > 0) ? (p.getPoint().getY() - y - spacing) : (p.getPoint().getY() - y + spacing);

							// Draw vertical line between the two boxes
							int lineX = p.getPoint().getX() + alignmentOffsetX;
							int lineY1 = p.getPoint().getY();
							int lineY2 = y + alignmentOffsetY;

							// Ensure lineY1 is the top point
							if (lineY1 > lineY2)
							{
								int temp = lineY1;
								lineY1 = lineY2;
								lineY2 = temp;
							}

							Point p1 = new Point(lineX, lineY1);
							Point p2 = new Point(lineX, lineY2);
							alignmentMarkers.add(new Line(p1, p2));
						}
					}
				}
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		java.awt.Point mousePoint = e.getPoint();

		boolean hoverChanged = false;
		PlayerAnimation previousHoveredAction = hoveredAction;
		hoveredAction = null;

		// Handle hover for the toggle button
		if (sidebarToggleButtonBounds != null && sidebarToggleButtonBounds.contains(mousePoint))
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			hoverChanged = true;
		}
		else
		{
			setCursor(Cursor.getDefaultCursor());

			if (!isSidebarCollapsed)
			{
				// Handle hover for action icons
				for (Map.Entry<PlayerAnimation, Rectangle> entry : actionIconPositions.entrySet())
				{
					if (entry.getValue().contains(mousePoint))
					{
						hoveredAction = entry.getKey();
						break;
					}
				}

				// Handle hover for sidebar buttons
				for (SidebarButton button : sidebarButtons)
				{
					boolean wasHovered = button.isHovered;
					button.isHovered = button.contains(e.getX(), e.getY());

					// Reset hover effect if hover state changed
					if (button.isHovered != wasHovered)
					{
						button.hoverEffectEnabled = true;
						hoverChanged = true;
					}
				}
			}
		}

		if (hoverChanged || hoveredAction != previousHoveredAction)
		{
			drawGraph();
		}

		// Skip chart hover actions if over the sidebar or if sidebar is collapsed and over the toggle button
		if (sidebarToggleButtonBounds != null && sidebarToggleButtonBounds.contains(e.getX(), e.getY()))
		{
			return;
		}
		if (!isSidebarCollapsed && e.getX() > getWidth() - sidebarWidth - RIGHT_MARGIN * 2)
		{
			return;
		}

		// Process chart-specific hover actions
		setTickHovered(e.getX(), e.getY());

		if (currentTool == ADD_TEXT_TOOL && isDragging)
		{
			setAlignmentMarkers(e.getX(), e.getY());
		}

		if (currentTool == ADD_AUTO_TOOL)
		{
			setPotentialAutos();
		}
		else
		{
			potentialAutos.clear();
		}
	}

	public String getBossName(int id, int index, int tick)
	{
		try
		{
			switch (id)
			{
				case TobIDs.MAIDEN_P0:
				case TobIDs.MAIDEN_P1:
				case TobIDs.MAIDEN_P2:
				case TobIDs.MAIDEN_P3:
				case TobIDs.MAIDEN_PRE_DEAD:
				case TobIDs.MAIDEN_P0_HM:
				case TobIDs.MAIDEN_P1_HM:
				case TobIDs.MAIDEN_P2_HM:
				case TobIDs.MAIDEN_P3_HM:
				case TobIDs.MAIDEN_PRE_DEAD_HM:
				case TobIDs.MAIDEN_P0_SM:
				case TobIDs.MAIDEN_P1_SM:
				case TobIDs.MAIDEN_P2_SM:
				case TobIDs.MAIDEN_P3_SM:
				case TobIDs.MAIDEN_PRE_DEAD_SM:
					return "Maiden (" + RoomUtil.varbitHPtoReadable(roomHP.get(tick + 1)) + ")";
				case TobIDs.BLOAT:
				case TobIDs.BLOAT_HM:
				case TobIDs.BLOAT_SM:
					return "Bloat (" + RoomUtil.varbitHPtoReadable(roomHP.get(tick)) + ")";
				case TobIDs.NYLO_BOSS_MELEE:
				case TobIDs.NYLO_BOSS_RANGE:
				case TobIDs.NYLO_BOSS_MAGE:
				case TobIDs.NYLO_BOSS_MELEE_HM:
				case TobIDs.NYLO_BOSS_RANGE_HM:
				case TobIDs.NYLO_BOSS_MAGE_HM:
				case TobIDs.NYLO_BOSS_MELEE_SM:
				case TobIDs.NYLO_BOSS_RANGE_SM:
				case TobIDs.NYLO_BOSS_MAGE_SM:
					return "Nylo Boss (" + RoomUtil.varbitHPtoReadable(roomHP.get(tick)) + ")";
				case TobIDs.XARPUS_P23:
				case TobIDs.XARPUS_P23_HM:
				case TobIDs.XARPUS_P23_SM:
					return "Xarpus (" + RoomUtil.varbitHPtoReadable(roomHP.get(tick)) + ")";
				case TobIDs.VERZIK_P1:
				case TobIDs.VERZIK_P2:
				case TobIDs.VERZIK_P3:
				case TobIDs.VERZIK_P1_HM:
				case TobIDs.VERZIK_P2_HM:
				case TobIDs.VERZIK_P3_HM:
				case TobIDs.VERZIK_P1_SM:
				case TobIDs.VERZIK_P2_SM:
				case TobIDs.VERZIK_P3_SM:
					return "Verzik (" + RoomUtil.varbitHPtoReadable(roomHP.get(tick)) + ")";
			}
			for (Integer i : NPCMap.keySet())
			{
				if (i == index)
				{
					String hp = "-1";
					try
					{
						hp = RoomUtil.varbitHPtoReadable(roomHP.get(tick));
					}
					catch (Exception ignored
					)
					{
					}
					return NPCMap.get(i) + " (Boss: " + hp + ")";
				}
			}
			return "?";
		}
		catch (Exception e)
		{
			return "?";
		}
	}

	public void setEnforceCD(boolean bool)
	{
		enforceCD = bool;
		drawGraph();
	}

	public ChartIOData getForSerialization()
	{
		return new ChartIOData(startTick, endTick, room, roomSpecificText, autos, specific, lines, outlineBoxes, textBoxes, "", thrallOutlineBoxes);
	}

	public void setStatus(String text)
	{
		if (statusBar != null)
		{
			statusBar.set(text);
		}
	}

	public void appendStatus(String text)
	{
		if (statusBar != null)
		{
			statusBar.append(text);
		}
	}

	public void appendToStartStatus(String text)
	{
		if (statusBar != null)
		{
			statusBar.appendToStart(text);
		}
	}

	@Setter
	private JTree tree;

	public void updateTree()
	{
		if (tree != null)
		{
			tree.setModel(null);
			tree.setCellRenderer(new DefaultTreeCellRenderer()
			{
				@Override
				public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
															  boolean isLeaf, int row, boolean focused)
				{
					Component cell = super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
					cell.setForeground(config.fontColor());
					return cell;
				}
			});
			DefaultMutableTreeNode root = new DefaultMutableTreeNode("Layers")
			{
				{
					setForeground(config.fontColor());
				}
			};
			DefaultMutableTreeNode attacks = new DefaultMutableTreeNode("Attacks")
			{
				{
					setForeground(config.fontColor());
				}
			};
			Map<String, DefaultMutableTreeNode> playerNodes = new LinkedHashMap<>();
			for (String player : playerOffsets.keySet())
			{
				playerNodes.put(player, new DefaultMutableTreeNode(player)
				{
					{
						setForeground(config.fontColor());
					}
				});
			}
			for (OutlineBox box : outlineBoxes)
			{
				playerNodes.get(box.player).add(new DefaultMutableTreeNode(box));
			}
			for (DefaultMutableTreeNode nodes : playerNodes.values())
			{
				attacks.add(nodes);
			}

			root.add(attacks);
			tree.setModel(new DefaultTreeModel(root));
		}
	}

	@Override
	public void focusGained(FocusEvent e)
	{
		setBorder(new LineBorder(config.boxColor()));
	}

	@Override
	public void focusLost(FocusEvent e)
	{
		setBorder(null);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent e)
	{
		synchronized (ChartPanel.class)
		{
			if (!shouldDraw() || (!room.equals("Creator") && !(e.getKeyCode() == KeyEvent.VK_CONTROL)))
			{
				return false;
			}
			switch (e.getID())
			{
				case KeyEvent.KEY_PRESSED:
					switch (e.getKeyCode())
					{
						case KeyEvent.VK_CONTROL:
							isCtrlPressed = true;
							break;
						case KeyEvent.VK_SHIFT:
							isShiftPressed = true;
							break;
					}
					break;
				case KeyEvent.KEY_RELEASED:
					switch (e.getKeyCode())
					{
						case KeyEvent.VK_CONTROL:
							isCtrlPressed = false;
							autoScrollAmount = 0;
							break;
						case KeyEvent.VK_SHIFT:
							isShiftPressed = false;
							break;
						case KeyEvent.VK_C:
							if (isCtrlPressed())
							{
								copyAttacks();
							}
							break;
						case KeyEvent.VK_V:
							if (isCtrlPressed())
							{
								pasteAttacks();
							}
							break;
						case KeyEvent.VK_DELETE:
							if (currentTool == SELECTION_TOOL)
							{
								List<OutlineBox> selectedBoxes = new ArrayList<>();
								for (ChartTick tick : selectedTicks)
								{
									for (OutlineBox box : outlineBoxes)
									{
										if (box.tick == tick.getTick() && Objects.equals(box.player, tick.getPlayer()))
										{
											selectedBoxes.add(box);
										}
									}
								}
								for (OutlineBox box : selectedBoxes)
								{
									removeAttack(box);
								}
								actionHistory.add(new ChartAction(selectedBoxes, ChartActionType.REMOVE_ELEMENT));
								selectedTicks.clear();
								redraw();
							}
							break;
						case KeyEvent.VK_Z:
							if (isCtrlPressed())
							{
								if (!actionHistory.isEmpty())
								{
									processChartAction(actionHistory.pop());
								}
							}
							break;
						case KeyEvent.VK_ENTER:
							if (isEditingBoxText)
							{
								isEditingBoxText = false;
								setCursor(Cursor.getDefaultCursor());
							}
							if (currentlyBeingEdited != null)
							{
								changesSaved = false;
								drawGraph();
							}
							else if (!selectedTicks.isEmpty())
							{
								List<OutlineBox> createdBoxes = new ArrayList<>();
								boolean shouldApplyBoxes = true;
								for (ChartTick tick : selectedTicks)
								{
									synchronized (outlineBoxes)
									{
										for (OutlineBox box : outlineBoxes)
										{
											if (tick.getTick() == box.tick && tick.getPlayer().equals(box.player))
											{
												shouldApplyBoxes = false;
												break;
											}
										}
										if (shouldApplyBoxes)
										{
											OutlineBox createdBox = new OutlineBox(selectedPrimary.shorthand, selectedPrimary.color, true, "", selectedPrimary, selectedPrimary.attackTicks, tick.getTick(), tick.getPlayer(), RaidRoom.getRoom(room), selectedPrimary.weaponIDs[0]);
											createdBoxes.add(createdBox);
										}
									}
								}
								if (shouldApplyBoxes)
								{
									for (OutlineBox box : createdBoxes)
									{
										addAttack(box);
										actionHistory.add(new ChartAction(createdBoxes, ADD_ELEMENT));
									}
								}
								else
								{
									isEditingBoxText = true;
									setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
								}
							}
							drawGraph();
							break;
						case KeyEvent.VK_A:
							if (isCtrlPressed())
							{
								selectedTicks.clear();
								currentTool = SELECTION_TOOL;
								synchronized (outlineBoxes)
								{
									for (OutlineBox box : outlineBoxes)
									{
										selectedTicks.add(new ChartTick(box.tick, box.player));
									}
								}
								drawGraph();
							}
							break;
					}
					break;
				case KeyEvent.KEY_TYPED:
					if (!isCtrlPressed())
					{
						char c = e.getKeyChar();
						if (c == '\b') // Backspace character
						{
							removeLastCharFromSelected();
						}
						else
						{
							if (currentTool == SELECTION_TOOL)
							{
								appendToSelected(c);
							}
							else if (currentTool == ADD_TEXT_TOOL)
							{
								if (currentlyBeingEdited != null)
								{
									currentlyBeingEdited.text += c;
									changesSaved = false;
									if (c == '\n')
									{
										drawGraph(); // Redraw to update the bounding box immediately
									}
								}
							}
						}
					}
					drawGraph();
					break;

			}
			return false;
		}
	}
}
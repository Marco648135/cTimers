package com.advancedraidtracker.ui.charts.chartcreator;

import com.advancedraidtracker.AdvancedRaidTrackerConfig;
import com.advancedraidtracker.ui.charts.ChartPanel;
import com.advancedraidtracker.utility.weapons.PlayerAnimation;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.border.LineBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.advancedraidtracker.ui.charts.ChartConstants.*;
import static com.advancedraidtracker.ui.charts.ChartPanel.*;
import static com.advancedraidtracker.utility.UISwingUtility.*;
import static com.advancedraidtracker.utility.UISwingUtility.createFlipped;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.getReplacement;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.getSpellIcon;

@Slf4j
public class ChartToolPanel extends JPanel implements MouseListener, MouseMotionListener
{
	private BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	private final AdvancedRaidTrackerConfig config;
	private final ChartCreatorFrame parentFrame;
	private PlayerAnimation primary = PlayerAnimation.SCYTHE;
	private PlayerAnimation secondary = PlayerAnimation.NOT_SET;
	private PlayerAnimation hoveredAttack = PlayerAnimation.EXCLUDED_ANIMATION;

	List<ChartTool> tools = List.of(
		new ChartTool(1, ADD_LINE_TOOL, "Line"),
		new ChartTool(1, SELECTION_TOOL, "Select"),
		new ChartTool(1, ADD_TEXT_TOOL, "Text"),
		new ChartTool(1, ADD_AUTO_TOOL, "Auto"),
		new ChartTool(1, ADD_THRALL_TOOL, "Thrall")
	);

	int tool = 0;
	int hoveredTool = NO_TOOL;
	int toolMargin = 10;

	Map<PlayerAnimation, BufferedImage> iconMap = new HashMap<>();
	private final Set<PlayerAnimation> excludedAnimations = new HashSet<>(Arrays.asList(PlayerAnimation.UNARMED, PlayerAnimation.UNDECIDED, PlayerAnimation.EXCLUDED_ANIMATION, PlayerAnimation.ACCURSED_SCEPTRE_AUTO, PlayerAnimation.ACCURSED_SCEPTRE_SPEC, PlayerAnimation.COLOSSEUM_AUTO, PlayerAnimation.COLOSSEUM_SPECIAL, PlayerAnimation.KERIS_SUN_SPEC));

	private boolean isHorizontalFlow;

	private List<ToolItem> allTools = new ArrayList<>();

	public ChartToolPanel(AdvancedRaidTrackerConfig config, ChartCreatorFrame parentFrame, ItemManager itemManager, ClientThread clientThread, SpriteManager spriteManager)
	{
		tool = ADD_ATTACK_TOOL;
		this.parentFrame = parentFrame;
		this.config = config;
		addMouseListener(this);
		addMouseMotionListener(this);
		setBackground(config.primaryDark());
		setFocusable(true);
		setOpaque(true);
		clientThread.invoke(()->
		{
			for(PlayerAnimation playerAnimation : PlayerAnimation.values())
			{
				if (playerAnimation.attackTicks > 0)
				{
					int weaponID = 0;
					if(playerAnimation.weaponIDs.length > 0)
					{
						weaponID = playerAnimation.weaponIDs[0];
					}
					if (config.useUnkitted())
					{
						weaponID = getReplacement(weaponID);
					}
					iconMap.put(playerAnimation, itemManager.getImage(weaponID, 1, false));
				} else
				{
					try
					{
						int animation = 0;
						if(playerAnimation.animations.length > 0)
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
		try
		{
			Thread.sleep(20); //wait for icons to populate
		}
		catch (Exception e)
		{

		}

		addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusGained(FocusEvent e)
			{
				super.focusGained(e);
				setBorder(new LineBorder(config.boxColor()));
			}

			@Override
			public void focusLost(FocusEvent e)
			{
				super.focusLost(e);
				setBorder(null);
			}
		});

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e ->
		{
			synchronized (ChartPanel.class)
			{
				if (e.getID() == KeyEvent.KEY_PRESSED)
				{
					if(!isCtrlPressed() && !isEditingBoxText && currentlyBeingEdited == null)
					{
						switch (e.getKeyCode())
						{
							case KeyEvent.VK_V:
								tool = SELECTION_TOOL;
								parentFrame.setToolSelection(SELECTION_TOOL);
								drawPanel();
								break;
							case KeyEvent.VK_A:
								tool = ADD_ATTACK_TOOL;
								parentFrame.setToolSelection(ADD_ATTACK_TOOL);
								drawPanel();
								break;
							case KeyEvent.VK_L:
								tool = ADD_LINE_TOOL;
								parentFrame.setToolSelection(ADD_LINE_TOOL);
								drawPanel();
								break;
						}
					}
					else if(isCtrlPressed())
					{
						switch(e.getKeyCode())
						{
							case KeyEvent.VK_A:
								tool = SELECTION_TOOL;
								drawPanel();
								break;
						}
					}
				}
				return false;
			}
		});
	}

	public void build()
	{
		img = new BufferedImage(400, 800, BufferedImage.TYPE_INT_ARGB);
		drawPanel();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (img == null || img.getWidth() != getWidth() || img.getHeight() != getHeight())
		{
			img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
			drawPanel();
		}
		if (img != null)
		{
			g.drawImage(img, 0, 0, null);
		}
	}

	private void drawPanel()
	{
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

		g.setColor(config.primaryDark());
		g.fillRect(0, 0, img.getWidth(), img.getHeight());

		int panelWidth = getWidth();
		int panelHeight = getHeight();

		isHorizontalFlow = panelWidth > panelHeight;

		int scale = config.chartScaleSize();
		int margin = this.toolMargin;
		int oneAndHalfMargin = (int)(1.5 * margin);

		allTools.clear();

		// Build the tool icons area
		List<ToolItem> toolItems = new ArrayList<>();

		// Add primary and secondary tools
		toolItems.add(new ToolItem("PRIMARY", 2 * scale, 2 * scale));
		toolItems.add(new ToolItem("SECONDARY", 2 * scale, 2 * scale));

		// Add ChartTools
		for (ChartTool chartTool : tools) {
			toolItems.add(new ToolItem(chartTool, 2 * scale, 2 * scale));
		}

		// Build the attack icons area
		List<ToolItem> attackItems = new ArrayList<>();
		List<PlayerAnimation> filteredValues = Arrays.stream(PlayerAnimation.values())
			.filter(o->!excludedAnimations.contains(o))
			.collect(Collectors.toList());

		for (PlayerAnimation pa : filteredValues) {
			attackItems.add(new ToolItem(pa, scale, scale));
		}

		if (!isHorizontalFlow) {
			// Vertical layout
			int x = margin;
			int y = margin;

			// Lay out tool icons vertically in the first column
			for (ToolItem toolItem : toolItems) {
				toolItem.setBounds(x, y, toolItem.width, toolItem.height);
				allTools.add(toolItem);

				drawTool(g, toolItem.toolObj, x, y, toolItem.width, toolItem.height);

				y += toolItem.height + margin;
			}

			// y is now at the bottom of the last tool icon plus margin

			// Arrange attack icons in two columns below the tool icons
			int attackIconWidth = scale;
			int attackIconHeight = scale;

			int x1 = x;
			int x2 = x + attackIconWidth + margin;
			int yAttackStart = y;

			int availableHeight = panelHeight - yAttackStart - margin;

			int attackIconsPerColumn = (availableHeight + margin) / (attackIconHeight + margin);

			int attackIconIndex = 0;
			int numAttackIcons = attackItems.size();

			y = yAttackStart;
			for (int i = 0; i < attackIconsPerColumn && attackIconIndex < numAttackIcons; i++) {
				// Left column
				ToolItem attackItem = attackItems.get(attackIconIndex++);
				attackItem.setBounds(x1, y, attackIconWidth, attackIconHeight);
				allTools.add(attackItem);
				drawTool(g, attackItem.toolObj, x1, y, attackIconWidth, attackIconHeight);

				if (attackIconIndex < numAttackIcons) {
					// Right column
					attackItem = attackItems.get(attackIconIndex++);
					attackItem.setBounds(x2, y, attackIconWidth, attackIconHeight);
					allTools.add(attackItem);
					drawTool(g, attackItem.toolObj, x2, y, attackIconWidth, attackIconHeight);
				}

				y += attackIconHeight + margin;
			}

			// Arrange additional attack icons in single columns to the right
			x = x + 2 * (attackIconWidth + margin);

			while (attackIconIndex < numAttackIcons) {
				y = margin; // Reset y to top margin
				while (y + attackIconHeight + margin <= panelHeight && attackIconIndex < numAttackIcons) {
					ToolItem attackItem = attackItems.get(attackIconIndex++);
					attackItem.setBounds(x, y, attackIconWidth, attackIconHeight);
					allTools.add(attackItem);
					drawTool(g, attackItem.toolObj, x, y, attackIconWidth, attackIconHeight);

					y += attackIconHeight + margin;
				}
				x += attackIconWidth + margin;
			}
		} else {
			// Horizontal layout
			int x = margin;
			int y = margin;

			// Lay out tool icons horizontally in the first row
			for (ToolItem toolItem : toolItems) {
				toolItem.setBounds(x, y, toolItem.width, toolItem.height);
				allTools.add(toolItem);

				drawTool(g, toolItem.toolObj, x, y, toolItem.width, toolItem.height);

				x += toolItem.width + margin;
			}

			// x is now at the right of the last tool icon plus margin

			// Arrange attack icons in two rows to the right of the tool icons
			int attackIconWidth = scale;
			int attackIconHeight = scale;

			int xAttackStart = x;
			int y1 = y;
			int y2 = y + attackIconHeight + margin;

			int availableWidth = panelWidth - xAttackStart - margin;

			int attackIconsPerRow = (availableWidth + margin) / (attackIconWidth + margin);

			int attackIconIndex = 0;
			int numAttackIcons = attackItems.size();

			x = xAttackStart;
			for (int i = 0; i < attackIconsPerRow && attackIconIndex < numAttackIcons; i++) {
				// Top row
				ToolItem attackItem = attackItems.get(attackIconIndex++);
				attackItem.setBounds(x, y1, attackIconWidth, attackIconHeight);
				allTools.add(attackItem);
				drawTool(g, attackItem.toolObj, x, y1, attackIconWidth, attackIconHeight);

				if (attackIconIndex < numAttackIcons) {
					// Bottom row
					attackItem = attackItems.get(attackIconIndex++);
					attackItem.setBounds(x, y2, attackIconWidth, attackIconHeight);
					allTools.add(attackItem);
					drawTool(g, attackItem.toolObj, x, y2, attackIconWidth, attackIconHeight);
				}

				x += attackIconWidth + margin;
			}

			// Arrange additional attack icons in single rows below
			y = y + 2 * (attackIconHeight + margin);

			while (attackIconIndex < numAttackIcons) {
				x = margin; // Reset x to left margin
				while (x + attackIconWidth + margin <= panelWidth && attackIconIndex < numAttackIcons) {
					ToolItem attackItem = attackItems.get(attackIconIndex++);
					attackItem.setBounds(x, y, attackIconWidth, attackIconHeight);
					allTools.add(attackItem);
					drawTool(g, attackItem.toolObj, x, y, attackIconWidth, attackIconHeight);

					x += attackIconWidth + margin;
				}
				y += attackIconHeight + margin;
			}
		}

		// Draw selection box around primary/secondary tools
		Rectangle primaryBounds = null;
		Rectangle secondaryBounds = null;
		for (ToolItem toolItem : allTools) {
			if ("PRIMARY".equals(toolItem.toolObj)) {
				primaryBounds = toolItem.bounds;
			} else if ("SECONDARY".equals(toolItem.toolObj)) {
				secondaryBounds = toolItem.bounds;
			}
		}

		if (primaryBounds != null && secondaryBounds != null) {
			// Compute bounding rectangle
			Rectangle selectionBounds = primaryBounds.union(secondaryBounds);

			// Draw outline if selected or hovered
			if (tool == ADD_ATTACK_TOOL || hoveredTool == ADD_ATTACK_TOOL) {
				if (hoveredTool == ADD_ATTACK_TOOL) {
					g.setColor(config.fontColor());
				} else {
					g.setColor(new Color(45, 140, 235)); // Selection color
				}
				g.drawRect(selectionBounds.x - 1, selectionBounds.y - 1, selectionBounds.width + 1, selectionBounds.height + 1);
			}
		}

		repaint();
	}

	private void drawTool(Graphics2D g, Object toolObj, int xMargin, int yMargin, int width, int height)
	{
		if (toolObj instanceof String)
		{
			String toolStr = (String)toolObj;
			if (toolStr.equals("PRIMARY"))
			{
				// Draw primary button
				drawPrimarySecondaryTool(g, xMargin, yMargin, width, height, primary, true);
			}
			else if (toolStr.equals("SECONDARY"))
			{
				// Draw secondary button
				drawPrimarySecondaryTool(g, xMargin, yMargin, width, height, secondary, false);
			}
		}
		else if (toolObj instanceof ChartTool)
		{
			ChartTool chartTool = (ChartTool)toolObj;
			drawChartTool(g, xMargin, yMargin, width, height, chartTool);
		}
		else if (toolObj instanceof PlayerAnimation)
		{
			PlayerAnimation playerAnimation = (PlayerAnimation)toolObj;
			drawPlayerAnimationTool(g, xMargin, yMargin, width, height, playerAnimation);
		}
	}

	private void drawPrimarySecondaryTool(Graphics2D g, int xMargin, int yMargin, int width, int height, PlayerAnimation animation, boolean isPrimary)
	{
		g.setColor(config.boxColor());
		g.drawRoundRect(xMargin + 3, yMargin + 3, width - 5, height - 5, 10, 10);
		int textOffset = (width / 2) - (getStringWidth(g, animation.shorthand) / 2);
		g.setColor(config.fontColor());

		if(config.useIconsOnChart())
		{
			if(!animation.equals(PlayerAnimation.NOT_SET))
			{
				BufferedImage img = iconMap.get(animation);
				if(img != null)
				{
					BufferedImage scaled = getScaledImage(iconMap.get(animation), (width - 4), (height - 4));
					drawIcon(g, animation, xMargin, yMargin, scaled);
				}
			}
		}
		else
		{
			setColorAndDrawBoxAndText(g, width, xMargin, yMargin, textOffset, animation);
		}
	}

	private void drawChartTool(Graphics2D g, int xMargin, int yMargin, int width, int height, ChartTool chartTool)
	{
		g.setColor(config.boxColor());
		g.drawRoundRect(xMargin + 3, yMargin + 3, width - 5, height - 5, 10, 10);
		int textOffset = (width / 2) - (getStringWidth(g, chartTool.getName()) / 2);
		g.setColor(config.fontColor());
		g.drawString(chartTool.getName(), xMargin + textOffset - 1, yMargin + (getStringHeight(g) / 2) + (height / 2) + 1);

		if(tool == chartTool.getTool() || hoveredTool == chartTool.getTool())
		{
			if(hoveredTool == chartTool.getTool())
			{
				g.setColor(config.fontColor());
			}
			else
			{
				g.setColor(new Color(45, 140, 235));
			}
			g.drawRect(xMargin - 1, yMargin - 1, width + 1, height + 1);
		}
	}

	private void drawPlayerAnimationTool(Graphics2D g, int xMargin, int yMargin, int width, int height, PlayerAnimation playerAnimation)
	{
		g.setColor(playerAnimation.color);

		if(config.useIconsOnChart())
		{
			try
			{
				if(!playerAnimation.equals(PlayerAnimation.NOT_SET))
				{
					BufferedImage scaled = getScaledImage(iconMap.get(playerAnimation), (width - 2), (height - 2));
					drawIcon(g, playerAnimation, xMargin, yMargin, scaled);
				}
			}
			catch (Exception e)
			{

			}
		}
		else
		{
			g.fillRoundRect(xMargin + 2, yMargin + 2, width - 3, height - 3, 5, 5);

			g.setColor(config.fontColor());
			String letter = playerAnimation.shorthand;
			int textOffset = (width / 2) - (getStringWidth(g, letter) / 2);
			g.drawString(letter, xMargin + textOffset - 1, yMargin + (getStringHeight(g) / 2) + (height / 2) + 1);
		}

		// Draw hovered tool outline box
		if (playerAnimation.equals(hoveredAttack))
		{
			g.setColor(config.fontColor());
			g.drawRoundRect(xMargin - 1, yMargin - 1, width + 1, height + 1, 5, 5);
		}
	}

	private void setColorAndDrawBoxAndText(Graphics2D g, int width, int xMargin, int yMargin, int textOffset, PlayerAnimation animation)
	{
		g.setColor(animation.color);
		g.fillRoundRect(xMargin + 4, yMargin + 4, width - 6, width - 6, 10, 10);
		g.drawString(animation.shorthand, xMargin + textOffset - 1, yMargin + (getStringHeight(g) / 2) + (width / 2) + 1);
	}

	private void drawIcon(Graphics2D g, PlayerAnimation playerAnimation, int xOffset, int yOffset, BufferedImage scaled)
	{
		if (playerAnimation.shouldFlip)
		{
			g.drawImage(createFlipped(createDropShadow(scaled)), xOffset + 3, yOffset + 3, null);
			g.drawImage(createFlipped(scaled), xOffset + 2, yOffset + 1, null);
		} else
		{
			g.drawImage(createDropShadow(scaled), xOffset + 3, yOffset + 3, null);
			g.drawImage(scaled, xOffset + 2, yOffset + 1, null);
		}
	}

	private void setHoveredTool(int x, int y)
	{
		hoveredTool = NO_TOOL;
		hoveredAttack = PlayerAnimation.EXCLUDED_ANIMATION;

		for (ToolItem toolItem : allTools) {
			if (toolItem.bounds.contains(x, y)) {
				if (toolItem.toolObj instanceof String) {
					String toolStr = (String)toolItem.toolObj;
					if (toolStr.equals("PRIMARY") || toolStr.equals("SECONDARY")) {
						hoveredTool = ADD_ATTACK_TOOL;
					}
				} else if (toolItem.toolObj instanceof ChartTool) {
					ChartTool chartTool = (ChartTool)toolItem.toolObj;
					hoveredTool = chartTool.getTool();
				} else if (toolItem.toolObj instanceof PlayerAnimation) {
					hoveredAttack = (PlayerAnimation)toolItem.toolObj;
				}
				break;
			}
		}
	}

	public void setTool(int tool)
	{
		this.tool = tool;
		drawPanel();
	}

	private void handleRelease(MouseEvent e)
	{
		requestFocus();
		if (hoveredAttack.equals(PlayerAnimation.EXCLUDED_ANIMATION))
		{
			if (SwingUtilities.isLeftMouseButton(e))
			{
				tool = hoveredTool;
				parentFrame.setToolSelection(hoveredTool);
				if (tool == ADD_TEXT_TOOL)
				{
					setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
				}
				else
				{
					setCursor(Cursor.getDefaultCursor());
				}
				drawPanel();
			}
			return;
		}
		if (SwingUtilities.isLeftMouseButton(e))
		{
			primary = hoveredAttack;
			parentFrame.setPrimaryTool(primary);
		} else if (SwingUtilities.isRightMouseButton(e))
		{
			secondary = hoveredAttack;
			parentFrame.setSecondaryTool(secondary);
		}
		tool = ADD_ATTACK_TOOL;
		parentFrame.setToolSelection(ADD_ATTACK_TOOL);
		drawPanel();
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		handleRelease(e);
	}

	@Override
	public void mousePressed(MouseEvent e)
	{

	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		handleRelease(e);
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{

	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		hoveredTool = NO_TOOL;
		hoveredAttack = PlayerAnimation.EXCLUDED_ANIMATION;
		drawPanel();
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{

	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		setHoveredTool(e.getX(), e.getY());
		drawPanel();
	}

	// Additional class to hold tool information with sizes and positions
	class ToolItem {
		Object toolObj;
		int width;
		int height;
		Rectangle bounds;

		public ToolItem(Object toolObj, int width, int height) {
			this.toolObj = toolObj;
			this.width = width;
			this.height = height;
		}

		public void setBounds(int x, int y, int width, int height) {
			this.bounds = new Rectangle(x, y, width, height);
			this.width = width;
			this.height = height;
		}
	}
}

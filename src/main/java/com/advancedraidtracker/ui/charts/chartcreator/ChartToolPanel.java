package com.advancedraidtracker.ui.charts.chartcreator;

import com.advancedraidtracker.AdvancedRaidTrackerConfig;
import com.advancedraidtracker.ui.charts.ChartPanel;
import com.advancedraidtracker.utility.weapons.PlayerAnimation;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
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
    int initialXMargin = 5;
    int initialYMargin;
    int toolsPerColumn;

    Map<PlayerAnimation, BufferedImage> iconMap = new HashMap<>();
    private final Set<PlayerAnimation> excludedAnimations = new HashSet<>(Arrays.asList(PlayerAnimation.UNARMED, PlayerAnimation.UNDECIDED, PlayerAnimation.EXCLUDED_ANIMATION, PlayerAnimation.ACCURSED_SCEPTRE_AUTO, PlayerAnimation.ACCURSED_SCEPTRE_SPEC, PlayerAnimation.COLOSSEUM_AUTO, PlayerAnimation.COLOSSEUM_SPECIAL, PlayerAnimation.KERIS_SUN_SPEC));

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
		// Get panel dimensions
		int panelWidth = getWidth();
		int panelHeight = getHeight();

		// Determine layout orientation
		boolean verticalLayout = panelHeight > panelWidth;

		// Recreate the image with the current size
		if(panelWidth <= 0 || panelHeight <= 0)
		{
			return;
		}
		img = new BufferedImage(panelWidth, panelHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) img.getGraphics();

		// Set rendering hints (smooth graphics)
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		RenderingHints qualityHints = new RenderingHints(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		qualityHints.put(
			RenderingHints.KEY_RENDERING,
			RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHints(qualityHints);

		// Fill background
		g.setColor(config.primaryDark());
		g.fillRect(0, 0, panelWidth, panelHeight);

		// Initial margins and sizes
		int initialXMargin = 5;
		int initialYMargin = 5;
		int toolMargin = 10;
		int toolHeight = config.chartScaleSize();
		int toolButtonWidth = toolHeight * 2;
		int toolButtonHeight = toolHeight * 2;
		int attackIconWidth = toolHeight;
		int attackIconHeight = toolHeight;
		int attackMargin = toolMargin / 2;

		// Positions for primary and secondary selections
		int xPosition, yPosition;

		// Handle vertical and horizontal layouts separately
		if (verticalLayout) {
			// Vertical Layout

			// Draw primary and secondary selections at the top
			int xPrimary = initialXMargin;
			int yPrimary = initialYMargin;

			// Draw primary selection
			drawSelectionBox(g, primary, xPrimary, yPrimary, toolButtonWidth, toolButtonHeight, ADD_ATTACK_TOOL);

			// Draw secondary selection
			int xSecondary = xPrimary + toolButtonWidth + toolMargin;
			drawSelectionBox(g, secondary, xSecondary, yPrimary, toolButtonWidth, toolButtonHeight, ADD_ATTACK_TOOL);

			// Starting position for tools
			int yToolPosition = yPrimary + toolButtonHeight + toolMargin;
			int numToolButtons = tools.size();
			int availableWidth = panelWidth - 2 * initialXMargin;
			int toolButtonWidthWithMargin = toolButtonWidth + toolMargin;
			int numToolColumns = Math.max(1, availableWidth / toolButtonWidthWithMargin);
			int numToolRows = (int) Math.ceil((double) numToolButtons / numToolColumns);

			// Draw tool buttons
			for (int i = 0; i < numToolButtons; i++) {
				int col = i % numToolColumns;
				int row = i / numToolColumns;
				xPosition = initialXMargin + col * toolButtonWidthWithMargin;
				yPosition = yToolPosition + row * (toolButtonHeight + toolMargin);

				if (yPosition > panelHeight) {
					break; // Outside panel, stop drawing further
				}

				ChartTool chartTool = tools.get(i);
				drawToolButton(g, chartTool, xPosition, yPosition, toolButtonWidth, toolButtonHeight);
			}

			// Starting position for attacks/icons
			int yAttacksStart = yToolPosition + numToolRows * (toolButtonHeight + toolMargin) + toolMargin;
			List<PlayerAnimation> filteredValues = getFilteredPlayerAnimations();
			int numAttackIcons = filteredValues.size();
			int attackIconWidthWithMargin = attackIconWidth + attackMargin;
			int numAttackColumns = Math.max(1, availableWidth / attackIconWidthWithMargin);
			int numAttackRows = (int) Math.ceil((double) numAttackIcons / numAttackColumns);

			// Draw attack icon buttons
			for (int i = 0; i < numAttackIcons; i++) {
				int col = i % numAttackColumns;
				int row = i / numAttackColumns;
				xPosition = initialXMargin + col * attackIconWidthWithMargin;
				yPosition = yAttacksStart + row * (attackIconHeight + attackMargin);

				if (yPosition > panelHeight) {
					break; // Clipped
				}

				PlayerAnimation playerAnimation = filteredValues.get(i);
				drawAttackIcon(g, playerAnimation, xPosition, yPosition, attackIconWidth, attackIconHeight);
			}
		} else {
			// Horizontal Layout

			// Draw primary and secondary selections at the left
			int xPrimary = initialXMargin;
			int yPrimary = initialYMargin;

			// Draw primary selection
			drawSelectionBox(g, primary, xPrimary, yPrimary, toolButtonWidth, toolButtonHeight, ADD_ATTACK_TOOL);

			// Draw secondary selection
			int ySecondary = yPrimary + toolButtonHeight + toolMargin;
			drawSelectionBox(g, secondary, xPrimary, ySecondary, toolButtonWidth, toolButtonHeight, ADD_ATTACK_TOOL);

			// Starting position for tools
			int xToolPosition = xPrimary + toolButtonWidth + toolMargin;
			int numToolButtons = tools.size();
			int availableHeight = panelHeight - 2 * initialYMargin;
			int toolButtonHeightWithMargin = toolButtonHeight + toolMargin;
			int numToolRows = Math.max(1, availableHeight / toolButtonHeightWithMargin);
			int numToolColumns = (int) Math.ceil((double) numToolButtons / numToolRows);

			// Draw tool buttons
			for (int i = 0; i < numToolButtons; i++) {
				int col = i / numToolRows;
				int row = i % numToolRows;
				xPosition = xToolPosition + col * (toolButtonWidth + toolMargin);
				yPosition = initialYMargin + row * toolButtonHeightWithMargin;

				if (xPosition > panelWidth) {
					break; // Outside panel, stop drawing further
				}

				ChartTool chartTool = tools.get(i);
				drawToolButton(g, chartTool, xPosition, yPosition, toolButtonWidth, toolButtonHeight);
			}

			// Starting position for attacks/icons
			int xAttacksStart = xToolPosition + numToolColumns * (toolButtonWidth + toolMargin) + toolMargin;
			List<PlayerAnimation> filteredValues = getFilteredPlayerAnimations();
			int numAttackIcons = filteredValues.size();
			int attackIconHeightWithMargin = attackIconHeight + attackMargin;
			int numAttackRows = Math.max(1, availableHeight / attackIconHeightWithMargin);
			int numAttackColumns = (int) Math.ceil((double) numAttackIcons / numAttackRows);

			// Draw attack icon buttons
			for (int i = 0; i < numAttackIcons; i++) {
				int col = i % numAttackColumns;
				int row = i / numAttackColumns;
				xPosition = xAttacksStart + col * (attackIconWidth + attackMargin);
				yPosition = initialYMargin + row * attackIconHeightWithMargin;

				if (xPosition > panelWidth) {
					break; // Clipped
				}

				PlayerAnimation playerAnimation = filteredValues.get(i);
				drawAttackIcon(g, playerAnimation, xPosition, yPosition, attackIconWidth, attackIconHeight);
			}
		}

		// Repaint the panel
		repaint();
	}

// Helper methods used in drawPanel()

	private void drawSelectionBox(Graphics2D g, PlayerAnimation animation, int x, int y, int width, int height, int toolType) {
		// Draw outer rectangle to indicate active tool
		if (tool == toolType || hoveredTool == toolType) {
			if (hoveredTool == toolType) {
				g.setColor(config.fontColor());
			} else {
				g.setColor(new Color(45, 140, 235)); // active selection color
			}
			g.drawRect(x + 1, y + 1, width - 2, height - 2);
		}

		// Draw box border
		g.setColor(config.boxColor());
		g.drawRoundRect(x + 3, y + 3, width - 5, height - 5, 10, 10);

		// Draw content
		if (config.useIconsOnChart()) {
			if (!animation.equals(PlayerAnimation.NOT_SET)) {
				BufferedImage imgIcon = iconMap.get(animation);
				if (imgIcon != null) {
					BufferedImage scaled = getScaledImage(imgIcon, width - 6, height - 6);
					drawIcon(g, animation, x, y, scaled);
				}
			}
		} else {
			g.setColor(animation.color);
			g.fillRoundRect(x + 4, y + 4, width - 6, height - 6, 10, 10);
			String shorthand = animation.shorthand;
			int textOffset = (width / 2) - (getStringWidth(g, shorthand, g.getFont()) / 2);
			g.setColor(config.fontColor());
			g.drawString(shorthand, x + textOffset - 1, y + (getStringHeight(g.getFont()) / 2) + (height / 2) + 1);
		}
	}

	private void drawToolButton(Graphics2D g, ChartTool chartTool, int x, int y, int width, int height) {
		// Draw the background rectangle
		g.setColor(config.boxColor());
		g.drawRoundRect(x + 3, y + 3, width - 5, height - 5, 10, 10);

		// Draw tool name
		String name = chartTool.getName();
		int textOffset = (width / 2) - (getStringWidth(g, name, g.getFont()) / 2);
		g.setColor(config.fontColor());
		g.drawString(name, x + textOffset - 1, y + (getStringHeight(g.getFont()) / 2) + (height / 2) + 1);

		// Draw selection or hover outline
		if (tool == chartTool.getTool() || hoveredTool == chartTool.getTool()) {
			if (hoveredTool == chartTool.getTool()) {
				g.setColor(config.fontColor());
			} else {
				g.setColor(new Color(45, 140, 235));
			}
			g.drawRect(x, y, width, height);
		}
	}

	private void drawAttackIcon(Graphics2D g, PlayerAnimation playerAnimation, int x, int y, int width, int height) {
		// Draw the icon or shorthand
		if (config.useIconsOnChart()) {
			if (!playerAnimation.equals(PlayerAnimation.NOT_SET)) {
				BufferedImage imgIcon = iconMap.get(playerAnimation);
				if (imgIcon != null) {
					BufferedImage scaled = getScaledImage(imgIcon, width - 2, height - 2);
					drawIcon(g, playerAnimation, x, y, scaled);
				}
			}
		} else {
			g.setColor(playerAnimation.color);
			g.fillRoundRect(x + 2, y + 2, width - 3, height - 3, 5, 5);
			String shorthand = playerAnimation.shorthand;
			int textOffset = (width / 2) - (getStringWidth(g, shorthand, g.getFont()) / 2);
			g.setColor(config.fontColor());
			g.drawString(shorthand, x + textOffset - 1, y + (getStringHeight(g.getFont()) / 2) + (height / 2) + 1);
		}

		// Draw hover outline
		if (hoveredAttack.equals(playerAnimation)) {
			g.setColor(config.fontColor());
			g.drawRoundRect(x + 1, y + 1, width - 2, height - 2, 5, 5);
		}
	}

	private void drawIcon(Graphics2D g, PlayerAnimation playerAnimation, int x, int y, BufferedImage scaled) {
		if (playerAnimation.shouldFlip) {
			g.drawImage(createFlipped(createDropShadow(scaled)), x + 3, y + 3, null);
			g.drawImage(createFlipped(scaled), x + 2, y + 1, null);
		} else {
			g.drawImage(createDropShadow(scaled), x + 3, y + 3, null);
			g.drawImage(scaled, x + 2, y + 1, null);
		}
	}

// Utility methods

	private int getStringWidth(Graphics2D g, String text, Font font) {
		FontMetrics metrics = g.getFontMetrics(font);
		return metrics.stringWidth(text);
	}

	private int getStringHeight(Font font) {
		FontMetrics metrics = getFontMetrics(font);
		return metrics.getHeight();
	}

	private BufferedImage getScaledImage(BufferedImage srcImg, int w, int h) {
		Image tmp = srcImg.getScaledInstance(w, h, Image.SCALE_SMOOTH);
		BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = resized.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();
		return resized;
	}

	private BufferedImage createFlipped(BufferedImage image) {
		AffineTransform at = AffineTransform.getScaleInstance(-1, 1);
		at.translate(-image.getWidth(), 0);
		AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		return op.filter(image, null);
	}

	private BufferedImage createDropShadow(BufferedImage image) {
		// Implement drop shadow effect as per your requirements
		// Placeholder implementation
		BufferedImage shadow = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = shadow.createGraphics();
		g2d.drawImage(image, 0, 0, null);
		g2d.dispose();
		return shadow;
	}

	private List<PlayerAnimation> getFilteredPlayerAnimations() {
		return Arrays.stream(PlayerAnimation.values())
			.filter(o -> !excludedAnimations.contains(o))
			.collect(Collectors.toList());
	}

	private void setColorAndDrawBoxAndText(Graphics2D g, int toolHeight, int xMargin, int yMargin, int textOffset, PlayerAnimation primary)
	{
		g.setColor(primary.color);
		g.fillRoundRect(xMargin + 4, yMargin + 4, toolHeight * 2 - 6, toolHeight * 2 - 6, 10, 10);
		g.drawString(primary.shorthand, xMargin + textOffset - 1, yMargin + (getStringHeight(g.getFont()) / 2) + (toolHeight) + 1);
	}

	private void setHoveredTool(int x, int y)
    {
        if (y > 16 && y < (16 + (config.chartScaleSize() * 2)))
        {
            if (x > 6 && x < (6 + toolMargin + (4 * config.chartScaleSize())))
            {
                hoveredTool = ADD_ATTACK_TOOL;
                return;
            }
        }
        int index = 2;
        int yIndex = 0;
        for(ChartTool chartTool : tools)
        {
            if(y > (16+((config.chartScaleSize()*2+toolMargin)*yIndex)) && y < (config.chartScaleSize() * 2 + 16+((config.chartScaleSize()*2+toolMargin)*yIndex)))
            {
                if (x > (6 + (((2 * index)) * config.chartScaleSize()) + (((index)) * toolMargin)) && x < (6 + ((2 + (2 * index)) * config.chartScaleSize()) + ((index) * toolMargin)))
                {
                    hoveredTool = chartTool.getTool();
                    return;
                }
            }
            index++;
            if(index%3 == 0)
            {
                index = 0;
                yIndex++;
            }
        }
        hoveredTool = NO_TOOL;
    }

	public void setTool(int tool)
	{
		this.tool = tool;
		drawPanel();
	}

    private void setHoveredAttack(int x, int y)
    {
        int numberOfModes = 2 + tools.size();
        int modeRows = ((numberOfModes-1)/3);
        int yStart = initialYMargin + (modeRows*(config.chartScaleSize()*2+toolMargin));
        int gap = config.chartScaleSize() + (toolMargin / 2);
        if (x > 5 && y > (yStart-initialYMargin+15) + (config.chartScaleSize() * 2) + (toolMargin / 2))
        {
            int positionInRow = (x - initialXMargin) / gap;
            int positionInColumn = (y - yStart) / gap;
            int index = positionInRow * toolsPerColumn + positionInColumn;
            List<PlayerAnimation> filteredValues = Arrays.stream(PlayerAnimation.values()).filter(o->!excludedAnimations.contains(o)).collect(Collectors.toList());

            if (index >= 0 && index < filteredValues.size())
            {
                hoveredAttack = filteredValues.get(index);
            }
        } else
        {
            hoveredAttack = PlayerAnimation.EXCLUDED_ANIMATION;
        }
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
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {

    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        setHoveredAttack(e.getX(), e.getY());
        setHoveredTool(e.getX(), e.getY());
        drawPanel();
    }
}

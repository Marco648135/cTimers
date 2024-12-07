package com.advancedraidtracker.ui.setups;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;

import com.advancedraidtracker.utility.UISwingUtility;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedMenu;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedMenuItem;

import static com.advancedraidtracker.utility.UISwingUtility.getThemedSeperator;
import static com.advancedraidtracker.utility.datautility.DataWriter.PLUGIN_DIRECTORY;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

enum BoxType
{
	INVENTORY,
	RUNEPOUCH,
	EQUIPMENT
}

@Slf4j
class DragState
{
	public static boolean dragging = false;
	public static PixelBox dragSourceBox = null;
	public static int dragItemId = -1;
	public static boolean ctrlPressed = false;
	public static boolean rmb = false;
	public static List<PixelBox> affectedBoxes = new ArrayList<>();

	public static void startDrag(PixelBox sourceBox, boolean isCtrlPressed, boolean isRmb)
	{
		dragging = true;
		dragSourceBox = sourceBox;
		ctrlPressed = isCtrlPressed;
		rmb = isRmb;
		affectedBoxes.clear();

		if (sourceBox.getId() != -1)
		{
			dragItemId = sourceBox.getId();
		}
		else
		{
			dragItemId = sourceBox.getSetupsWindow().getSelectedItem();
		}
	}

	public static void stopDrag()
	{
		dragging = false;
		dragSourceBox = null;
		dragItemId = -1;
		ctrlPressed = false;
		clearAffectedBoxes();
	}

	public static void clearAffectedBoxes()
	{
		for (PixelBox box : affectedBoxes)
		{
			box.resetPreview();
		}
		affectedBoxes.clear();
	}

	public static void addAffectedBox(PixelBox box)
	{
		if (!affectedBoxes.contains(box))
		{
			affectedBoxes.add(box);
		}
	}

	public static void removeAffectedBox(PixelBox box)
	{
		for (PixelBox matchingBox : affectedBoxes)
		{
			matchingBox.resetPreview();
		}
		affectedBoxes.remove(box);
	}

	public static void setAffectedBoxes(List<PixelBox> currentAffectedBoxes)
	{
		affectedBoxes = currentAffectedBoxes;
	}

	public static List<PixelBox> getAffectedBoxes()
	{
		return affectedBoxes;
	}
}

@Slf4j
class PixelBox extends JButton
{
	private int id;
	private ItemManager itemManager;
	private SetupsWindow setupsWindow;
	BoxType boxType;

	public int gridRow;
	public int gridCol;
	public GridPanel parentGridPanel;

	private Point mousePressPoint;
	private boolean skipPaintOverride = false;
	boolean isPreviewing = false;
	private boolean isHoveringSubBox = false;
	private final Rectangle subBoxBounds = new Rectangle(getWidth() - 15, 5, 10, 10);
	private JPopupMenu popupMenu;
	private JTextField inputField;
	private JList<Trie.Entry> suggestionList;
	private DefaultListModel<Trie.Entry> listModel;


	public PixelBox(ItemManager itemManager, SetupsWindow setupsWindow, BoxType boxType, GridPanel parentGridPanel, int gridRow, int gridCol)
	{
		this.id = -1;
		this.itemManager = itemManager;
		this.setupsWindow = setupsWindow;
		this.boxType = boxType;
		this.parentGridPanel = parentGridPanel;
		this.gridRow = gridRow;
		this.gridCol = gridCol;

		setPreferredSize(new Dimension(40, 40));
		setMargin(new Insets(0, 0, 0, 0));
		setBackground(config != null ? config.primaryDark() : Color.DARK_GRAY);
		setOpaque(true);

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (subBoxBounds.contains(e.getPoint()))
				{
					showAutocompletePopup();
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (!skipPaintOverride)
				{
					mousePressPoint = e.getPoint();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (skipPaintOverride)
				{
					return;
				}
				if (DragState.dragging)
				{
					parentGridPanel.handleMouseReleased(e);
				}
				else
				{
					if (SwingUtilities.isLeftMouseButton(e))
					{
						onBoxClicked();
					}
					else if (SwingUtilities.isRightMouseButton(e))
					{
						clearItem();
					}
				}
				DragState.stopDrag();
				SetupsWindow.getInstance().hideDragImage();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				onHover();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				isHoveringSubBox = false;
				if (!DragState.dragging)
				{
					resetPreview();
				}
			}
		});

		addMouseMotionListener(new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				if (skipPaintOverride)
				{
					return;
				}
				if (subBoxBounds.contains(e.getPoint()))
				{
					if (!isHoveringSubBox)
					{
						isHoveringSubBox = true;
						setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
						repaint(subBoxBounds);
					}
				}
				else
				{
					if (isHoveringSubBox)
					{
						isHoveringSubBox = false;
						setCursor(Cursor.getDefaultCursor());
						repaint(subBoxBounds);
					}
				}
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (skipPaintOverride)
				{
					return;
				}
				int dx = Math.abs(e.getX() - mousePressPoint.x);
				int dy = Math.abs(e.getY() - mousePressPoint.y);
				int DRAG_THRESHOLD = 5;
				if (dx > DRAG_THRESHOLD || dy > DRAG_THRESHOLD)
				{
					if (!DragState.dragging)
					{
						boolean ctrlPressed = e.isControlDown();
						if (id != -1 || setupsWindow.getSelectedItem() != -1)
						{
							DragState.startDrag(PixelBox.this, ctrlPressed, SwingUtilities.isRightMouseButton(e));
							if (id != -1)
							{
								AsyncBufferedImage itemImage = itemManager.getImage(id);
								itemImage.onLoaded(() -> {
									BufferedImage dragImage = createTransparentImage(itemImage, 0.5f);
									SwingUtilities.invokeLater(() -> {
										SetupsWindow.getInstance().showDragImage(dragImage, e.getLocationOnScreen());
									});
								});
							}
						}
					}
					if (DragState.dragging)
					{
						parentGridPanel.handleMouseDragged(e);
						SetupsWindow.getInstance().updateDragImagePosition(e.getXOnScreen(), e.getYOnScreen());

						Point translatedPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), parentGridPanel);
						PixelBox targetBox = parentGridPanel.getPixelBoxAt(translatedPoint);
						SetupsWindow.getInstance().setTargetBox(targetBox);
					}
				}
			}
		});
	}

	private void showAutocompletePopup()
	{
		if (popupMenu != null && popupMenu.isVisible())
		{
			return;
		}

		popupMenu = new JPopupMenu();
		popupMenu.setBorder(BorderFactory.createLineBorder(config.boxColor()));

		inputField = new JTextField();
		inputField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		listModel = new DefaultListModel<>();
		suggestionList = new JList<>(listModel);
		suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		suggestionList.setCellRenderer(new SuggestionListRenderer(itemManager));

		inputField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{
				String input = inputField.getText().trim();
				updateSuggestions(input);

				if (e.getKeyCode() == KeyEvent.VK_DOWN)
				{
					suggestionList.requestFocusInWindow();
					suggestionList.setSelectedIndex(0);
				}
				else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					popupMenu.setVisible(false);
				}
			}
		});

		suggestionList.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					selectSuggestedItem();
				}
				else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					popupMenu.setVisible(false);
				}
			}
		});

		suggestionList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					selectSuggestedItem();
				}
			}
		});

		popupMenu.setLayout(new BorderLayout());
		popupMenu.add(inputField, BorderLayout.NORTH);
		popupMenu.add(new JScrollPane(suggestionList), BorderLayout.CENTER);

		popupMenu.setPreferredSize(new Dimension(200, 340));

		Point location = this.getLocationOnScreen();
		popupMenu.show(this, subBoxBounds.x - 160, subBoxBounds.y + subBoxBounds.height);

		inputField.requestFocusInWindow();
	}

	private void updateSuggestions(String input)
	{
		listModel.clear();

		if (input.isEmpty())
		{
			return;
		}

		List<Trie.Entry> suggestions = ItemParser.getItemTrie().getSuggestions(input, 10);
		log.info("Requesting suggestions for: " + input);
		for (Trie.Entry entry : suggestions)
		{
			log.info(entry.getName() + ", " + entry.getId());
			listModel.addElement(entry);
		}
	}

	private void selectSuggestedItem()
	{
		Trie.Entry selectedEntry = suggestionList.getSelectedValue();
		if (selectedEntry != null)
		{
			setId(selectedEntry.getId());
		}
		popupMenu.setVisible(false);
	}


	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		if (skipPaintOverride)
		{
			return;
		}
		if (id != -1)
		{
			return;
		}
		Graphics2D g2d = (Graphics2D) g.create();

		subBoxBounds.setBounds(getWidth() - 12, 1, 10, 10);

		if (isHoveringSubBox)
		{
			g2d.setColor(config.boxColor());
		}
		else
		{
			g2d.setColor(config.primaryMiddle());
		}
		g2d.fillRect(subBoxBounds.x, subBoxBounds.y, subBoxBounds.width, subBoxBounds.height);

		g2d.setColor(config.primaryLight());
		g2d.drawRect(subBoxBounds.x, subBoxBounds.y, subBoxBounds.width, subBoxBounds.height);

		if (isHoveringSubBox)
		{
			g2d.setColor(config.fontColor());
		}
		else
		{
			g2d.setColor(config.boxColor());
		}
		FontMetrics fm = g2d.getFontMetrics();
		String plus = "+";
		int plusWidth = fm.stringWidth(plus);
		int plusHeight = fm.getAscent();
		int plusX = 5 + subBoxBounds.x - plusWidth / 2;
		int plusY = 5 + subBoxBounds.y + plusHeight / 2;
		g2d.drawString(plus, plusX, plusY);

		g2d.dispose();
	}

	public SetupsWindow getSetupsWindow()
	{
		return setupsWindow;
	}

	public void onHover()
	{
		if (!DragState.dragging && id == -1)
		{
			int selectedItemId = setupsWindow.getSelectedItem();
			if (selectedItemId != -1)
			{
				isPreviewing = true;
				showPreview(selectedItemId);
			}
		}
	}

	public void showPreview(int itemId)
	{
		AsyncBufferedImage itemImage = itemManager.getImage(itemId);
		itemImage.onLoaded(() -> {
			BufferedImage image = itemImage;
			BufferedImage transparentImage = createTransparentImage(image, 0.5f);
			SwingUtilities.invokeLater(() -> {
				if (isPreviewing)
				{
					setIcon(new ImageIcon(transparentImage));
				}
				repaint();
			});
		});
	}

	public void resetPreview()
	{
		if (id != -1)
		{
			setId(id);
		}
		else
		{
			isPreviewing = false;
			setIcon(null);
		}
	}

	private void onBoxClicked()
	{
		int selectedItemId = setupsWindow.getSelectedItem();
		if (selectedItemId != -1)
		{
			setId(selectedItemId);
		}
	}

	private void clearItem()
	{
		setId(-1);
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
		setupsWindow.addItemToRecent(id);
		setupsWindow.pushItemChanges();
		if (id != -1)
		{
			AsyncBufferedImage itemImage = itemManager.getImage(id);
			itemImage.onLoaded(() -> {
				BufferedImage image = itemImage;
				SwingUtilities.invokeLater(() -> {
					setIcon(new ImageIcon(image));
					repaint();
				});
			});
		}
		else
		{
			SwingUtilities.invokeLater(() -> {
				setIcon(null);
				repaint();
			});
		}
	}

	private BufferedImage createTransparentImage(BufferedImage image, float opacity)
	{
		BufferedImage transparentImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = transparentImage.createGraphics();
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
		g2d.drawImage(image, 0, 0, null);
		g2d.dispose();
		return transparentImage;
	}

	public void setSkipPaintComponent(boolean b)
	{
		skipPaintOverride = b;
	}
}

@Slf4j
class GridPanel extends JPanel
{
	public int rows;
	public int cols;
	public PixelBox[][] boxes;
	private ItemManager itemManager;
	private SetupsWindow setupsWindow;
	private BoxType boxType;

	public GridPanel(int rows, int cols, ItemManager itemManager, SetupsWindow setupsWindow, BoxType boxType)
	{
		this.rows = rows;
		this.cols = cols;
		this.itemManager = itemManager;
		this.setupsWindow = setupsWindow;
		this.boxType = boxType;
		this.boxes = new PixelBox[rows][cols];
		setBackground(config.primaryMiddle());
		setOpaque(true);
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(1, 1, 1, 1);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;

		boolean[][] cellExists = null;
		if (boxType == BoxType.EQUIPMENT)
		{
			cellExists = new boolean[rows][cols];
			for (int r = 0; r < rows; r++)
			{
				for (int c = 0; c < cols; c++)
				{
					cellExists[r][c] = true;
				}
			}
			// 5x3 grid but we skip empty spots cause inv isnt fully 5x3
			cellExists[0][0] = false;
			cellExists[3][0] = false;
			cellExists[3][2] = false;
		}

		for (int r = 0; r < rows; r++)
		{
			for (int c = 0; c < cols; c++)
			{
				if (boxType == BoxType.EQUIPMENT && cellExists != null && !cellExists[r][c])
				{
					continue;
				}

				gbc.gridx = c;
				gbc.gridy = r;

				PixelBox box = new PixelBox(itemManager, setupsWindow, boxType, this, r, c);
				boxes[r][c] = box;
				add(box, gbc);
			}
		}
	}

	public void updateBoxSize(int boxSize)
	{
		for (int r = 0; r < rows; r++)
		{
			for (int c = 0; c < cols; c++)
			{
				if (boxes[r][c] != null)
				{
					boxes[r][c].setPreferredSize(new Dimension(boxSize, boxSize));
					boxes[r][c].setMaximumSize(new Dimension(boxSize, boxSize));
					boxes[r][c].setMinimumSize(new Dimension(boxSize, boxSize));
				}
			}
		}
		revalidate();
		repaint();
	}

	private void revertDrag()
	{
		PixelBox sourceBox = DragState.dragSourceBox;
		sourceBox.setId(DragState.dragItemId);
	}

	public void handleMouseDragged(MouseEvent e)
	{
		Point gridPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), this);
		PixelBox targetBox = getPixelBoxAt(gridPoint);
		boolean rmb = SwingUtilities.isRightMouseButton(e);

		if (targetBox != null)
		{
			if (DragState.ctrlPressed && DragState.dragSourceBox.getId() != -1)
			{
				showCtrlDragPreview(targetBox);
			}
			else if (DragState.dragSourceBox.getId() != -1 && !rmb)
			{
				DragState.clearAffectedBoxes();
				DragState.addAffectedBox(targetBox);
				targetBox.showPreview(DragState.dragItemId);
			}
			else
			{
				calculateMassFillAffectedBoxes(targetBox, rmb);
			}
		}

		SetupsWindow.getInstance().updateDragImagePosition(e.getXOnScreen(), e.getYOnScreen());
	}

	public void handleMouseReleased(MouseEvent e)
	{
		Point gridPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), this);
		PixelBox targetBox = getPixelBoxAt(gridPoint);

		boolean rmb = SwingUtilities.isRightMouseButton(e);

		if (DragState.ctrlPressed && DragState.dragSourceBox.getId() != -1)
		{
			performCtrlDragCopy(targetBox);
		}
		else if (DragState.dragSourceBox.getId() != -1 && !rmb)
		{
			if (targetBox != null && isValidDrop(targetBox))
			{
				performItemMove(targetBox);
			}
			else
			{
				revertDrag();
			}
		}
		else
		{
			performMassFill(rmb);
		}

		DragState.clearAffectedBoxes();
		DragState.stopDrag();
		SetupsWindow.getInstance().hideDragImage();
	}

	private boolean isValidDrop(PixelBox targetBox)
	{
		return targetBox.getId() == -1;
	}

	PixelBox getPixelBoxAt(Point point)
	{
		Component comp = SwingUtilities.getDeepestComponentAt(this, point.x, point.y);
		if (comp instanceof PixelBox)
		{
			return (PixelBox) comp;
		}
		return null;
	}

	private void calculateMassFillAffectedBoxes(PixelBox targetBox, boolean rmb)
	{
		PixelBox sourceBox = DragState.dragSourceBox;
		if (sourceBox.parentGridPanel == targetBox.parentGridPanel)
		{
			GridPanel grid = sourceBox.parentGridPanel;
			int startRow = sourceBox.gridRow;
			int startCol = sourceBox.gridCol;
			int endRow = targetBox.gridRow;
			int endCol = targetBox.gridCol;

			int rowIncrement = (endRow >= startRow) ? 1 : -1;
			int colIncrement = (endCol >= startCol) ? 1 : -1;

			List<PixelBox> currentAffectedBoxes = new ArrayList<>();

			for (int r = startRow; r != endRow + rowIncrement; r += rowIncrement)
			{
				int cStart = (r == startRow) ? startCol : (colIncrement > 0 ? 0 : grid.cols - 1);
				int cEnd = (r == endRow) ? endCol : (colIncrement > 0 ? grid.cols - 1 : 0);

				for (int c = cStart; c != cEnd + colIncrement; c += colIncrement)
				{
					PixelBox box = grid.boxes[r][c];
					if (rmb)
					{
						if (box.getId() != -1)
						{
							box.isPreviewing = true;
							box.showPreview(box.getId());
						}
					}
					else if (box.getId() == -1 && !box.isPreviewing)
					{
						box.isPreviewing = true;
						box.showPreview(DragState.dragItemId);
					}
					currentAffectedBoxes.add(box);
				}
			}

			for (PixelBox box : DragState.getAffectedBoxes())
			{
				if (!currentAffectedBoxes.contains(box))
				{
					box.isPreviewing = false;
					DragState.removeAffectedBox(box);
				}
			}

			DragState.setAffectedBoxes(currentAffectedBoxes);
		}
	}

	private void performMassFill(boolean rmb)
	{
		for (PixelBox box : DragState.affectedBoxes)
		{
			if (rmb)
			{
				if (box.getId() != -1)
				{
					box.setId(-1);
				}
			}
			else
			{
				if (box.getId() == -1)
				{
					box.setId(DragState.dragItemId);
				}
			}
		}
	}

	private void performItemMove(PixelBox targetBox)
	{
		PixelBox sourceBox = DragState.dragSourceBox;

		if (sourceBox.boxType == targetBox.boxType && isValidDrop(targetBox))
		{
			sourceBox.setId(-1);
			targetBox.setId(DragState.dragItemId);
		}
		else
		{
			revertDrag();
		}
	}

	private void showCtrlDragPreview(PixelBox targetBox)
	{
		PixelBox sourceBox = DragState.dragSourceBox;
		int dragItemId = DragState.dragItemId;
		BoxType boxType = sourceBox.boxType;
		int row = sourceBox.gridRow;
		int col = sourceBox.gridCol;

		DragState.clearAffectedBoxes();
		if (targetBox.gridRow == sourceBox.gridRow && targetBox.gridCol == sourceBox.gridCol)
		{
			return;
		}
		for (SetupPanel setup : SetupPanel.allSetups)
		{
			GridPanel grid = null;
			switch (boxType)
			{
				case INVENTORY:
					grid = setup.inventoryGrid;
					break;
				case RUNEPOUCH:
					grid = setup.runepouchGrid;
					break;
				case EQUIPMENT:
					grid = setup.equipmentGrid;
					break;
			}
			if (grid != null)
			{
				if (row >= 0 && row < grid.rows && col >= 0 && col < grid.cols)
				{
					PixelBox box = grid.boxes[row][col];
					if (box != null && box != sourceBox)
					{
						box.isPreviewing = true;
						box.showPreview(dragItemId);
						DragState.addAffectedBox(box);
					}
				}
			}
		}
	}

	private void performCtrlDragCopy(PixelBox targetBox)
	{
		PixelBox sourceBox = DragState.dragSourceBox;
		int dragItemId = DragState.dragItemId;
		BoxType boxType = sourceBox.boxType;
		int row = sourceBox.gridRow;
		int col = sourceBox.gridCol;
		if (sourceBox.gridCol == targetBox.gridCol && sourceBox.gridRow == targetBox.gridRow)
		{
			return;
		}
		for (SetupPanel setup : SetupPanel.allSetups)
		{
			GridPanel grid = null;
			switch (boxType)
			{
				case INVENTORY:
					grid = setup.inventoryGrid;
					break;
				case RUNEPOUCH:
					grid = setup.runepouchGrid;
					break;
				case EQUIPMENT:
					grid = setup.equipmentGrid;
					break;
			}
			if (grid != null)
			{
				if (row >= 0 && row < grid.rows && col >= 0 && col < grid.cols)
				{
					PixelBox box = grid.boxes[row][col];
					if (box != null)
					{
						box.setId(dragItemId);
					}
				}
			}
		}
	}

	public List<Integer> getItems()
	{
		List<Integer> itemList = new ArrayList<>();
		for (int r = 0; r < rows; r++)
		{
			for (int c = 0; c < cols; c++)
			{
				if (boxes[r][c] != null)
				{
					int idAtCell = boxes[r][c].getId();
					if (idAtCell != -1)
					{
						itemList.add(idAtCell);
					}
				}
			}
		}
		return itemList;
	}
}

class DragGlassPane extends JComponent
{
	private Image dragImage;
	private Point dragPoint;
	private PixelBox targetBox;

	public DragGlassPane()
	{
		setOpaque(false);
	}

	public void setDragImage(Image image, Point initialPoint)
	{
		this.dragImage = image;
		this.dragPoint = initialPoint;
		repaint();
	}

	public void setDragPoint(Point point)
	{
		this.dragPoint = point;
		repaint();
	}

	public void setTargetBox(PixelBox box)
	{
		this.targetBox = box;
		repaint();
	}

	public void clear()
	{
		this.dragImage = null;
		this.dragPoint = null;
		this.targetBox = null;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		if (dragImage != null && dragPoint != null && !DragState.ctrlPressed && !DragState.rmb)
		{
			Graphics2D g2d = (Graphics2D) g.create();
			int x = dragPoint.x - dragImage.getWidth(this) / 2;
			int y = dragPoint.y - 3 * dragImage.getHeight(this) / 2;
			g2d.drawImage(dragImage, x, y, this);

			if (targetBox != null)
			{
				Rectangle bounds = targetBox.getBounds();
				Point parentOnScreen = targetBox.getParent().getLocationOnScreen();
				Point glassOnScreen = this.getLocationOnScreen();
				int offsetX = targetBox.getX() - (glassOnScreen.x - parentOnScreen.x);
				int offsetY = targetBox.getY() - (glassOnScreen.y - parentOnScreen.y);
				g2d.setColor(config.markerColor());
				g2d.setStroke(new BasicStroke(1));
				g2d.drawRect(offsetX, offsetY, bounds.width, bounds.height);
			}

			g2d.dispose();
		}
	}
}

@Slf4j
class SetupPanel extends JPanel
{
	public GridPanel inventoryGrid;
	public GridPanel runepouchGrid;
	public GridPanel equipmentGrid;
	JTextField labelField;

	public static List<SetupPanel> allSetups = new ArrayList<>();

	public SetupPanel(ItemManager itemManager, SetupsWindow setupsWindow, SetupsContainer container)
	{
		setBackground(config.primaryMiddle());
		setOpaque(true);
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		int y = 0;

		gbc.gridx = 0;
		gbc.gridy = y++;
		gbc.insets = new Insets(10, 25, 10, 25);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		labelField = UISwingUtility.getThemedTextField("Setup Name");
		labelField.setColumns(10);
		labelField.setHorizontalAlignment(JTextField.CENTER);
		labelField.setPreferredSize(new Dimension(160, labelField.getPreferredSize().height + 5));
		add(labelField, gbc);

		gbc.gridx = 0;
		gbc.gridy = y++;
		gbc.insets = new Insets(0, 25, 25, 25);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		inventoryGrid = new GridPanel(7, 4, itemManager, setupsWindow, BoxType.INVENTORY);
		add(inventoryGrid, gbc);

		gbc.gridx = 0;
		gbc.gridy = y++;
		gbc.insets = new Insets(0, 25, 25, 25);
		gbc.anchor = GridBagConstraints.CENTER;
		runepouchGrid = new GridPanel(1, 4, itemManager, setupsWindow, BoxType.RUNEPOUCH);
		add(runepouchGrid, gbc);

		gbc.gridx = 0;
		gbc.gridy = y++;
		gbc.insets = new Insets(0, 25, 10, 25);
		gbc.anchor = GridBagConstraints.CENTER;
		equipmentGrid = new GridPanel(5, 3, itemManager, setupsWindow, BoxType.EQUIPMENT);
		add(equipmentGrid, gbc);


		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		setMaximumSize(getPreferredSize());

		allSetups.add(this);

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				Component clickedComponent = getComponentAt(e.getPoint());
				if (clickedComponent == SetupPanel.this)
				{
					requestFocus();
					repaint();
				}
			}
		});
	}

	public void updateBoxSize(int boxSize)
	{
		inventoryGrid.updateBoxSize(boxSize);
		runepouchGrid.updateBoxSize(boxSize);
		equipmentGrid.updateBoxSize(boxSize);
	}

	public List<Integer> getItems()
	{
		List<Integer> itemList = new ArrayList<>();
		itemList.addAll(inventoryGrid.getItems());
		itemList.addAll(runepouchGrid.getItems());
		itemList.addAll(equipmentGrid.getItems());
		return itemList;
	}

	public String getSetupName()
	{
		return labelField.getText();
	}
}

class TransferableImage implements Transferable
{
	private Image image;

	public TransferableImage(Image image)
	{
		this.image = image;
	}

	public Object getTransferData(java.awt.datatransfer.DataFlavor flavor)
	{
		if (flavor.equals(java.awt.datatransfer.DataFlavor.imageFlavor) && image != null)
		{
			return image;
		}
		return null;
	}

	public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors()
	{
		return new java.awt.datatransfer.DataFlavor[]{java.awt.datatransfer.DataFlavor.imageFlavor};
	}

	public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor)
	{
		return flavor.equals(java.awt.datatransfer.DataFlavor.imageFlavor);
	}
}

public class SetupsContainer extends JPanel
{
	private ItemManager itemManager;
	private SetupsWindow setupsWindow;
	private List<SetupPanel> setupPanels;
	private int setupCount;

	private JPopupMenu popupMenu;

	public SetupsContainer(ItemManager itemManager, SetupsWindow setupsWindow)
	{
		this.itemManager = itemManager;
		this.setupsWindow = setupsWindow;
		this.setupPanels = new ArrayList<>();
		setBackground(config.primaryMiddle());
		setOpaque(true);
		setBorder(BorderFactory.createLineBorder(config.primaryDark()));
		setSetupCount(5);

		this.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				resizeSetups();
			}
		});

		popupMenu = new JPopupMenu();
		popupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				rebuildPopupMenus();
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e)
			{
			}
		});

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (SwingUtilities.isRightMouseButton(e))
				{
					popupMenu.show(SetupsContainer.this, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				Component clickedComponent = getComponentAt(e.getPoint());
				if (clickedComponent == SetupsContainer.this)
				{
					requestFocus();
					repaint();
				}
			}
		});

		attachSetupPanelMouseListeners();
	}

	private void attachSetupPanelMouseListeners()
	{
		for (SetupPanel sp : setupPanels)
		{
			sp.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseReleased(MouseEvent e)
				{
					if (SwingUtilities.isRightMouseButton(e))
					{
						Point containerPoint = SwingUtilities.convertPoint(sp, e.getPoint(), SetupsContainer.this);
						popupMenu.show(SetupsContainer.this, containerPoint.x, containerPoint.y);
					}
				}
			});
		}
	}

	private void rebuildPopupMenus()
	{
		popupMenu.removeAll();

		JMenuItem copyImageItem = getThemedMenuItem("Copy image to clipboard");
		copyImageItem.addActionListener(e -> copySetupContainerImageToClipboard());
		popupMenu.add(copyImageItem);

		popupMenu.add(getThemedSeperator());

		JMenu changeNumberMenu = getThemedMenu("Change number of setups");
		int currentCount = getSetupCount();
		for (int i = 1; i <= 8; i++)
		{
			JMenuItem item = getThemedMenuItem(String.valueOf(i));
			item.setEnabled(i != currentCount);
			final int newCount = i;
			item.addActionListener(e -> changeNumberOfSetups(newCount));
			changeNumberMenu.add(item);
		}
		popupMenu.add(changeNumberMenu);

		popupMenu.add(getThemedSeperator());


		JMenu saveInventoryMenu = getThemedMenu("Save inventory");
		addSetupsToSubMenu(saveInventoryMenu, (setup) -> {
			List<Integer> inv = getInventoryForSetup(setup);
			setupsWindow.saveInventory(inv);
		});
		popupMenu.add(saveInventoryMenu);

		JMenu saveRunepouchMenu = getThemedMenu("Save runepouch");
		addSetupsToSubMenu(saveRunepouchMenu, (setup) -> {
			int[] runes = getRunepouchForSetup(setup);
			setupsWindow.saveRunepouch(runes[0], runes[1], runes[2], runes[3]);
		});
		popupMenu.add(saveRunepouchMenu);

		JMenu saveEquipmentMenu = getThemedMenu("Save equipment");
		addSetupsToSubMenu(saveEquipmentMenu, (setup) -> {
			List<Integer> equip = getEquipmentForSetup(setup);
			setupsWindow.saveEquipment(equip);
		});
		popupMenu.add(saveEquipmentMenu);

		JMenu saveSingleSetupMenu = getThemedMenu("Save a single setup");
		addSetupsToSubMenu(saveSingleSetupMenu, (setup) -> {
			SetupTemplate template = buildSetupTemplate(setup);
			setupsWindow.saveSetup(template);
		});
		popupMenu.add(saveSingleSetupMenu);

		JMenuItem saveGroupItem = getThemedMenuItem("Save all setups individually");
		saveGroupItem.addActionListener(e -> {
			List<SetupTemplate> allTemplates = new ArrayList<>();
			for (SetupPanel s : getSetupPanels())
			{
				allTemplates.add(buildSetupTemplate(s));
			}
			setupsWindow.saveAllSetups(allTemplates);
		});
		popupMenu.add(saveGroupItem);
		popupMenu.add(getThemedSeperator());

		JMenuItem importTextItem = getThemedMenuItem("Import text setup from clipboard");
		importTextItem.addActionListener(e -> {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			try
			{
				String data = (String) clipboard.getData(DataFlavor.stringFlavor);
				if (data != null && !data.isEmpty())
				{
					importSetupsFromText(data);
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		});
		popupMenu.add(importTextItem);


		JMenuItem exportTextItem = getThemedMenuItem("Export text setup to clipboard");
		exportTextItem.addActionListener(e -> {
			String setupsText = exportSetupsToText();
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(setupsText);
			clipboard.setContents(selection, null);
		});
		popupMenu.add(exportTextItem);


		JMenu exportSingleSetupMenu = getThemedMenu("Export to Inventory Setups format");
		int index = 1;
		for (SetupPanel s : getSetupPanels())
		{
			String name = s.getSetupName();
			if (name == null || name.isEmpty())
			{
				name = "Setup Name";
			}
			JMenuItem item = getThemedMenuItem(index + " (" + name + ")");
			final SetupPanel sp = s;
			item.addActionListener(e ->
			{
				String jsonString = exportSetupToInventorySetupsFormat(sp);
				if (jsonString != null)
				{
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection selection = new StringSelection(jsonString);
					clipboard.setContents(selection, null);
					JOptionPane.showMessageDialog(this, "Setup exported to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);
				}
				else
				{
					JOptionPane.showMessageDialog(this, "Failed to export setup.", "Error", JOptionPane.ERROR_MESSAGE);
				}
			});
			exportSingleSetupMenu.add(item);
			index++;
		}
		popupMenu.add(exportSingleSetupMenu);

		popupMenu.add(getThemedSeperator());

		JMenuItem saveSetupAsItem = getThemedMenuItem("Save Setup As...");
		saveSetupAsItem.addActionListener(e -> {
			String name = JOptionPane.showInputDialog(SetupsContainer.this, "Enter name for the setup");
			if (name != null && !name.trim().isEmpty())
			{
				String setupsText = exportSetupsToText();
				saveSetupsToFile(setupsText, name.trim());
			}
		});
		popupMenu.add(saveSetupAsItem);

		JMenu openSetupMenu = getThemedMenu("Open Setup");
		Map<String, String> setupsMap = loadSetupsFromFile();
		if (setupsMap.isEmpty())
		{
			JMenuItem emptyItem = getThemedMenuItem("No saved setups");
			emptyItem.setEnabled(false);
			openSetupMenu.add(emptyItem);
		}
		else
		{
			for (Map.Entry<String, String> entry : setupsMap.entrySet())
			{
				String name = entry.getKey();
				String setupsText = entry.getValue();

				JMenu setupMenu = getThemedMenu(name);

				JMenuItem loadItem = getThemedMenuItem("Load");
				loadItem.addActionListener(e -> {
					importSetupsFromText(setupsText);
				});
				setupMenu.add(loadItem);

				JMenuItem deleteItem = getThemedMenuItem("Delete");
				deleteItem.addActionListener(e -> {
					deleteSetupFromFile(name);
					rebuildPopupMenus();
				});
				setupMenu.add(deleteItem);

				openSetupMenu.add(setupMenu);
			}
		}
		popupMenu.add(openSetupMenu);


	}

	public String exportSetupToInventorySetupsFormat(SetupPanel setup)
	{
		try
		{
			Map<String, Object> setupMap = new HashMap<>();
			Map<String, Object> innerSetup = new HashMap<>();

			List<Map<String, Object>> invList = new ArrayList<>();
			List<Integer> invIds = getInventoryForSetup(setup);
			for (int id : invIds)
			{
				if (id == -1)
				{
					invList.add(null);
				}
				else
				{
					Map<String, Object> itemMap = new HashMap<>();
					itemMap.put("id", id);
					invList.add(itemMap);
				}
			}
			innerSetup.put("inv", invList);

			List<Map<String, Object>> eqList = new ArrayList<>(Collections.nCopies(14, null)); // Initialize with nulls

			List<Integer> equipIds = getEquipmentForSetup(setup);
			if (equipIds.size() >= 15)
			{
				Map<Integer, Integer> eqIndexMap = new HashMap<>();
				eqIndexMap.put(0, 1);
				eqIndexMap.put(1, 3);
				eqIndexMap.put(2, 4);
				eqIndexMap.put(3, 6);
				eqIndexMap.put(4, 7);
				eqIndexMap.put(5, 8);
				//skip
				eqIndexMap.put(7, 10);
				//skip
				eqIndexMap.put(9, 12);
				eqIndexMap.put(10, 13);
				//skip
				eqIndexMap.put(12, 14);
				eqIndexMap.put(13, 5);

				for (int eqIndex = 0; eqIndex < 14; eqIndex++)
				{
					if (eqIndexMap.containsKey(eqIndex))
					{
						int equipIndex = eqIndexMap.get(eqIndex);
						int id = equipIds.get(equipIndex);
						if (id != -1)
						{
							Map<String, Object> itemMap = new HashMap<>();
							itemMap.put("id", id);
							eqList.set(eqIndex, itemMap);
						}
						else
						{
							eqList.set(eqIndex, null);
						}
					}
					else
					{
						eqList.set(eqIndex, null);
					}
				}
			}
			else
			{
				System.err.println("Equipment list size is less than expected.");
				return null;
			}
			innerSetup.put("eq", eqList);

			int[] runes = getRunepouchForSetup(setup);
			List<Map<String, Object>> rpList = new ArrayList<>();
			for (int id : runes)
			{
				if (id != -1)
				{
					Map<String, Object> itemMap = new HashMap<>();
					itemMap.put("id", id);
					itemMap.put("q", 1000);
					rpList.add(itemMap);
				}
				else
				{
					rpList.add(null);
				}
			}
			innerSetup.put("rp", rpList);

			int index2ItemId = equipIds.get(2);
			if (index2ItemId != -1)
			{
				List<Map<String, Object>> qvList = new ArrayList<>();
				Map<String, Object> itemMap = new HashMap<>();
				itemMap.put("id", index2ItemId);
				itemMap.put("q", 1000);
				qvList.add(itemMap);
				innerSetup.put("qv", qvList);
			}

			innerSetup.put("name", setup.getSetupName());
			innerSetup.put("hc", "#FFFFFFFF");
			innerSetup.put("sb", 2);
			setupMap.put("setup", innerSetup);

			List<Integer> layoutList = new ArrayList<>();

			for (int i = 0; i < 56; i++)
			{
				layoutList.add(-1);
			}

			for (int r = 0; r < 5; r++)
			{
				for (int c = 0; c < 3; c++)
				{
					boolean skip = (r == 0 && c == 0) || (r == 3 && c == 0) || (r == 3 && c == 2);
					if (skip)
					{
						continue;
					}

					int equipIndex = r * 3 + c;
					int layoutIndex = r * 8 + c;

					if (r > 0 || c > 0)
					{
						if (r > 0)
						{
							equipIndex -= 1;
						}
						if (r > 3 || (r == 3 && c > 0))
						{
							equipIndex -= 1;
						}
					}
					else
					{
						equipIndex = 0;
					}

					if (equipIndex >= 0 && equipIndex < equipIds.size())
					{
						int id = equipIds.get(equipIndex);
						layoutList.set(layoutIndex, id);
					}
				}
			}

			invIds = getInventoryForSetup(setup);
			int invIndex = 0;
			for (int r = 0; r < 7; r++)
			{
				for (int c = 0; c < 4; c++)
				{
					int id = invIds.get(invIndex++);
					int layoutRow = r;
					int layoutCol = c + 4;
					int layoutIndex = layoutRow * 8 + layoutCol;
					layoutList.set(layoutIndex, id);
				}
			}

			runes = getRunepouchForSetup(setup);
			for (int c = 0; c < 4; c++)
			{
				int id = runes[c];
				int layoutRow = 5;
				int layoutCol = c;
				int layoutIndex = layoutRow * 8 + layoutCol;
				layoutList.set(layoutIndex, id);
			}

			setupMap.put("layout", layoutList);

			Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
			String jsonString = gson.toJson(setupMap);

			return jsonString;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public Map<String, String> loadSetupsFromFile()
	{
		Map<String, String> setupsMap = new LinkedHashMap<>();
		String pluginDir = PLUGIN_DIRECTORY;
		String filePath = pluginDir + "/misc-dir/setupgroups.setups";
		File file = new File(filePath);
		if (!file.exists())
		{
			return setupsMap;
		}
		try (BufferedReader br = new BufferedReader(new FileReader(file)))
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				int lastCommaIndex = line.lastIndexOf(",");
				if (lastCommaIndex == -1)
				{
					continue;
				}
				String setupsText = line.substring(0, lastCommaIndex);
				String name = line.substring(lastCommaIndex + 1);
				setupsMap.put(name, setupsText);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return setupsMap;
	}

	public void saveSetupsToFile(String setupsText, String name)
	{
		String pluginDir = PLUGIN_DIRECTORY;
		String filePath = pluginDir + "/misc-dir/setupgroups.setups";
		File file = new File(filePath);
		if (!file.exists())
		{
			file.getParentFile().mkdirs();
			try
			{
				file.createNewFile();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return;
			}
		}
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true)))
		{
			bw.write(setupsText + "," + name);
			bw.newLine();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void deleteSetupFromFile(String name)
	{
		String pluginDir = PLUGIN_DIRECTORY;
		String filePath = pluginDir + "/misc-dir/setupgroups.setups";
		File file = new File(filePath);
		if (!file.exists())
		{
			return;
		}
		File tempFile = new File(file.getAbsolutePath() + ".tmp");
		try (BufferedReader br = new BufferedReader(new FileReader(file));
			 BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile)))
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				int lastCommaIndex = line.lastIndexOf(",");
				if (lastCommaIndex == -1)
				{
					continue;
				}
				String setupName = line.substring(lastCommaIndex + 1);
				if (!setupName.equals(name))
				{
					bw.write(line);
					bw.newLine();
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		if (!file.delete())
		{
			System.err.println("Could not delete original file");
			return;
		}
		if (!tempFile.renameTo(file))
		{
			System.err.println("Could not rename temp file");
		}
	}

	public String exportSetupsToText()
	{
		StringBuilder sb = new StringBuilder();
		int setupIndex = 0;
		for (SetupPanel setup : setupPanels)
		{
			if (setupIndex > 0)
			{
				sb.append(",");
			}
			sb.append("{").append(setup.labelField.getText()).append("}");

			List<Integer> invIds = new ArrayList<>();
			for (int r = 0; r < 7; r++)
			{
				for (int c = 0; c < 4; c++)
				{
					int id = setup.inventoryGrid.boxes[r][c].getId();
					invIds.add(id);
				}
			}
			sb.append("{");
			for (int i = 0; i < invIds.size(); i++)
			{
				sb.append(invIds.get(i));
				if (i < invIds.size() - 1)
				{
					sb.append(",");
				}
			}
			sb.append("}");

			sb.append("{");
			for (int c = 0; c < 4; c++)
			{
				int id = setup.runepouchGrid.boxes[0][c].getId();
				sb.append(id);
				if (c < 3)
				{
					sb.append(",");
				}
			}
			sb.append("}");

			List<Integer> equipIds = new ArrayList<>();
			for (int r = 0; r < 5; r++)
			{
				for (int c = 0; c < 3; c++)
				{
					boolean skip = (r == 0 && c == 0) || (r == 3 && c == 0) || (r == 3 && c == 2);
					if (!skip && setup.equipmentGrid.boxes[r][c] != null)
					{
						int id = setup.equipmentGrid.boxes[r][c].getId();
						equipIds.add(id);
					}
					else
					{
						equipIds.add(-1);
					}
				}
			}
			sb.append("{");
			for (int i = 0; i < equipIds.size(); i++)
			{
				sb.append(equipIds.get(i));
				if (i < equipIds.size() - 1)
				{
					sb.append(",");
				}
			}
			sb.append("}");

			sb.append("{").append(setup.labelField.getText()).append("}");

			setupIndex++;
		}
		return sb.toString();
	}

	public void importSetupsFromText(String text)
	{
		String[] setups = text.split("(?<=\\})(?:,)");
		List<SetupPanel> newSetupPanels = new ArrayList<>();
		for (String setupText : setups)
		{
			String[] parts = setupText.split("\\}(?=\\{)");
			if (parts.length < 5)
			{
				continue;
			}
			String topLabel = parts[0].replaceFirst("^\\{", "");
			String invPart = parts[1].replaceFirst("^\\{", "");
			String runePart = parts[2].replaceFirst("^\\{", "");
			String equipPart = parts[3].replaceFirst("^\\{", "");
			String savedLabel = parts[4].replaceAll("^\\{|}$", "");

			String[] invTokens = invPart.split(",", -1);
			if (invTokens.length != 28)
			{
				continue;
			}
			List<Integer> invIds = new ArrayList<>();
			for (String t : invTokens)
			{
				invIds.add(Integer.parseInt(t));
			}

			String[] runeTokens = runePart.split(",", -1);
			if (runeTokens.length != 4)
			{
				continue;
			}
			int[] runes = new int[4];
			for (int i = 0; i < 4; i++)
			{
				runes[i] = Integer.parseInt(runeTokens[i]);
			}

			String[] equipTokens = equipPart.split(",", -1);
			if (equipTokens.length != 15)
			{
				continue;
			}
			List<Integer> equipIds = new ArrayList<>();
			for (String t : equipTokens)
			{
				equipIds.add(Integer.parseInt(t));
			}

			SetupPanel setupPanel = new SetupPanel(itemManager, setupsWindow, this);
			setupPanel.labelField.setText(savedLabel); // Use savedLabel
			int idx = 0;
			for (int r = 0; r < 7; r++)
			{
				for (int c = 0; c < 4; c++)
				{
					setupPanel.inventoryGrid.boxes[r][c].setId(invIds.get(idx++));
				}
			}
			for (int c = 0; c < 4; c++)
			{
				setupPanel.runepouchGrid.boxes[0][c].setId(runes[c]);
			}
			idx = 0;
			for (int r = 0; r < 5; r++)
			{
				for (int c = 0; c < 3; c++)
				{
					boolean skip = (r == 0 && c == 0) || (r == 3 && c == 0) || (r == 3 && c == 2);
					if (!skip && setupPanel.equipmentGrid.boxes[r][c] != null)
					{
						setupPanel.equipmentGrid.boxes[r][c].setId(equipIds.get(idx));
					}
					idx++;
				}
			}

			newSetupPanels.add(setupPanel);
		}
		setSetupCount(newSetupPanels.size());
		this.setupPanels.clear();
		this.removeAll();

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;

		int x = 0;
		for (SetupPanel sp : newSetupPanels)
		{
			gbc.gridx = x++;
			if (x == 1)
			{
				gbc.insets = new Insets(0, 10, 0, 0);
			}
			else if (x == newSetupPanels.size())
			{
				gbc.insets = new Insets(0, 0, 0, 10);
			}
			else
			{
				gbc.insets = new Insets(0, 0, 0, 0);
			}
			this.setupPanels.add(sp);
			this.add(sp, gbc);
		}

		attachSetupPanelMouseListeners();

		revalidate();
		repaint();
	}

	public Map<Integer, Integer> getItemCounts()
	{
		Map<Integer, Integer> itemCounts = new HashMap<>();

		for (SetupPanel setupPanel : setupPanels)
		{
			for (Integer item : setupPanel.getItems())
			{
				itemCounts.merge(item, 1, Integer::sum);
			}
		}

		return itemCounts;
	}

	public int getSetupCount()
	{
		return setupCount;
	}

	public List<SetupPanel> getSetupPanels()
	{
		return setupPanels;
	}

	private void resizeSetups()
	{
		int W_total = getWidth();
		int H_total = getHeight();
		int N_setups = setupPanels.size();

		int M_horiz = 10;
		int LeftMargin = 10;
		int RightMargin = 10;
		int totalSetupMargins = (N_setups - 1) * M_horiz + LeftMargin + RightMargin;
		int W_content = W_total - totalSetupMargins;
		if (W_content <= 0)
		{
			return;
		}
		int W_perSetup = W_content / N_setups;

		int maxBoxSizeWidth = calculateMaxBoxSizeWidth(W_perSetup);
		int maxBoxSizeHeight = calculateMaxBoxSizeHeight(H_total);
		int finalBoxSize = Math.min(Math.min(maxBoxSizeWidth, maxBoxSizeHeight), 40);

		for (SetupPanel setupPanel : setupPanels)
		{
			setupPanel.updateBoxSize(finalBoxSize);
		}
	}

	private int calculateMaxBoxSizeWidth(int W_perSetup)
	{
		int boxGap = 2;
		int maxCols = 4;
		int gridInsets = 2;
		int maxBoxSizeWidth = (W_perSetup - ((maxCols - 1) * boxGap) - gridInsets) / maxCols;
		return Math.max(maxBoxSizeWidth, 1);
	}

	private int calculateMaxBoxSizeHeight(int H_total)
	{
		int labelFieldHeight = 20;
		int totalSetupVerticalMargins = 80;
		int gridInsets = 2;
		int boxGap = 2;
		int totalRows = 7 + 1 + 5;
		int totalBoxGaps = (6 + 0 + 4) * boxGap;
		int totalGridInsets = gridInsets * 3;
		int maxBoxSizeHeight = (H_total - labelFieldHeight - totalSetupVerticalMargins - totalBoxGaps - totalGridInsets) / totalRows;
		return Math.max(maxBoxSizeHeight, 1);
	}

	public void setSetupCount(int amount)
	{
		this.setupCount = amount;
		removeAll();
		SetupPanel.allSetups.clear();
		setupPanels.clear();

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;

		int x = 0;
		for (int i = 0; i < setupCount; i++)
		{
			gbc.gridx = x++;
			gbc.insets = new Insets(0, 0, 0, 0);

			if (i == 0)
			{
				gbc.insets = new Insets(0, 10, 0, 0);
			}

			if (i == setupCount - 1)
			{
				gbc.insets = new Insets(0, 0, 0, 10);
			}

			SetupPanel setupPanel = new SetupPanel(itemManager, setupsWindow, this);
			setupPanels.add(setupPanel);
			add(setupPanel, gbc);

			if (i < setupCount - 1)
			{
				gbc.gridx = x++;
			}
		}

		attachSetupPanelMouseListeners();

		revalidate();
		repaint();
	}

	private void addSetupsToSubMenu(JMenu parentMenu, SetupAction action)
	{
		int index = 1;
		for (SetupPanel s : getSetupPanels())
		{
			String name = s.getSetupName();
			if (name == null || name.isEmpty())
			{
				name = "Setup Name";
			}
			JMenuItem item = getThemedMenuItem(index + " (" + name + ")");
			final SetupPanel sp = s;
			item.addActionListener(e -> action.perform(sp));
			parentMenu.add(item);
			index++;
		}
	}

	private interface SetupAction
	{
		void perform(SetupPanel setup);
	}

	private List<Integer> getInventoryForSetup(SetupPanel setup)
	{
		List<Integer> inv = new ArrayList<>();
		for (int r = 0; r < 7; r++)
		{
			for (int c = 0; c < 4; c++)
			{
				int id = setup.inventoryGrid.boxes[r][c].getId();
				inv.add(id);
			}
		}
		return inv;
	}

	private int[] getRunepouchForSetup(SetupPanel setup)
	{
		int[] runes = new int[4];
		for (int c = 0; c < 4; c++)
		{
			runes[c] = setup.runepouchGrid.boxes[0][c].getId();
		}
		return runes;
	}

	private List<Integer> getEquipmentForSetup(SetupPanel setup)
	{
		List<Integer> equip = new ArrayList<>();
		int eqIndex = 0;
		for (int r = 0; r < 5; r++)
		{
			for (int c = 0; c < 3; c++)
			{
				boolean skip = (r == 0 && c == 0) || (r == 3 && c == 0) || (r == 3 && c == 2);
				if (skip)
				{
					equip.add(-1);
				}
				else
				{
					PixelBox pb = setup.equipmentGrid.boxes[r][c];
					int id = (pb != null) ? pb.getId() : -1;
					equip.add(id);
				}
				eqIndex++;
			}
		}
		return equip;
	}

	private SetupTemplate buildSetupTemplate(SetupPanel setup)
	{
		int[] runes = getRunepouchForSetup(setup);
		List<Integer> inv = getInventoryForSetup(setup);
		List<Integer> equip = getEquipmentForSetup(setup);

		return new SetupTemplate(runes[0], runes[1], runes[2], runes[3], inv, equip, setup.labelField.getText());
	}

	private void changeNumberOfSetups(int newCount)
	{
		List<SetupTemplate> currentTemplates = new ArrayList<>();
		List<String> names = new ArrayList<>();
		for (SetupPanel s : getSetupPanels())
		{
			currentTemplates.add(buildSetupTemplate(s));
			names.add(s.getSetupName());
		}

		setSetupCount(newCount);

		List<SetupPanel> newPanels = getSetupPanels();
		for (int i = 0; i < Math.min(newCount, currentTemplates.size()); i++)
		{
			SetupTemplate t = currentTemplates.get(i);
			SetupPanel s = newPanels.get(i);
			s.labelField.setText(names.get(i));

			List<Integer> inv = t.getInventory();
			int index = 0;
			for (int r = 0; r < 7; r++)
			{
				for (int c = 0; c < 4; c++)
				{
					s.inventoryGrid.boxes[r][c].setId(inv.get(index++));
				}
			}

			int[] runes = new int[]{t.getRuneA(), t.getRuneB(), t.getRuneC(), t.getRuneD()};
			for (int c = 0; c < 4; c++)
			{
				s.runepouchGrid.boxes[0][c].setId(runes[c]);
			}

			List<Integer> equip = t.getEquipment();
			int eqIndex = 0;
			for (int r = 0; r < 5; r++)
			{
				for (int c = 0; c < 3; c++)
				{
					boolean skip = (r == 0 && c == 0) || (r == 3 && c == 0) || (r == 3 && c == 2);
					int id = equip.get(eqIndex++);
					if (!skip && s.equipmentGrid.boxes[r][c] != null)
					{
						s.equipmentGrid.boxes[r][c].setId(id);
					}
				}
			}
		}
		repaint();
	}

	private void copySetupContainerImageToClipboard()
	{
		for (SetupPanel sp : getSetupPanels())
		{
			for (PixelBox[] row : sp.inventoryGrid.boxes)
			{
				for (PixelBox pb : row)
				{
					if (pb != null)
					{
						pb.setSkipPaintComponent(true);
					}
				}
			}
			for (PixelBox[] row : sp.runepouchGrid.boxes)
			{
				for (PixelBox pb : row)
				{
					if (pb != null)
					{
						pb.setSkipPaintComponent(true);
					}
				}
			}
			for (int r = 0; r < 5; r++)
			{
				for (int c = 0; c < 3; c++)
				{
					if (r == 0 && c == 0)
					{
						continue;
					}
					if (r == 3 && c == 0)
					{
						continue;
					}
					if (r == 3 && c == 2)
					{
						continue;
					}
					if (sp.equipmentGrid.boxes[r][c] != null)
					{
						sp.equipmentGrid.boxes[r][c].setSkipPaintComponent(true);
					}
				}
			}
		}

		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = image.createGraphics();
		paint(g2d);
		g2d.dispose();

		for (SetupPanel sp : getSetupPanels())
		{
			for (PixelBox[] row : sp.inventoryGrid.boxes)
			{
				for (PixelBox pb : row)
				{
					if (pb != null)
					{
						pb.setSkipPaintComponent(false);
					}
				}
			}
			for (PixelBox[] row : sp.runepouchGrid.boxes)
			{
				for (PixelBox pb : row)
				{
					if (pb != null)
					{
						pb.setSkipPaintComponent(false);
					}
				}
			}
			for (int r = 0; r < 5; r++)
			{
				for (int c = 0; c < 3; c++)
				{
					if (r == 0 && c == 0)
					{
						continue;
					}
					if (r == 3 && c == 0)
					{
						continue;
					}
					if (r == 3 && c == 2)
					{
						continue;
					}
					if (sp.equipmentGrid.boxes[r][c] != null)
					{
						sp.equipmentGrid.boxes[r][c].setSkipPaintComponent(false);
					}
				}
			}
		}
		Transferable t = new TransferableImage(image);
		Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		cb.setContents(t, null);
	}
}
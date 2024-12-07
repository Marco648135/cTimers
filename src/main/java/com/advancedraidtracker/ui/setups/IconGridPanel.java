package com.advancedraidtracker.ui.setups;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JPanel;
import javax.swing.Timer;
import net.runelite.client.game.ItemManager;

public class IconGridPanel extends JPanel
{
	private static final int IMAGE_SIZE = 40;
	private static final int MARGIN = 5;
	protected static IconGridPanel SELECTED = null;

	protected final ItemManager itemManager;
	protected final List<ItemEntry> items;
	protected Integer hoveredIndex = null;
	protected Integer selectedIndex = null;
	protected SetupsWindow setupsWindow;
	private static List<WeakReference<IconGridPanel>> instances = new CopyOnWriteArrayList<>();

	static class ItemEntry
	{
		int id;
		int count;

		ItemEntry(int id)
		{
			this.id = id;
			this.count = 1;
		}

		ItemEntry(int id, int count)
		{
			this.id = id;
			this.count = count;
		}

		void increment()
		{
			this.count++;
		}

		void setCount(int count)
		{
			this.count = count;
		}
	}

	public static void repaintAll()
	{
		Iterator<WeakReference<IconGridPanel>> iterator = instances.iterator();
		while (iterator.hasNext())
		{
			WeakReference<IconGridPanel> ref = iterator.next();
			IconGridPanel instance = ref.get();
			if (instance != null)
			{
				instance.repaint();
			}
			else
			{
				iterator.remove();
			}
		}
	}


	public IconGridPanel(ItemManager itemManager, SetupsWindow setupsWindow)
	{
		this.itemManager = itemManager;
		this.setupsWindow = setupsWindow;
		this.items = new ArrayList<>();
		instances.add(new WeakReference<>(this));
		setBackground(config.primaryDark());
		setOpaque(true);

		MouseAdapter mouseHandler = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				handleMouseMove(e.getX(), e.getY());
				IconGridPanel.repaintAll();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (hoveredIndex != null)
				{
					hoveredIndex = null;
					repaint();
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					handlePopupTrigger(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					handlePopupTrigger(e);
				}
				else
				{
					handleMouseClick(e.getX(), e.getY());
				}
			}
		};

		addMouseMotionListener(mouseHandler);
		addMouseListener(mouseHandler);
		Timer timer = new Timer(100, e -> {
			repaint();
			((Timer) e.getSource()).stop();
		});
		timer.setRepeats(false);
		timer.start();
	}

	public void addItem(int id)
	{
		for (ItemEntry entry : items)
		{
			if (entry.id == id)
			{
				entry.increment();
				sortItems();
				repaint();
				updatePreferredSize();
				return;
			}
		}
		items.add(new ItemEntry(id));
		sortItems();
		repaint();
		updatePreferredSize();
	}

	public void addItemAt(int position, int id)
	{
		for (ItemEntry entry : items)
		{
			if (entry.id == id)
			{
				entry.increment();
				sortItems();
				repaint();
				updatePreferredSize();
				return;
			}
		}
		if (position < 0 || position > items.size())
		{
			position = items.size();
		}
		items.add(position, new ItemEntry(id));
		sortItems();
		repaint();
		updatePreferredSize();
	}

	public void addItems(List<Integer> ids)
	{
		boolean changed = false;
		for (int id : ids)
		{
			boolean found = false;
			for (ItemEntry entry : items)
			{
				if (entry.id == id)
				{
					entry.increment();
					found = true;
					changed = true;
					break;
				}
			}
			if (!found)
			{
				items.add(new ItemEntry(id));
				changed = true;
			}
		}
		if (changed)
		{
			sortItems();
			repaint();
			updatePreferredSize();
		}
	}


	public void clearItems()
	{
		items.clear();
		selectedIndex = null;
		hoveredIndex = null;
		repaint();
		updatePreferredSize();
	}


	public void replaceData(Map<Integer, Integer> data)
	{
		if (data == null)
		{
			throw new IllegalArgumentException("Data map cannot be null.");
		}

		items.clear();
		for (Map.Entry<Integer, Integer> entry : data.entrySet())
		{
			int id = entry.getKey();
			int count = entry.getValue();
			if (count < 1)
			{
				count = 1;
			}
			items.add(new ItemEntry(id, count));
		}

		sortItems();
		repaint();
		updatePreferredSize();
	}

	protected void handleMouseMove(int mouseX, int mouseY)
	{
		int panelWidth = getWidth();
		int usableWidth = panelWidth - 2 * MARGIN;
		if (usableWidth <= 0)
		{
			return;
		}

		int columns = Math.max(usableWidth / IMAGE_SIZE, 1);
		int row = mouseY / IMAGE_SIZE;
		int col = (mouseX - MARGIN) / IMAGE_SIZE;

		if (col < 0 || col >= columns)
		{
			if (hoveredIndex != null)
			{
				hoveredIndex = null;
				repaint();
			}
			return;
		}

		int index = row * columns + col;
		if (index >= items.size())
		{
			if (hoveredIndex != null)
			{
				hoveredIndex = null;
				repaint();
			}
			return;
		}

		if (!Integer.valueOf(index).equals(hoveredIndex))
		{
			hoveredIndex = index;
			repaint();
		}
	}

	protected void handleMouseClick(int mouseX, int mouseY)
	{
		int panelWidth = getWidth();
		int usableWidth = panelWidth - 2 * MARGIN;
		if (usableWidth <= 0)
		{
			return;
		}

		int columns = Math.max(usableWidth / IMAGE_SIZE, 1);
		int row = mouseY / IMAGE_SIZE;
		int col = (mouseX - MARGIN) / IMAGE_SIZE;

		if (col < 0 || col >= columns)
		{
			return;
		}

		int index = row * columns + col;
		if (index >= items.size())
		{
			return;
		}

		SELECTED = this;
		selectedIndex = index;
		setupsWindow.setSelectedItem(items.get(selectedIndex).id);
		repaint();
	}

	public void mouseWheelIncremented(int rotation)
	{
		if(rotation > 0)
		{
			if(selectedIndex >= items.size()-2) //+ is -1
			{
				selectedIndex = 0;
			}
			else
			{
				selectedIndex++;
			}
			setupsWindow.setSelectedItem(items.get(selectedIndex).id);
		}
		else if(rotation < 0)
		{
			if(selectedIndex <= 0)
			{
				selectedIndex = items.size()-1;
			}
			else
			{
				selectedIndex--;
			}
			setupsWindow.setSelectedItem(items.get(selectedIndex).id);
		}
	}

	protected void handlePopupTrigger(MouseEvent e)
	{

	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g.create();
		try
		{
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int panelWidth = getWidth();
			int panelHeight = getHeight();
			if (panelWidth <= 2 * MARGIN)
			{
				return;
			}

			int usableWidth = panelWidth - 2 * MARGIN;

			int columns = Math.max(usableWidth / IMAGE_SIZE, 1);
			int rows = (int) Math.ceil((double) items.size() / columns);

			for (int i = 0; i < items.size(); i++)
			{
				int row = i / columns;
				int col = i % columns;

				int x = MARGIN + col * IMAGE_SIZE;
				int y = row * IMAGE_SIZE;

				ItemEntry entry = items.get(i);

				BufferedImage image = ImageManager.getImage(itemManager, entry.id);

				if (image != null && entry.id != -1)
				{
					g2d.drawImage(image, x, y, IMAGE_SIZE, IMAGE_SIZE, this);
				}
				else if (entry.id == -1)
				{
					g2d.setColor(config.boxColor());
					g2d.drawRect(x + 1, y + 4, IMAGE_SIZE - 8, IMAGE_SIZE - 8);
					g2d.setStroke(new BasicStroke(1));
					g2d.drawLine(x - 3 + IMAGE_SIZE / 2, y + 10, x - 3 + IMAGE_SIZE / 2, y + IMAGE_SIZE - 10);
					g2d.drawLine(x - 3 + 10, y + IMAGE_SIZE / 2, x - 3 + IMAGE_SIZE - 10, y + IMAGE_SIZE / 2);
				}

				if (entry.count > 1)
				{
					String countStr = String.valueOf(entry.count);
					g2d.setFont(g2d.getFont().deriveFont(16f));
					FontMetrics fm = g2d.getFontMetrics();
					int textWidth = fm.stringWidth(countStr);
					int textHeight = fm.getHeight();

					g2d.setColor(config.primaryDark());
					g2d.fillRect(x + 2, y + 2, textWidth + 4, textHeight);

					g2d.setColor(config.fontColor());
					g2d.drawString(countStr, x + 4, y + fm.getAscent() + 2);
				}

				if (hoveredIndex != null && hoveredIndex == i)
				{
					g2d.setColor(new Color(255, 255, 255, 50)); // 50/255 transparency
					g2d.fillRect(x, y, IMAGE_SIZE, IMAGE_SIZE);

					g2d.setColor(Color.LIGHT_GRAY);
					g2d.drawRect(x, y, IMAGE_SIZE, IMAGE_SIZE);
				}

				if (selectedIndex != null && selectedIndex == i && SELECTED == this)
				{
					g2d.setColor(new Color(40, 140, 235));
					g2d.setStroke(new BasicStroke(1));
					g2d.drawRect(x, y, IMAGE_SIZE, IMAGE_SIZE);
				}
			}

			Dimension preferredSize = new Dimension(panelWidth, rows * IMAGE_SIZE);
			if (!preferredSize.equals(getPreferredSize()))
			{
				setPreferredSize(preferredSize);
				revalidate();
			}
		}
		finally
		{
			g2d.dispose();
		}
	}

	/**
	 * Updates the preferred size based on the current number of items.
	 */
	private void updatePreferredSize()
	{
		int panelWidth = getWidth();
		int usableWidth = panelWidth - 2 * MARGIN;
		if (usableWidth <= 0)
		{
			return;
		}

		int columns = Math.max(usableWidth / IMAGE_SIZE, 1);
		int rows = (int) Math.ceil((double) items.size() / columns);
		Dimension preferredSize = new Dimension(panelWidth, rows * IMAGE_SIZE);
		if (!preferredSize.equals(getPreferredSize()))
		{
			setPreferredSize(preferredSize);
			revalidate();
		}
	}

	private void sortItems()
	{
		items.sort((a, b) -> Integer.compare(b.count, a.count));
	}
}

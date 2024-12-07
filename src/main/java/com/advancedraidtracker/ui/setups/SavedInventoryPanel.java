package com.advancedraidtracker.ui.setups;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;
import com.advancedraidtracker.ui.customrenderers.IconManager;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedMenuItem;
import static com.advancedraidtracker.utility.datautility.DataWriter.PLUGIN_DIRECTORY;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;

@Slf4j
class SavedInventoryPanel extends JPanel
{
	private static final String PRESETS_FILE = PLUGIN_DIRECTORY + "misc-dir/inventory.presets";

	private JPanel columnsPanel;
	private List<ColumnPanel> columnPanels;
	private ItemManager itemManager;
	private SetupsWindow setupsWindow;

	public SavedInventoryPanel(ItemManager itemManager, SetupsWindow setupsWindow)
	{
		this.itemManager = itemManager;
		this.setupsWindow = setupsWindow;
		setBackground(config.primaryDark());
		setOpaque(true);

		columnsPanel = new JPanel();
		columnsPanel.setLayout(new BoxLayout(columnsPanel, BoxLayout.X_AXIS));
		columnsPanel.setBackground(config.primaryDark());

		JScrollPane scrollPane = new JScrollPane(columnsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

		setLayout(new BorderLayout());
		add(scrollPane, BorderLayout.CENTER);

		columnPanels = new ArrayList<>();

		try
		{
			File file = new File(PRESETS_FILE);
			if (!file.exists())
			{
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			loadPresetsFromFile();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void loadPresetsFromFile()
	{
		try
		{
			List<String> lines = Files.readAllLines(Paths.get(PRESETS_FILE));
			for (String line : lines)
			{
				String[] tokens = line.split(",", -1);
				if (tokens.length < 29)
				{
					continue;
				}
				List<Integer> ids = new ArrayList<>();
				for (int i = 0; i < 28; i++)
				{
					ids.add(Integer.parseInt(tokens[i]));
				}
				String label = tokens[28];
				addSavedInventory(ids, label, false);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void addSavedInventory(List<Integer> ids, String label)
	{
		addSavedInventory(ids, label, true);
	}

	private void addSavedInventory(List<Integer> ids, String label, boolean writeToFile)
	{
		ColumnPanel columnPanel = new ColumnPanel(ids, label);
		columnPanels.add(columnPanel);
		columnsPanel.add(columnPanel);
		columnsPanel.revalidate();
		columnsPanel.repaint();

		if (writeToFile)
		{
			updatePresetsFile();
		}
	}

	private void removeColumn(ColumnPanel columnPanel)
	{
		int index = columnPanels.indexOf(columnPanel);
		if (index >= 0)
		{
			columnPanels.remove(index);
			columnsPanel.remove(columnPanel);
			columnsPanel.revalidate();
			columnsPanel.repaint();
			updatePresetsFile();
		}
	}

	private void updatePresetsFile()
	{
		try
		{
			List<String> lines = new ArrayList<>();
			for (ColumnPanel columnPanel : columnPanels)
			{
				List<Integer> ids = columnPanel.ids;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < 28; i++)
				{
					sb.append(ids.get(i));
					sb.append(",");
				}
				sb.append(columnPanel.labelField.getText());
				lines.add(sb.toString());
			}
			Files.write(Paths.get(PRESETS_FILE), lines);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private class ColumnPanel extends JPanel implements MouseListener, MouseMotionListener
	{
		private GridPanel gridPanel;
		private JButton addButton;
		private JButton deleteButton;
		private JTextField labelField;
		private List<Integer> ids;
		private Point initialClick;
		private boolean dragging = false;

		public ColumnPanel(List<Integer> ids, String label)
		{
			this.ids = ids;
			setLayout(new GridBagLayout());
			setOpaque(false);

			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5),
				BorderFactory.createLineBorder(config.primaryLight())
			));

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(1, 1, 1, 1);
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 1.0;

			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 2;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			labelField = new JTextField(label);
			labelField.setBackground(config.primaryDark());
			labelField.setForeground(config.fontColor());
			add(labelField, gbc);

			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.NONE;

			gridPanel = new GridPanel(7, 4, itemManager, setupsWindow, BoxType.INVENTORY);
			for (int r = 0; r < 7; r++)
			{
				for (int c = 0; c < 4; c++)
				{
					int index = r * 4 + c;
					int id = ids.get(index);
					PixelBox box = gridPanel.boxes[r][c];
					box.setId(id);
					box.setSkipPaintComponent(true);
				}
			}

			addButton = new JButton(IconManager.getAddIcon());
			addButton.setPreferredSize(new Dimension(40, 40));
			addButton.setBackground(config.primaryDark());
			addButton.setOpaque(true);
			addButton.addActionListener(e -> showAddMenu(addButton));

			deleteButton = new JButton(IconManager.getDeleteIcon());
			deleteButton.setPreferredSize(new Dimension(40, 40));
			deleteButton.setBackground(config.primaryDark());
			deleteButton.setOpaque(true);
			deleteButton.addActionListener(e -> removeColumn(ColumnPanel.this));

			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.gridheight = 7;
			gbc.weighty = 1.0;
			add(gridPanel, gbc);

			gbc.gridx = 1;
			gbc.gridy = 1;
			gbc.gridheight = 1;
			gbc.weighty = 0.0;
			add(addButton, gbc);

			gbc.gridy = 2;
			add(deleteButton, gbc);

			for (int i = 3; i < 8; i++)
			{
				gbc.gridy = i;
				add(Box.createRigidArea(new Dimension(40, 40)), gbc);
			}

			addMouseListener(this);
			addMouseMotionListener(this);
		}

		private void showAddMenu(Component comp)
		{
			JPopupMenu menu = new JPopupMenu();
			List<SetupPanel> allSetups = setupsWindow.getSetupsContainer().getSetupPanels();
			int setupIndex = 1;
			for (SetupPanel sp : allSetups)
			{
				String setupName = sp.getSetupName();
				if (setupName == null || setupName.isEmpty())
				{
					setupName = "Setup Name";
				}
				JMenuItem menuItem = getThemedMenuItem("Add to " + setupIndex + " (" + setupName + ")");
				final int index = setupIndex - 1;
				menuItem.addActionListener(ae -> {
					applyInventoryToSetup(allSetups.get(index));
				});
				menu.add(menuItem);
				setupIndex++;
			}
			menu.show(comp, 0, comp.getHeight());
		}

		private void applyInventoryToSetup(SetupPanel targetSetup)
		{
			int idx = 0;
			for (int r = 0; r < 7; r++)
			{
				for (int c = 0; c < 4; c++)
				{
					targetSetup.inventoryGrid.boxes[r][c].setId(ids.get(idx++));
				}
			}
			targetSetup.revalidate();
			targetSetup.repaint();
			setupsWindow.pushItemChanges();
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			initialClick = e.getPoint();
			dragging = false;
		}

		@Override
		public void mouseDragged(MouseEvent e)
		{
			int dx = Math.abs(e.getX() - initialClick.x);
			int dy = Math.abs(e.getY() - initialClick.y);
			if (dx > 5 || dy > 5)
			{
				dragging = true;
			}
			if (dragging)
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			if (dragging)
			{
				setCursor(Cursor.getDefaultCursor());
				dragging = false;
				Component component = columnsPanel.getComponentAt(SwingUtilities.convertPoint(this, e.getPoint(), columnsPanel));
				if (component instanceof ColumnPanel)
				{
					int targetIndex = columnPanels.indexOf(component);
					int sourceIndex = columnPanels.indexOf(this);
					if (targetIndex != sourceIndex)
					{
						columnsPanel.remove(this);
						columnsPanel.add(this, targetIndex);
						columnPanels.remove(this);
						columnPanels.add(targetIndex, this);
						columnsPanel.revalidate();
						columnsPanel.repaint();
						updatePresetsFile();
					}
				}
			}
		}

		@Override
		public void mouseClicked(MouseEvent e)
		{
		}

		@Override
		public void mouseEntered(MouseEvent e)
		{
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
		}

		@Override
		public void mouseMoved(MouseEvent e)
		{
		}
	}
}
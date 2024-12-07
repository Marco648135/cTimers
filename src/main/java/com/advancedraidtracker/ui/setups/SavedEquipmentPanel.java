package com.advancedraidtracker.ui.setups;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;
import com.advancedraidtracker.ui.customrenderers.IconManager;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedMenuItem;
import static com.advancedraidtracker.utility.datautility.DataWriter.PLUGIN_DIRECTORY;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;

@Slf4j
class SavedEquipmentPanel extends JPanel
{
	private static final String PRESETS_FILE = PLUGIN_DIRECTORY + "misc-dir/equipment.presets";

	private JPanel columnsPanel;
	private List<ColumnPanel> columnPanels;
	private ItemManager itemManager;
	private SetupsWindow setupsWindow;

	public SavedEquipmentPanel(ItemManager itemManager, SetupsWindow setupsWindow)
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
				if (tokens.length < 16)
				{
					continue;
				}
				List<Integer> ids = new ArrayList<>();
				for (int i = 0; i < 15; i++)
				{
					String token = tokens[i];
					if (token.isEmpty())
					{
						ids.add(null);
					}
					else
					{
						ids.add(Integer.parseInt(token));
					}
				}
				String label = tokens[15];

				addSavedEquipment(ids, label, false);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void addSavedEquipment(List<Integer> ids, String label)
	{
		addSavedEquipment(ids, label, true);
	}

	private void addSavedEquipment(List<Integer> ids, String label, boolean writeToFile)
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
				for (int i = 0; i < ids.size(); i++)
				{
					Integer val = ids.get(i);
					if (val == null)
					{
						sb.append("");
					}
					else
					{
						sb.append(val);
					}
					sb.append(",");
				}
				// Append label at end
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
			gbc.gridheight = 1;
			gbc.gridwidth = 2;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			labelField = new JTextField(label);
			labelField.setBackground(config.primaryDark());
			labelField.setForeground(config.fontColor());
			add(labelField, gbc);

			gbc.fill = GridBagConstraints.NONE;
			gbc.gridwidth = 1;

			gridPanel = new GridPanel(5, 3, itemManager, setupsWindow, BoxType.EQUIPMENT);

			// Map the ids
			int index = 0;
			for (int r = 0; r < 5; r++)
			{
				for (int c = 0; c < 3; c++)
				{
					if (gridPanel.boxes[r][c] != null)
					{
						if (index < ids.size())
						{
							Integer id = ids.get(index);
							PixelBox box = gridPanel.boxes[r][c];
							if (id != null)
							{
								box.setId(id);
							}
							else
							{
								box.setId(-1);
							}
							box.setSkipPaintComponent(true);
						}
					}
					index++;
				}
			}

			addButton = new JButton(IconManager.getAddIcon());
			addButton.setPreferredSize(new Dimension(40, 40));
			addButton.setMargin(new Insets(0, 0, 0, 0));
			addButton.setBackground(config.primaryDark());
			addButton.setOpaque(true);
			addButton.addActionListener(e -> showAddMenu(addButton));

			deleteButton = new JButton(IconManager.getDeleteIcon());
			deleteButton.setPreferredSize(new Dimension(40, 40));
			deleteButton.setMargin(new Insets(0, 0, 0, 0));
			deleteButton.setBackground(config.primaryDark());
			deleteButton.setOpaque(true);

			deleteButton.addActionListener(e -> removeColumn(ColumnPanel.this));

			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.gridheight = 5;
			gbc.weighty = 1.0;
			add(gridPanel, gbc);

			gbc.gridx = 1;
			gbc.gridy = 1;
			gbc.gridheight = 1;
			gbc.weighty = 0.0;
			add(addButton, gbc);

			gbc.gridy = 2;
			add(deleteButton, gbc);

			for (int i = 3; i < 6; i++)
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
					applyEquipmentToSetup(allSetups.get(index));
				});
				menu.add(menuItem);
				setupIndex++;
			}
			menu.show(comp, 0, comp.getHeight());
		}

		private void applyEquipmentToSetup(SetupPanel targetSetup)
		{
			int index = 0;
			for (int r = 0; r < 5; r++)
			{
				for (int c = 0; c < 3; c++)
				{
					boolean skip = (r == 0 && c == 0) || (r == 3 && c == 0) || (r == 3 && c == 2);
					Integer val = ids.get(index++);
					if (!skip && targetSetup.equipmentGrid.boxes[r][c] != null)
					{
						targetSetup.equipmentGrid.boxes[r][c].setId(val == null ? -1 : val);
					}
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

package com.advancedraidtracker.ui.setups;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;

import com.advancedraidtracker.ui.customrenderers.IconManager;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedLabel;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedMenuItem;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedPanel;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedTextField;
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
class SavedSetupsPanel extends JPanel
{
	private static final String PRESETS_FILE = PLUGIN_DIRECTORY + "misc-dir/setups.setups";

	private ItemManager itemManager;
	private SetupsWindow setupsWindow;

	private JPanel columnsPanel;
	private List<ColumnPanel> columnPanels;

	public SavedSetupsPanel(ItemManager itemManager, SetupsWindow setupsWindow)
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

		loadPresetsFromFile();
	}

	private void loadPresetsFromFile()
	{
		File file = new File(PRESETS_FILE);
		if (!file.exists())
		{
			try
			{
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			catch (IOException ex)
			{
			}
			return;
		}

		try
		{
			List<String> lines = Files.readAllLines(Paths.get(PRESETS_FILE));
			for (String line : lines)
			{
				String[] parts = line.split("}");
				if (parts.length < 5)
				{
					continue;
				}
				String topLabel = parts[0].replace("{", "");
				String invPart = parts[1].replace("{", "");
				String runePart = parts[2].replace("{", "");
				String equipPart = parts[3].replace("{", "");
				String savedLabel = parts[4].replace("{", "");

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

				addSavedSetup(topLabel, invIds, runes, equipIds, savedLabel, false);
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}

	public void addSavedSetup(String topLabel, List<Integer> invIds, int[] runes, List<Integer> equipIds, String savedLabel)
	{
		addSavedSetup(topLabel, invIds, runes, equipIds, savedLabel, true);
	}

	private void addSavedSetup(String topLabel, List<Integer> invIds, int[] runes, List<Integer> equipIds, String savedLabel, boolean writeToFile)
	{
		ColumnPanel cp = new ColumnPanel(topLabel, invIds, runes, equipIds, savedLabel);
		columnPanels.add(cp);
		columnsPanel.add(cp);
		columnsPanel.revalidate();
		columnsPanel.repaint();
		if (writeToFile)
		{
			updatePresetsFile();
		}
	}

	private void removeColumn(ColumnPanel cp)
	{
		int index = columnPanels.indexOf(cp);
		if (index >= 0)
		{
			columnPanels.remove(index);
			columnsPanel.remove(cp);
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
			for (ColumnPanel cp : columnPanels)
			{
				StringBuilder sb = new StringBuilder();
				sb.append("{").append(cp.topLabelField.getText()).append("}");
				sb.append("{");
				for (int i = 0; i < 28; i++)
				{
					sb.append(cp.invIds.get(i));
					if (i < 27)
					{
						sb.append(",");
					}
				}
				sb.append("}");
				sb.append("{");
				for (int i = 0; i < 4; i++)
				{
					sb.append(cp.runes[i]);
					if (i < 3)
					{
						sb.append(",");
					}
				}
				sb.append("}");
				sb.append("{");
				for (int i = 0; i < 15; i++)
				{
					sb.append(cp.equipIds.get(i));
					if (i < 14)
					{
						sb.append(",");
					}
				}
				sb.append("}");
				sb.append("{").append(cp.savedLabelField.getText()).append("}");
				lines.add(sb.toString());
			}
			Files.write(Paths.get(PRESETS_FILE), lines);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}

	private class ColumnPanel extends JPanel implements MouseListener, MouseMotionListener
	{
		JLabel topLabelField;
		JTextField savedLabelField;
		GridPanel invGrid;
		GridPanel runeGrid;
		GridPanel equipGrid;

		JButton addButton;
		JButton deleteButton;

		String topLabel;
		String savedLabel;
		List<Integer> invIds;
		int[] runes;
		List<Integer> equipIds;
		private Point initialClick;
		private boolean dragging = false;

		public ColumnPanel(String topLabel, List<Integer> invIds, int[] runes, List<Integer> equipIds, String savedLabel)
		{
			this.topLabel = topLabel;
			this.invIds = invIds;
			this.runes = runes;
			this.equipIds = equipIds;
			this.savedLabel = savedLabel;

			setLayout(new GridBagLayout());
			setOpaque(false);
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5),
				BorderFactory.createLineBorder(config.primaryLight())));

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(1, 1, 1, 1);
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 1.0;

			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 2;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			topLabelField = getThemedLabel(topLabel);

			savedLabelField = getThemedTextField(savedLabel);


			JPanel splitPanel = getThemedPanel();
			splitPanel.setLayout(new GridLayout(1, 2));
			splitPanel.add(topLabelField);
			splitPanel.add(savedLabelField);

			add(splitPanel, gbc);

			gbc.fill = GridBagConstraints.NONE;
			gbc.gridwidth = 1;

			invGrid = new GridPanel(7, 4, itemManager, setupsWindow, BoxType.INVENTORY);
			int idx = 0;
			for (int r = 0; r < 7; r++)
			{
				for (int c = 0; c < 4; c++)
				{
					invGrid.boxes[r][c].setId(invIds.get(idx++));
					invGrid.boxes[r][c].setSkipPaintComponent(true);
				}
			}
			runeGrid = new GridPanel(1, 4, itemManager, setupsWindow, BoxType.RUNEPOUCH);
			for (int c = 0; c < 4; c++)
			{
				runeGrid.boxes[0][c].setId(runes[c]);
				runeGrid.boxes[0][c].setSkipPaintComponent(true);
			}
			equipGrid = new GridPanel(5, 3, itemManager, setupsWindow, BoxType.EQUIPMENT);
			idx = 0;
			for (int r = 0; r < 5; r++)
			{
				for (int c = 0; c < 3; c++)
				{
					boolean skip = (r == 0 && c == 0) || (r == 3 && c == 0) || (r == 3 && c == 2);
					int val = equipIds.get(idx++);
					if (!skip && equipGrid.boxes[r][c] != null)
					{
						equipGrid.boxes[r][c].setId(val);
						equipGrid.boxes[r][c].setSkipPaintComponent(true);
					}
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
			add(invGrid, gbc);

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

			gbc.gridx = 0;
			gbc.gridy = 8;
			gbc.gridheight = 1;
			gbc.gridwidth = 2;
			gbc.weighty = 0;
			add(runeGrid, gbc);

			gbc.gridy = 9;
			gbc.gridwidth = 2;
			add(equipGrid, gbc);

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
				JMenuItem mi = getThemedMenuItem("Add to " + setupIndex + " (" + setupName + ")");
				final int idx = setupIndex - 1;
				mi.addActionListener(ae -> applyFullSetupToSetup(allSetups.get(idx)));
				menu.add(mi);
				setupIndex++;
			}
			menu.show(comp, 0, comp.getHeight());
		}

		private void applyFullSetupToSetup(SetupPanel targetSetup)
		{
			int index = 0;
			for (int r = 0; r < 7; r++)
			{
				for (int c = 0; c < 4; c++)
				{
					targetSetup.inventoryGrid.boxes[r][c].setId(invIds.get(index++));
				}
			}

			for (int c = 0; c < 4; c++)
			{
				targetSetup.runepouchGrid.boxes[0][c].setId(runes[c]);
			}

			index = 0;
			for (int r = 0; r < 5; r++)
			{
				for (int c = 0; c < 3; c++)
				{
					boolean skip = (r == 0 && c == 0) || (r == 3 && c == 0) || (r == 3 && c == 2);
					int val = equipIds.get(index++);
					if (!skip && targetSetup.equipmentGrid.boxes[r][c] != null)
					{
						targetSetup.equipmentGrid.boxes[r][c].setId(val);
					}
				}
			}

			targetSetup.labelField.setText(topLabelField.getText());
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
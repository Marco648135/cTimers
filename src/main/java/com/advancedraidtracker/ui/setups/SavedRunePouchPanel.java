package com.advancedraidtracker.ui.setups;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;

import com.advancedraidtracker.ui.customrenderers.IconManager;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedMenuItem;
import static com.advancedraidtracker.utility.datautility.DataWriter.PLUGIN_DIRECTORY;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;

@Slf4j
class SavedRunePouchPanel extends JPanel
{
	private static final String PRESETS_FILE_PATH = PLUGIN_DIRECTORY + "misc-dir/runepouch.presets";
	private ItemManager itemManager;
	private SetupsWindow setupsWindow;

	private JPanel contentPanel;
	private List<RunePouchPresetRow> presetRows;

	public SavedRunePouchPanel(ItemManager itemManager, SetupsWindow setupsWindow)
	{
		this.itemManager = itemManager;
		this.setupsWindow = setupsWindow;
		setBackground(config.primaryDark());
		setOpaque(true);

		setLayout(new BorderLayout());
		contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBackground(config.primaryDark());
		contentPanel.setOpaque(true);

		JScrollPane scrollPane = new JScrollPane(contentPanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		add(scrollPane, BorderLayout.CENTER);

		presetRows = new ArrayList<>();

		readPresetsFromFile();
		initializeDragAndDrop();
	}

	private void readPresetsFromFile()
	{
		File file = null;
		try
		{
			file = new File(PRESETS_FILE_PATH);
			if (!file.exists())
			{
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}


		try (BufferedReader reader = new BufferedReader(new FileReader(file)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] tokens = line.split(",", -1);
				if (tokens.length < 5)
				{
					continue;
				}
				int id1 = Integer.parseInt(tokens[0]);
				int id2 = Integer.parseInt(tokens[1]);
				int id3 = Integer.parseInt(tokens[2]);
				int id4 = Integer.parseInt(tokens[3]);
				String label = tokens[4];
				addSavedRunePouch(id1, id2, id3, id4, label, false);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void addSavedRunePouch(int id1, int id2, int id3, int id4)
	{
		addSavedRunePouch(id1, id2, id3, id4, "", true);
	}

	private void addSavedRunePouch(int id1, int id2, int id3, int id4, String label, boolean writeToFile)
	{
		RunePouchPresetRow row = new RunePouchPresetRow(id1, id2, id3, id4, label);
		presetRows.add(row);
		contentPanel.add(row);
		contentPanel.revalidate();
		contentPanel.repaint();

		if (writeToFile)
		{
			updatePresetsFile();
		}
	}

	private void removePresetRow(RunePouchPresetRow row)
	{
		int index = presetRows.indexOf(row);
		if (index >= 0)
		{
			presetRows.remove(index);
			contentPanel.remove(row);
			contentPanel.revalidate();
			contentPanel.repaint();
			updatePresetsFile();
		}
	}

	private void updatePresetsFile()
	{
		File file = new File(PRESETS_FILE_PATH);
		file.getParentFile().mkdirs();

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file)))
		{
			for (RunePouchPresetRow row : presetRows)
			{
				writer.write(row.id1 + "," + row.id2 + "," + row.id3 + "," + row.id4 + "," + row.labelField.getText() + System.lineSeparator());
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void initializeDragAndDrop()
	{

	}

	private class RunePouchPresetRow extends JPanel
	{
		private int id1, id2, id3, id4;
		private JTextField labelField;
		private JButton addButton;
		private JButton deleteButton;

		public RunePouchPresetRow(int id1, int id2, int id3, int id4, String label)
		{
			this.id1 = id1;
			this.id2 = id2;
			this.id3 = id3;
			this.id4 = id4;

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setBackground(config.primaryDark());
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5, 5, 5, 5),
				BorderFactory.createLineBorder(config.primaryLight())));

			addButton = new JButton(IconManager.getAddIcon());
			addButton.setPreferredSize(new Dimension(40, 40));
			addButton.setBackground(config.primaryDark());
			addButton.setOpaque(true);
			addButton.setBorder(BorderFactory.createEmptyBorder());
			addButton.addActionListener(e -> showAddMenu(addButton));

			deleteButton = new JButton(IconManager.getDeleteIcon());
			deleteButton.setPreferredSize(new Dimension(40, 40));
			deleteButton.setBackground(config.primaryDark());
			deleteButton.setOpaque(true);
			deleteButton.setBorder(BorderFactory.createEmptyBorder());
			deleteButton.addActionListener(e -> removePresetRow(RunePouchPresetRow.this));

			labelField = new JTextField(label);
			labelField.setBackground(config.primaryDark());
			labelField.setForeground(config.fontColor());

			add(labelField);
			add(Box.createRigidArea(new Dimension(3, 0)));
			add(addButton);
			add(Box.createRigidArea(new Dimension(3, 0)));
			add(deleteButton);
			add(Box.createRigidArea(new Dimension(3, 0)));

			for (int id : new int[]{id1, id2, id3, id4})
			{
				PixelBox pixelBox = new PixelBox(itemManager, setupsWindow, BoxType.RUNEPOUCH, null, 0, 0);
				pixelBox.setPreferredSize(new Dimension(40, 40));
				pixelBox.setId(id);
				pixelBox.setSkipPaintComponent(true);
				add(pixelBox);
				add(Box.createRigidArea(new Dimension(1, 0)));
			}
			remove(getComponentCount() - 1);
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
				JMenuItem item = getThemedMenuItem("Add to " + setupIndex + " (" + setupName + ")");
				final int idx = setupIndex - 1;
				item.addActionListener(ae -> applyRunePouchToSetup(allSetups.get(idx)));
				menu.add(item);
				setupIndex++;
			}
			menu.show(comp, 0, comp.getHeight());
		}

		private void applyRunePouchToSetup(SetupPanel targetSetup)
		{
			targetSetup.runepouchGrid.boxes[0][0].setId(id1);
			targetSetup.runepouchGrid.boxes[0][1].setId(id2);
			targetSetup.runepouchGrid.boxes[0][2].setId(id3);
			targetSetup.runepouchGrid.boxes[0][3].setId(id4);
			targetSetup.revalidate();
			targetSetup.repaint();
			setupsWindow.pushItemChanges();
		}
	}
}
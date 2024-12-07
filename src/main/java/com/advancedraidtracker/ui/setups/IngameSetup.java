package com.advancedraidtracker.ui.setups;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;
import com.advancedraidtracker.ui.customrenderers.IconManager;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedMenuItem;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
import javax.swing.Box;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IngameSetup extends JPanel
{
	ClientThread clientThread;
	Client client;
	ItemManager itemManager;
	SetupsWindow setupsWindow;
	JButton refreshButton;
	JButton addButton;
	GridPanel inventoryGrid;
	GridPanel equipmentGrid;

	public IngameSetup(ClientThread clientThread, Client client, ItemManager itemManager, SetupsWindow setupsWindow)
	{
		this.clientThread = clientThread;
		this.client = client;
		this.itemManager = itemManager;
		this.setupsWindow = setupsWindow;
		setBackground(config.primaryDark());
		setOpaque(true);

		setLayout(new GridBagLayout());

		refreshButton = new JButton(IconManager.getRefreshIcon());
		addButton = new JButton(IconManager.getAddIcon());

		refreshButton.setBackground(config.primaryDark());
		refreshButton.setOpaque(true);
		refreshButton.addActionListener(e -> processItems());
		refreshButton.setToolTipText("Refresh items from game");

		addButton.setBackground(config.primaryDark());
		addButton.setOpaque(true);
		addButton.addActionListener(e -> showAddMenu(addButton));
		addButton.setToolTipText("Add items to setup");

		inventoryGrid = new GridPanel(7, 4, itemManager, setupsWindow, BoxType.INVENTORY);
		equipmentGrid = new GridPanel(5, 3, itemManager, setupsWindow, BoxType.EQUIPMENT);

		for (int r = 0; r < 7; r++)
		{
			for (int c = 0; c < 4; c++)
			{
				inventoryGrid.boxes[r][c].setSkipPaintComponent(true);
			}
		}

		for (int r = 0; r < 5; r++)
		{
			for (int c = 0; c < 3; c++)
			{
				PixelBox box = equipmentGrid.boxes[r][c];
				if (box != null)
				{
					box.setSkipPaintComponent(true);
				}
			}
		}

		int pixelBoxSize = 40;
		Dimension inventoryGridSize = new Dimension(4 * pixelBoxSize, 7 * pixelBoxSize);
		inventoryGrid.setPreferredSize(inventoryGridSize);
		inventoryGrid.setMinimumSize(inventoryGridSize);

		Dimension equipmentGridSize = new Dimension(3 * pixelBoxSize, 5 * pixelBoxSize);
		equipmentGrid.setPreferredSize(equipmentGridSize);
		equipmentGrid.setMinimumSize(equipmentGridSize);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setBackground(config.primaryDark());
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		buttonPanel.add(refreshButton);
		buttonPanel.add(addButton);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.NORTHEAST;
		gbc.fill = GridBagConstraints.NONE;
		add(buttonPanel, gbc);

		JPanel gridsPanel = new JPanel();
		gridsPanel.setBackground(config.primaryDark());
		gridsPanel.setLayout(new GridBagLayout());

		GridBagConstraints gridsGbc = new GridBagConstraints();
		gridsGbc.insets = new Insets(5, 5, 5, 5);
		gridsGbc.fill = GridBagConstraints.NONE;
		gridsGbc.anchor = GridBagConstraints.CENTER;

		gridsGbc.gridx = 0;
		gridsGbc.gridy = 0;
		gridsPanel.add(inventoryGrid, gridsGbc);

		gridsGbc.gridx = 1;
		gridsGbc.gridy = 0;
		gridsPanel.add(equipmentGrid, gridsGbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		add(gridsPanel, gbc);
	}

	void processItems()
	{
		clientThread.invokeLater(() ->
		{
			ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
			ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);

			int[] inventoryItemIds = new int[28];
			Map<Integer, Integer> equipmentIds = new HashMap<>();

			if (inventory != null)
			{
				Item[] items = inventory.getItems();
				for (int i = 0; i < items.length && i < 28; i++)
				{
					inventoryItemIds[i] = items[i].getId();
				}
			}
			else
			{
				Arrays.fill(inventoryItemIds, -1);
			}

			if (equipment != null)
			{
				Item[] items = equipment.getItems();
				for (int i = 0; i < items.length; i++)
				{
					equipmentIds.put(i, items[i].getId());
				}
			}

			SwingUtilities.invokeLater(() ->
			{
				for (int i = 0; i < 28; i++)
				{
					int row = i / 4;
					int col = i % 4;
					int id = inventoryItemIds[i];
					inventoryGrid.boxes[row][col].setId(id);
				}

				int[][] equipmentGridPositions = {
					{-1, 0, -1},
					{1, 2, 13},
					{3, 4, 5},
					{-1, 7, -1},
					{9, 10, 12}
				};

				for (int row = 0; row < 5; row++)
				{
					for (int col = 0; col < 3; col++)
					{
						PixelBox box = equipmentGrid.boxes[row][col];
						if (box == null)
						{
							continue;
						}

						int equipmentIndex = equipmentGridPositions[row][col];
						if (equipmentIndex == -1)
						{
							box.setId(-1);
							continue;
						}

						Integer itemId = equipmentIds.get(equipmentIndex);
						if (itemId != null)
						{
							box.setId(itemId);
						}
						else
						{
							box.setId(-1);
						}
					}
				}

				revalidate();
				repaint();
			});
		});
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
			menuItem.addActionListener(ae ->
			{
				applyItemsToSetup(allSetups.get(index));
			});
			menu.add(menuItem);
			setupIndex++;
		}
		menu.show(comp, 0, comp.getHeight());
	}

	private void applyItemsToSetup(SetupPanel targetSetup)
	{
		for (int row = 0; row < 7; row++)
		{
			for (int col = 0; col < 4; col++)
			{
				int id = inventoryGrid.boxes[row][col].getId();
				targetSetup.inventoryGrid.boxes[row][col].setId(id);
			}
		}

		for (int row = 0; row < 5; row++)
		{
			for (int col = 0; col < 3; col++)
			{
				PixelBox srcBox = equipmentGrid.boxes[row][col];
				PixelBox destBox = targetSetup.equipmentGrid.boxes[row][col];
				if (srcBox != null && destBox != null)
				{
					int id = srcBox.getId();
					destBox.setId(id);
				}
			}
		}

		targetSetup.revalidate();
		targetSetup.repaint();

		setupsWindow.pushItemChanges();
	}
}

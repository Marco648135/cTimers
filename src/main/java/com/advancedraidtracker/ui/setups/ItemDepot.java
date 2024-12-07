package com.advancedraidtracker.ui.setups;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;
import static com.advancedraidtracker.utility.datautility.DataWriter.PLUGIN_DIRECTORY;
import net.runelite.client.game.ItemManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDepot extends IconGridPanel
{
	private static final int ADD_BUTTON_ID = -1;
	private static final String MISC_DIR = "misc-dir/";

	private String presetFileName;

	private JPopupMenu popupMenu;
	private JTextField inputField;
	private JList<Trie.Entry> suggestionList;
	private DefaultListModel<Trie.Entry> listModel;
	private final static int MARGIN = 5;
	private final static int IMAGE_SIZE = 40;
	private boolean userCreated;

	public ItemDepot(ItemManager itemManager, SetupsWindow setupsWindow, String presetFileName, boolean userCreated)
	{
		super(itemManager, setupsWindow);
		this.presetFileName = presetFileName + ".preset";
		this.userCreated = userCreated;
		initialize();
	}

	public ItemDepot(ItemManager itemManager, SetupsWindow setupsWindow, String presetFileName, boolean userCreated, List<Integer> defaultValues)
	{
		super(itemManager, setupsWindow);
		this.presetFileName = presetFileName + ".preset";
		this.userCreated = userCreated;
		initialize(defaultValues);
	}

	public boolean isUserCreated()
	{
		return userCreated;
	}

	private void initialize(List<Integer> defaultValues)
	{
		try
		{
			Path miscDirPath = Path.of(PLUGIN_DIRECTORY + MISC_DIR);
			if (!Files.exists(miscDirPath))
			{
				Files.createDirectories(miscDirPath);
			}

			Path presetFilePath = miscDirPath.resolve(presetFileName);
			if (!Files.exists(presetFilePath))
			{
				Files.createFile(presetFilePath);
				try (BufferedWriter writer = Files.newBufferedWriter(presetFilePath))
				{
					for (Integer id : defaultValues)
					{
						writer.write(id.toString());
						writer.newLine();
					}
				}
			}

			List<Integer> loadedIds = loadItemIds(presetFilePath);
			addItems(loadedIds);

			addAddButton();

		}
		catch (IOException e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Failed to initialize ItemDepot.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}


	private void initialize()
	{
		initialize(new ArrayList<>());
	}

	private List<Integer> loadItemIds(Path filePath) throws IOException
	{
		List<Integer> ids = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(filePath))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (!line.isEmpty())
				{
					try
					{
						int id = Integer.parseInt(line);
						ids.add(id);
					}
					catch (NumberFormatException e)
					{
						System.err.println("Invalid item ID in " + presetFileName + ": " + line);
					}
				}
			}
		}
		return ids;
	}

	public void deleteFile()
	{
		Path presetFilePath = Path.of(PLUGIN_DIRECTORY + MISC_DIR + presetFileName);
		try
		{
			Files.deleteIfExists(presetFilePath);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Failed to delete the preset file: " + presetFileName,
				"Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	private void addAddButton()
	{
		addItem(ADD_BUTTON_ID);
	}

	@Override
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

		if (index == items.size() - 1 && items.get(index).id == ADD_BUTTON_ID)
		{
			showAutocompletePopup(new Point(mouseX, mouseY));
		}
		else
		{
			SELECTED = this;
			selectedIndex = index;
			setupsWindow.setSelectedItem(items.get(selectedIndex).id);
			repaint();
		}
	}

	@Override
	protected void handlePopupTrigger(MouseEvent e)
	{
		Point point = e.getPoint();
		int mouseX = point.x;
		int mouseY = point.y;

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

		int itemId = items.get(index).id;
		if (itemId == ADD_BUTTON_ID)
		{
			return;
		}

		JPopupMenu popup = new JPopupMenu();
		JMenuItem deleteItem = new JMenuItem("Delete");
		popup.add(deleteItem);

		deleteItem.addActionListener(event -> deleteItemAt(index));

		popup.show(this, mouseX, mouseY);
	}

	private void deleteItemAt(int index)
	{
		if (index < 0 || index >= items.size())
		{
			return;
		}

		int itemId = items.get(index).id;
		if (itemId == ADD_BUTTON_ID)
		{
			return;
		}

		items.remove(index);

		if (selectedIndex != null && selectedIndex == index)
		{
			selectedIndex = null;
			setupsWindow.setSelectedItem(-1);
		}
		else if (selectedIndex != null && selectedIndex > index)
		{
			selectedIndex--;
		}

		Path presetFilePath = Path.of(PLUGIN_DIRECTORY + MISC_DIR + presetFileName);
		try
		{
			List<String> lines = Files.readAllLines(presetFilePath);
			List<String> updatedLines = new ArrayList<>();
			for (String line : lines)
			{
				line = line.trim();
				if (!line.isEmpty())
				{
					try
					{
						int id = Integer.parseInt(line);
						if (id != itemId)
						{
							updatedLines.add(line);
						}
						else
						{
							itemId = -2;
						}
					}
					catch (NumberFormatException e)
					{
						updatedLines.add(line);
					}
				}
			}
			Files.write(presetFilePath, updatedLines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Failed to delete the item from the preset file.", "Error", JOptionPane.ERROR_MESSAGE);
		}

		repaint();

	}

	private void showAutocompletePopup(Point point)
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

		popupMenu.show(this, point.x, point.y);

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
		for (Trie.Entry entry : suggestions)
		{
			listModel.addElement(entry);
		}
	}

	private void selectSuggestedItem()
	{
		Trie.Entry selectedEntry = suggestionList.getSelectedValue();
		if (selectedEntry != null)
		{
			addNewItem(selectedEntry.getId());
		}
		popupMenu.setVisible(false);
	}

	private void addNewItem(int itemId)
	{
		items.remove(items.size() - 1);
		addItem(itemId);
		addAddButton();
		repaint();

		Path presetFilePath = Path.of(PLUGIN_DIRECTORY + MISC_DIR + presetFileName);
		try (BufferedWriter writer = Files.newBufferedWriter(presetFilePath, StandardOpenOption.APPEND))
		{
			writer.write(String.valueOf(itemId));
			writer.newLine();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Failed to add new supply item.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

}

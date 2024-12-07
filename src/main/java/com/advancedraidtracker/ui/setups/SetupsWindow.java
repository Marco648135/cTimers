package com.advancedraidtracker.ui.setups;

import com.advancedraidtracker.ui.BaseFrame;
import com.advancedraidtracker.ui.docking.*;
import static com.advancedraidtracker.utility.datautility.DataWriter.PLUGIN_DIRECTORY;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import javax.swing.JPanel;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;


public class SetupsWindow extends BaseFrame
{
	List<Integer> magicDefaults = List.of(
		21018, 21021, 21024, 21006, 12002, 19544, 13235, 21791,
		27275, 27251, 28313, 22323, 26241, 26243, 26245, 11663, 27624
	);

	List<Integer> meleeDefaults = List.of(
		28254, 28256, 28258, 22325, 29801, 22981, 13239, 21295,
		11804, 28307, 28316, 21003, 13576, 22322, 21015, 23987,
		13652, 24219, 11665
	);

	List<Integer> rangedDefaults = List.of(
		11664, 13072, 13073, 20997, 19547, 8842, 13237, 28951,
		12926, 26374, 27235, 27238, 27241, 26235, 11959, 21000,
		11212, 21944, 28922, 28310
	);

	List<Integer> miscDefaults = List.of(
		25975, 9763, 12612, 12608, 25818, 10588, 12018, 27281, 27641
	);

	List<Integer> suppliesDefaults = List.of(
		11090, 13441, 12695, 4417, 23685, 2444, 23733, 3024, 6685
	);
	RecentItems recentItems;
	private static SetupsWindow instance;
	private SetupsContainer setups;
	private DragGlassPane dragGlassPane;
	IconGridPanel itemCounts;
	private SavedInventoryPanel savedInventoryPanel;
	private SavedEquipmentPanel savedEquipmentPanel;
	private SavedRunePouchPanel savedRunePouchPanel;
	private SavedSetupsPanel savedSetupsPanel;
	private ItemManager itemManager;
	private ClientThread clientThread;
	private Client client;

	public SetupsWindow(ItemManager itemManager, ClientThread clientThread, Client client)
	{
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.client = client;
		ItemParser.processItemID();
		setTitle("Setup Creator");
		setPreferredSize(new Dimension(800, 600));
		SetupsToolbar toolbar = new SetupsToolbar();
		MultiSplitPane mainPane = new MultiSplitPane(false);

		setLayout(new BorderLayout());

		MultiSplitPane leftPane = new MultiSplitPane(true);
		MultiSplitPane middlePane = new MultiSplitPane(true);
		MultiSplitPane rightPane = new MultiSplitPane(true);

		MultiSplitPane bottomMiddlePane = new MultiSplitPane(false);

		savedSetupsPanel = new SavedSetupsPanel(itemManager, this);

		CustomPanel savedSetupsContainer = new CustomPanel("Saved Setups");
		savedSetupsContainer.getContentPanel().setLayout(new BorderLayout());
		savedSetupsContainer.getContentPanel().add(savedSetupsPanel, BorderLayout.CENTER);
		savedSetupsContainer.setPreferredSize(new Dimension(480, 700));

		setups = new SetupsContainer(itemManager, this);

		CustomPanel setupsContainer = new CustomPanel("Setups");
		setupsContainer.getContentPanel().setLayout(new BorderLayout());
		setupsContainer.getContentPanel().add(setups, BorderLayout.CENTER);

		RuneDepot runeDepot = new RuneDepot(itemManager, this);

		CustomPanel runeContainer = new CustomPanel("Runes");
		runeContainer.getContentPanel().setLayout(new BorderLayout());
		runeContainer.getContentPanel().add(runeDepot, BorderLayout.CENTER);
		runeContainer.setPreferredSize(new Dimension(300, 300));

		recentItems = new RecentItems(itemManager, this);

		CustomPanel recentItemsContainer = new CustomPanel("Recent Items");
		recentItemsContainer.getContentPanel().setLayout(new BorderLayout());
		recentItemsContainer.getContentPanel().add(recentItems, BorderLayout.CENTER);
		recentItemsContainer.setPreferredSize(new Dimension(300, 200));

		savedInventoryPanel = new SavedInventoryPanel(itemManager, this);
		CustomPanel savedInventoryContainer = new CustomPanel("Saved Inventories");
		savedInventoryContainer.getContentPanel().setLayout(new BorderLayout());
		savedInventoryContainer.getContentPanel().add(savedInventoryPanel, BorderLayout.CENTER);

		savedRunePouchPanel = new SavedRunePouchPanel(itemManager, this);
		CustomPanel savedRunePouchContainer = new CustomPanel("Saved Rune Pouches");
		savedRunePouchContainer.getContentPanel().setLayout(new BorderLayout());
		savedRunePouchContainer.getContentPanel().add(savedRunePouchPanel, BorderLayout.CENTER);
		savedRunePouchContainer.setPreferredSize(new Dimension(350, 0));

		savedEquipmentPanel = new SavedEquipmentPanel(itemManager, this);
		CustomPanel savedEquipmentContainer = new CustomPanel("Saved Equipment");
		savedEquipmentContainer.getContentPanel().setLayout(new BorderLayout());
		savedEquipmentContainer.getContentPanel().add(savedEquipmentPanel, BorderLayout.CENTER);
		savedEquipmentContainer.setPreferredSize(new Dimension(900, 0));

		IngameSetup ingameSetup = new IngameSetup(clientThread, client, itemManager, this);
		CustomPanel ingameContainer = new CustomPanel("In-game Setup");
		ingameContainer.getContentPanel().setLayout(new BorderLayout());
		ingameContainer.getContentPanel().add(ingameSetup, BorderLayout.CENTER);
		ingameContainer.setPreferredSize(new Dimension(450, 350));

		MultiSplitPane suppliesMiscPanel = new MultiSplitPane(false);
		suppliesMiscPanel.setPreferredSize(new Dimension(300, 300));
		savedEquipmentContainer.addCustomPanelAsTab(savedInventoryContainer);

		bottomMiddlePane.addComponent(savedEquipmentContainer);
		bottomMiddlePane.addComponent(savedRunePouchContainer);

		itemCounts = new IconGridPanel(itemManager, this);

		CustomPanel itemCountContainer = new CustomPanel("Item Counts");
		itemCountContainer.getContentPanel().setLayout(new BorderLayout());
		itemCountContainer.getContentPanel().add(itemCounts, BorderLayout.CENTER);
		itemCountContainer.setPreferredSize(new Dimension(450, 300));

		ingameContainer.addCustomPanelAsTab(savedSetupsContainer);

		leftPane.addComponent(itemCountContainer);
		leftPane.addComponent(ingameContainer);

		middlePane.addComponent(setupsContainer);
		middlePane.addComponent(bottomMiddlePane);

		rightPane.addComponent(runeContainer);

		suppliesMiscPanel.addComponent(createCustomPanel("Supplies", suppliesDefaults));
		suppliesMiscPanel.addComponent(createCustomPanel("Misc", miscDefaults));

		suppliesMiscPanel.setPreferredSize(new Dimension(0, 400));

		rightPane.addComponent(suppliesMiscPanel);
		rightPane.addComponent(createCustomPanel("Melee", meleeDefaults));
		rightPane.addComponent(createCustomPanel("Ranged", rangedDefaults));
		rightPane.addComponent(createCustomPanel("Magic", magicDefaults));

		leftPane.setPreferredSize(new Dimension(450, 0));
		middlePane.setPreferredSize(new Dimension(1300, 0));
		bottomMiddlePane.setPreferredSize(new Dimension(0, 320));
		rightPane.setPreferredSize(new Dimension(380, 0));

		mainPane.addComponent(leftPane);
		mainPane.addComponent(middlePane);
		mainPane.addComponent(rightPane);


		DockingPanel panel = new DockingPanel(PLUGIN_DIRECTORY + "misc-dir/docking.json");
		panel.setPanelFactory(name -> createCustomPanel(name, true));
		panel.registerPanel(recentItemsContainer);
		panel.init(mainPane);

		add(panel, BorderLayout.CENTER);

		dragGlassPane = new DragGlassPane();
		setGlassPane(dragGlassPane);
		dragGlassPane.setVisible(true);

		instance = this;

		addMouseWheelListener(new MouseAdapter()
		{
			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				if(IconGridPanel.SELECTED != null)
				{
					IconGridPanel.SELECTED.mouseWheelIncremented(e.getWheelRotation());
					e.consume();
				}
			}
		});

		this.setExtendedState(this.getExtendedState() | this.MAXIMIZED_BOTH);
	}

	private int selectedItem = -1;

	public void setSelectedItem(int selection)
	{
		selectedItem = selection;
		IconGridPanel.repaintAll();
		setups.updatePixelBoxes();
	}

	public int getSelectedItem()
	{
		return selectedItem;
	}

	public static SetupsWindow getInstance()
	{
		return instance;
	}

	public void showDragImage(Image image, Point initialPoint)
	{
		dragGlassPane.setDragImage(image, initialPoint);
	}

	public void updateDragImagePosition(int xOnScreen, int yOnScreen)
	{
		Point windowLocation = getLocationOnScreen();
		Point dragPoint = new Point(xOnScreen - windowLocation.x, yOnScreen - windowLocation.y);
		dragGlassPane.setDragPoint(dragPoint);
	}

	public void setTargetBox(PixelBox box)
	{
		dragGlassPane.setTargetBox(box);
	}

	public void hideDragImage()
	{
		dragGlassPane.clear();
	}

	public void addItemToRecent(int id)
	{
		if (recentItems != null)
		{
			recentItems.addRecentItem(id);
		}
	}

	private CustomPanel createCustomPanel(String name, List<Integer> defaultValues)
	{
		ItemDepot itemDepot = new ItemDepot(itemManager, this, name, false, defaultValues);


		CustomPanel itemContainer = new CustomPanel(name);
		itemContainer.getContentPanel().setLayout(new BorderLayout());
		itemContainer.getContentPanel().add(itemDepot, BorderLayout.CENTER);
		itemContainer.setPreferredSize(new Dimension(300, 300));
		return itemContainer;
	}

	private CustomPanel createCustomPanel(String name, boolean userCreated)
	{
		ItemDepot itemDepot = new ItemDepot(itemManager, this, name, userCreated);

		CustomPanel itemContainer = new CustomPanel(name);
		itemContainer.getContentPanel().setLayout(new BorderLayout());
		itemContainer.getContentPanel().add(itemDepot, BorderLayout.CENTER);
		itemContainer.setPreferredSize(new Dimension(300, 800));
		return itemContainer;
	}

	public void pushItemChanges()
	{
		if (itemCounts != null && setups.getItemCounts() != null)
		{
			itemCounts.replaceData(setups.getItemCounts());
		}
	}

	public SetupsContainer getSetupsContainer()
	{
		return setups;
	}

	public void saveInventory(List<Integer> inventory)
	{
		savedInventoryPanel.addSavedInventory(inventory, "");
	}

	public void saveEquipment(List<Integer> equipment)
	{
		savedEquipmentPanel.addSavedEquipment(equipment, "");
	}

	public void saveRunepouch(int a, int b, int c, int d)
	{
		savedRunePouchPanel.addSavedRunePouch(a, b, c, d);
	}

	public void saveSetup(SetupTemplate setup)
	{
		List<Integer> inv = setup.getInventory();
		int[] runes = new int[]{setup.getRuneA(), setup.getRuneB(), setup.getRuneC(), setup.getRuneD()};
		List<Integer> equip = setup.getEquipment();
		addSetupToSavedSetups(setup.getLabel(), inv, runes, equip, "");
	}

	public void saveAllSetups(List<SetupTemplate> setupsList)
	{
		int i = 1;
		for (SetupTemplate s : setupsList)
		{
			addSetupToSavedSetups(s.getLabel(), s.getInventory(), new int[]{s.getRuneA(), s.getRuneB(), s.getRuneC(), s.getRuneD()}, s.getEquipment(), "");
			i++;
		}
	}

	private void addSetupToSavedSetups(String topLabel, List<Integer> invIds, int[] runes, List<Integer> equipIds, String savedLabel)
	{
		savedSetupsPanel.addSavedSetup(topLabel, invIds, runes, equipIds, savedLabel);
	}

}
package com.advancedraidtracker.ui.docking;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;
import com.advancedraidtracker.ui.setups.ItemDepot;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedMenu;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedMenuItem;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedPanel;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedPopupMenu;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedSeperator;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedTabbedPane;
import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

public class CustomPanel extends JPanel
{

	private final JTabbedPane tabbedPane;
	private final String title;

	public String getTitle()
	{
		return title;
	}

	public JTabbedPane getTabbedPane()
	{
		return tabbedPane;
	}

	public void addTab(String title, Component component)
	{
		int index = tabbedPane.getTabCount();
		tabbedPane.addTab(title, component);
		setCustomTabComponent(tabbedPane, index);
		tabbedPane.setSelectedIndex(index);
	}

	public CustomPanel(String title)
	{
		this.title = title;
		setLayout(new BorderLayout());
		tabbedPane = getThemedTabbedPane();
		tabbedPane.setUI(new FlatTabbedPaneUI()
		{

			@Override
			protected void installDefaults()
			{
				super.installDefaults();
				underlineColor = config.markerColor();
				inactiveUnderlineColor = config.boxColor();
				setBackground(config.primaryDark());
				selectedBackground = config.primaryDark();
				cardTabSelectionHeight = 1;
				tabSelectionHeight = 1;
			}

			@Override
			protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight)
			{
				return fontHeight + 4;
			}

			@Override
			protected Insets getTabInsets(int tabPlacement, int tabIndex)
			{
				return new Insets(3, 5, 3, 5);
			}

		});


		setBackground(config.primaryDark());
		setOpaque(true);
		JPanel contentPanel = getThemedPanel();
		contentPanel.setLayout(new BorderLayout());
		tabbedPane.addTab(title, contentPanel);
		setCustomTabComponent(tabbedPane, 0);
		add(tabbedPane, BorderLayout.CENTER);
		new TabDragSource(tabbedPane);
		setDeselected();
		tabbedPane.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isRightMouseButton(e))
				{
					showRegistryPopup(e);
				}
			}
		});


	}

	public JPanel getContentPanel()
	{
		return (JPanel) tabbedPane.getComponentAt(0);
	}


	public void addCustomPanelAsTab(CustomPanel newPanel)
	{
		int newIndex = tabbedPane.getTabCount();
		Component childComp = newPanel.getTabbedPane().getComponentAt(0);
		String childTitle = newPanel.getTitle();

		tabbedPane.addTab(childTitle, childComp);
		setCustomTabComponent(tabbedPane, newIndex);
		tabbedPane.setSelectedIndex(newIndex);
		tabbedPane.revalidate();
		tabbedPane.repaint();
	}

	void setCustomTabComponent(JTabbedPane tabbedPane, int index)
	{
		JPanel tabComponent = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tabComponent.setOpaque(false);

		JLabel tabTitle = new JLabel(tabbedPane.getTitleAt(index));
		tabComponent.add(tabTitle);

		JButton closeButton = new JButton("x");
		closeButton.setFocusable(false);
		closeButton.setBorder(null);
		closeButton.setContentAreaFilled(false);

		closeButton.addActionListener(e -> {
			int idx = tabbedPane.indexOfTabComponent(tabComponent);
			if (idx != -1)
			{
				closeTab(idx);
			}
		});

		tabComponent.add(closeButton);

		tabbedPane.setTabComponentAt(index, tabComponent);
	}

	private void closeTab(int index)
	{
		Component comp = tabbedPane.getComponentAt(index);
		String closedTabTitle = tabbedPane.getTitleAt(index);

		DockingPanel dp = findDockingPanel();
		if (dp != null && comp != null && closedTabTitle != null && !closedTabTitle.isEmpty())
		{
			dp.registerClosedTabAsPanel(closedTabTitle, comp);
			dp.onLayoutChanged();
		}

		if (tabbedPane.getTabCount() == 0)
		{
			closePanel();
		}

	}

	void closePanel()
	{
		if (getParent() instanceof MultiSplitPane)
		{
			MultiSplitPane multiSplitPane = (MultiSplitPane) getParent();
			multiSplitPane.removeComponent(this);
			multiSplitPane.revalidate();
			multiSplitPane.repaint();
		}
	}

	private DockingPanel findDockingPanel()
	{
		Component c = this;
		while (c != null && !(c instanceof DockingPanel))
		{
			c = c.getParent();
		}
		return (DockingPanel) c;
	}

	private void showRegistryPopup(MouseEvent e)
	{
		DockingPanel dp = findDockingPanel();
		if (dp == null)
		{
			return;
		}

		JPopupMenu popup = getThemedPopupMenu();

		Map<String, CustomPanel> registry = dp.getRegistry();
		if (registry.isEmpty())
		{
			popup.add(getThemedMenuItem("(No Hidden Panels)"));
		}
		else
		{
			for (String panelTitle : registry.keySet())
			{
				JMenu panelMenu = getThemedMenu(panelTitle);
				JMenuItem openItem = getThemedMenuItem("Open");
				openItem.addActionListener(ae -> {
					CustomPanel toAdd = registry.get(panelTitle);
					addCustomPanelAsTab(toAdd);
					dp.removeFromRegistry(panelTitle);
					dp.onLayoutChanged();
				});
				panelMenu.add(openItem);
				AtomicReference<CustomPanel> cp = new AtomicReference<>(registry.get(panelTitle));
				Component mainComp = cp.get().getContentPanel().getComponent(0);
				boolean canDelete = false;
				if (mainComp instanceof ItemDepot)
				{
					canDelete = ((ItemDepot) mainComp).isUserCreated();
				}

				if (canDelete)
				{
					JMenuItem deleteItem = getThemedMenuItem("Delete");
					deleteItem.addActionListener(ae -> {
						cp.set(registry.remove(panelTitle));
						if (cp.get() != null)
						{
							((ItemDepot) mainComp).deleteFile();
						}
						dp.onLayoutChanged();
					});
					panelMenu.add(deleteItem);
				}

				popup.add(panelMenu);
			}
		}

		popup.add(getThemedSeperator());
		popup.add(getThemedMenuItem("Add New Item Container...")).addActionListener(ae -> {
			String name = JOptionPane.showInputDialog(this, "Enter name for new item container:", "New Item Container", JOptionPane.PLAIN_MESSAGE);
			if (name != null && !name.trim().isEmpty())
			{
				name = name.trim();
				if (panelNameExists(dp, name))
				{
					JOptionPane.showMessageDialog(this, "A panel with this name already exists.", "Error", JOptionPane.ERROR_MESSAGE);
				}
				else
				{
					CustomPanel newPanel = dp.createOrGetPanel(name);
					addCustomPanelAsTab(newPanel);
					dp.onLayoutChanged();
				}
			}
		});

		popup.add(getThemedSeperator());
		popup.add(getThemedMenuItem("Reset UI To Default")).addActionListener(ae -> {
			int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to reset the UI to default?", "Confirm Reset", JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION)
			{
				dp.resetToDefault();
			}
		});

		popup.show(e.getComponent(), e.getX(), e.getY());
	}


	private boolean panelNameExists(DockingPanel dp, String name)
	{
		if (dp.getRegistry().containsKey(name) || dp.getCustomPanelMap().containsKey(name))
		{
			return true;
		}

		Set<String> allNames = collectAllPanelNames(dp.mainPane);
		return allNames.contains(name);
	}

	private Set<String> collectAllPanelNames(MultiSplitPane pane)
	{
		Set<String> names = new HashSet<>();
		for (Component c : pane.getComponents())
		{
			if (c instanceof MultiSplitPane)
			{
				names.addAll(collectAllPanelNames((MultiSplitPane) c));
			}
			else if (c instanceof CustomPanel)
			{
				CustomPanel cp = (CustomPanel) c;
				names.add(cp.getTitle());

				int tabCount = cp.getTabbedPane().getTabCount();
				for (int i = 1; i < tabCount; i++)
				{
					String childTitle = cp.getTabbedPane().getTitleAt(i);
					names.add(childTitle);
				}
			}
		}
		return names;
	}

	private static class TabDragSource implements DragGestureListener, DragSourceListener
	{
		private final DragSource dragSource;
		private final JTabbedPane tabbedPane;
		private Point lastDragLocation;
		private int draggedTabIndex;

		public TabDragSource(JTabbedPane tabbedPane)
		{
			this.tabbedPane = tabbedPane;
			dragSource = new DragSource();
			dragSource.createDefaultDragGestureRecognizer(tabbedPane, DnDConstants.ACTION_MOVE, this);
		}

		@Override
		public void dragGestureRecognized(DragGestureEvent dge)
		{
			Point tabPt = dge.getDragOrigin();
			draggedTabIndex = tabbedPane.indexAtLocation(tabPt.x, tabPt.y);
			if (draggedTabIndex >= 0)
			{
				Transferable transferable = new TabTransferable(tabbedPane, draggedTabIndex);
				dragSource.startDrag(dge, DragSource.DefaultMoveDrop, transferable, this);
			}
		}

		private void updateLastDragLocation(DragSourceDragEvent dsde)
		{
			lastDragLocation = dsde.getLocation();
		}

		@Override
		public void dragEnter(DragSourceDragEvent dsde)
		{
			updateLastDragLocation(dsde);
		}

		@Override
		public void dragOver(DragSourceDragEvent dsde)
		{
			updateLastDragLocation(dsde);
		}

		@Override
		public void dropActionChanged(DragSourceDragEvent dsde)
		{
			updateLastDragLocation(dsde);
		}

		@Override
		public void dragExit(DragSourceEvent dse)
		{
		}

		@Override
		public void dragDropEnd(DragSourceDropEvent dsde)
		{
			if (!dsde.getDropSuccess())
			{
				if (lastDragLocation != null)
				{
					SwingUtilities.invokeLater(() -> createNewWindowWithTab(lastDragLocation));
				}
			}
		}

		private void createNewWindowWithTab(Point locationOnScreen)
		{
			Component draggedComponent = tabbedPane.getComponentAt(draggedTabIndex);
			String draggedTitle = tabbedPane.getTitleAt(draggedTabIndex);

			tabbedPane.remove(draggedComponent);
			if (tabbedPane.getTabCount() == 0)
			{
				CustomPanel parentPanel = (CustomPanel) SwingUtilities.getAncestorOfClass(CustomPanel.class, tabbedPane);
				if (parentPanel != null)
				{
					parentPanel.closePanel();
				}
			}

			JFrame newFrame = new JFrame();
			newFrame.setSize(400, 300);
			newFrame.setLocation(locationOnScreen);

			CustomPanel newCustomPanel = new CustomPanel(draggedTitle);


			MultiSplitPane newMainPane = new MultiSplitPane(true);
			newMainPane.addComponent(newCustomPanel);

			DockingPanel newDockingPanel = new DockingPanel("test.json");
			newDockingPanel.init(newMainPane);

			CustomLayerUI layerUI = new CustomLayerUI();
			layerUI.setRootMultiSplitPane(newMainPane);
			JLayer<JComponent> layer = new JLayer<>(newMainPane, layerUI);
			layer.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
			layerUI.setCustomLayerListener(newDockingPanel);
			new GlobalDropTarget(layer, layerUI, newMainPane, newDockingPanel);

			newFrame.setContentPane(layer);
			newFrame.setVisible(true);
		}
	}


	public void setSelected()
	{
		setBorder(BorderFactory.createLineBorder(new Color(45, 140, 235)));
		revalidate();
		repaint();
	}

	public void setDeselected()
	{
		setBorder(BorderFactory.createLineBorder(new Color(40, 40, 40)));
		revalidate();
		repaint();
	}

}


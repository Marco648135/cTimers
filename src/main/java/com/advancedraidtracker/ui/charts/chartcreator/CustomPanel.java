package com.advancedraidtracker.ui.charts.chartcreator;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;
import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class CustomPanel extends JPanel
{

	private final JTabbedPane tabbedPane;


	public void addTab(String title, Component component)
	{
		int index = tabbedPane.getTabCount();
		tabbedPane.addTab(title, component);
		setCustomTabComponent(tabbedPane, index);
		tabbedPane.setSelectedIndex(index);
	}

	public CustomPanel(String title)
	{
		setLayout(new BorderLayout());
		tabbedPane = new JTabbedPane();
		tabbedPane.setUI(new FlatTabbedPaneUI()
		{

			@Override
			protected void installDefaults()
			{
				super.installDefaults();
				underlineColor = config.boxColor();
				inactiveUnderlineColor = config.boxColor();
				setBackground(config.primaryDark());
				selectedBackground = config.primaryDark();
				cardTabSelectionHeight = 2;
				tabSelectionHeight = 2;
			}

			@Override
			protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight)
			{
				return fontHeight + 6;
			}

			@Override
			protected Insets getTabInsets(int tabPlacement, int tabIndex)
			{
				return new Insets(3, 5, 3, 5);
			}

		});


		JPanel contentPanel = new JPanel(new BorderLayout());
		tabbedPane.addTab(title, contentPanel);
		setCustomTabComponent(tabbedPane, 0);
		add(tabbedPane, BorderLayout.CENTER);
		new TabDragSource(tabbedPane);
	}

	public JPanel getContentPanel()
	{
		return (JPanel) tabbedPane.getComponentAt(0);
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
		tabbedPane.remove(index);
		if (tabbedPane.getTabCount() == 0)
		{
			closePanel();
		}
	}

	void closePanel()
	{
		Container parent = getParent();
		if (parent instanceof MultiSplitPane)
		{
			MultiSplitPane splitPane = (MultiSplitPane) parent;
			splitPane.removeComponent(this);
		}
		else if (parent != null)
		{
			parent.remove(this);
			parent.revalidate();
			parent.repaint();
		}
	}

	private static class TabDragSource implements DragGestureListener, DragSourceListener
	{

		private final DragSource dragSource;
		private final JTabbedPane tabbedPane;

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
			int draggedTabIndex = tabbedPane.indexAtLocation(tabPt.x, tabPt.y);
			if (draggedTabIndex >= 0)
			{
				Transferable transferable = new TabTransferable(tabbedPane, draggedTabIndex);
				dragSource.startDrag(dge, DragSource.DefaultMoveDrop, transferable, this);
			}
		}

		@Override
		public void dragEnter(DragSourceDragEvent dsde)
		{
		}

		@Override
		public void dragOver(DragSourceDragEvent dsde)
		{
		}

		@Override
		public void dropActionChanged(DragSourceDragEvent dsde)
		{
		}

		@Override
		public void dragExit(DragSourceEvent dse)
		{
		}

		@Override
		public void dragDropEnd(DragSourceDropEvent dsde)
		{
		}
	}

}

class TabTransferable implements Transferable
{

	public static final DataFlavor TAB_INDEX_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.lang.Integer", "Tab Index");
	public static final DataFlavor TABBED_PANE_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=javax.swing.JTabbedPane", "Tabbed Pane");


	private final DataFlavor[] flavors = {TAB_INDEX_FLAVOR, TABBED_PANE_FLAVOR};

	private final JTabbedPane tabbedPane;
	private final int tabIndex;

	public TabTransferable(JTabbedPane tabbedPane, int tabIndex)
	{
		this.tabbedPane = tabbedPane;
		this.tabIndex = tabIndex;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors()
	{
		return flavors;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
		for (DataFlavor f : flavors)
		{
			if (f.equals(flavor))
			{
				return true;
			}
		}
		return false;
	}

	@NonNull
	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
	{
		if (flavor.equals(TAB_INDEX_FLAVOR))
		{
			return tabIndex;
		}
		else if (flavor.equals(TABBED_PANE_FLAVOR))
		{
			return tabbedPane;
		}
		else
		{
			throw new UnsupportedFlavorException(flavor);
		}
	}
}

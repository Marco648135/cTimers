package com.advancedraidtracker.ui.charts.chartcreator;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

public class CustomPanel extends JPanel
{

	private JLabel titleLabel;
	private JButton closeButton;
	private JPanel headerPanel;
	private JPanel contentPanel;

	public String title;

	public static final DataFlavor PANEL_FLAVOR = new DataFlavor(
		DataFlavor.javaJVMLocalObjectMimeType + ";class=" + CustomPanel.class.getName(),
		"CustomPanel");

	public CustomPanel(String title, OverlayPane overlayPane)
	{
		this.title = title;
		setLayout(new BorderLayout());

		// Header panel with title and close button
		headerPanel = new JPanel(new BorderLayout());
		headerPanel.setPreferredSize(new Dimension(0, 25));

		titleLabel = new JLabel(title);
		titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
		closeButton = new JButton("x");
		closeButton.setFocusable(false);
		closeButton.setBorderPainted(false);
		closeButton.setContentAreaFilled(false);
		closeButton.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

		headerPanel.add(titleLabel, BorderLayout.CENTER);
		headerPanel.add(closeButton, BorderLayout.EAST);

		add(headerPanel, BorderLayout.NORTH);

		// Content panel
		contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout());
		add(contentPanel, BorderLayout.CENTER);

		// Close button action
		closeButton.addActionListener(e -> closePanel());

		// Make header draggable
		headerPanel.setTransferHandler(new PanelTransferHandler());
		headerPanel.addMouseListener(new DragMouseAdapter());

		// Set up TransferHandler for the panel to accept drops
		this.setTransferHandler(new PanelTransferHandler());

		// Set up DragSource
		DragSource dragSource = DragSource.getDefaultDragSource();
		dragSource.createDefaultDragGestureRecognizer(headerPanel, DnDConstants.ACTION_MOVE, new DragGestureListener()
		{
			@Override
			public void dragGestureRecognized(DragGestureEvent dge)
			{
				// Start the drag
				Transferable transferable = new PanelTransferable(CustomPanel.this);
				// Set the drag image if desired
				dge.startDrag(DragSource.DefaultMoveDrop, transferable, new TabDragSourceListener(overlayPane));
			}
		});

		// Set up DropTarget
		new DropTarget(this, DnDConstants.ACTION_MOVE, new PanelDropTargetListener(overlayPane, this), true);


	}

	private void closePanel()
	{
		// Start with the parent of this panel
		Container parent = getParent();

		// First, check if the parent is a JTabbedPane
		if (parent instanceof JTabbedPane)
		{
			JTabbedPane tabbedPane = (JTabbedPane) parent;
			int index = tabbedPane.indexOfComponent(this);
			if (index != -1)
			{
				tabbedPane.remove(index);
			}

			// If tabbedPane is empty after removal, remove it from its parent
			if (tabbedPane.getTabCount() == 0)
			{
				Container tabParent = tabbedPane.getParent();
				if (tabParent instanceof MultiSplitPane)
				{
					MultiSplitPane splitPane = (MultiSplitPane) tabParent;
					splitPane.removeComponent(tabbedPane);
				}
				else
				{
					// If not, remove tabbedPane directly
					tabParent.remove(tabbedPane);
					tabParent.revalidate();
					tabParent.repaint();
				}
			}
		}
		else
		{
			// Traverse up the component hierarchy to find a MultiSplitPane
			Container parentContainer = parent;
			while (parentContainer != null && !(parentContainer instanceof MultiSplitPane))
			{
				parentContainer = parentContainer.getParent();
			}

			if (parentContainer instanceof MultiSplitPane)
			{
				MultiSplitPane splitPane = (MultiSplitPane) parentContainer;
				splitPane.removeComponent(this);
			}
			else if (parent != null)
			{
				// If no MultiSplitPane found, remove from immediate parent
				parent.remove(this);
				parent.revalidate();
				parent.repaint();
			}
		}
	}


	public JPanel getContentPanel()
	{
		return contentPanel;
	}

	// Resizing logic
	private class Resizable extends MouseAdapter
	{
		private static final int BORDER = 5;
		private int cursor;
		private Point startPos = null;

		@Override
		public void mouseMoved(MouseEvent e)
		{
			if (isOnBorder(e))
			{
				setCursor(Cursor.getPredefinedCursor(cursor));
			}
			else
			{
				setCursor(Cursor.getDefaultCursor());
			}
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
			setCursor(Cursor.getDefaultCursor());
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			if (isOnBorder(e))
			{
				startPos = e.getPoint();
			}
			else
			{
				startPos = null;
			}
		}

		@Override
		public void mouseDragged(MouseEvent e)
		{
			if (startPos != null)
			{
				int dx = e.getX() - startPos.x;
				int dy = e.getY() - startPos.y;

				Rectangle bounds = getBounds();

				if (cursor == Cursor.N_RESIZE_CURSOR)
				{
					bounds.y += dy;
					bounds.height -= dy;
				}
				else if (cursor == Cursor.S_RESIZE_CURSOR)
				{
					bounds.height += dy;
				}
				else if (cursor == Cursor.W_RESIZE_CURSOR)
				{
					bounds.x += dx;
					bounds.width -= dx;
				}
				else if (cursor == Cursor.E_RESIZE_CURSOR)
				{
					bounds.width += dx;
				}
				else if (cursor == Cursor.NW_RESIZE_CURSOR)
				{
					bounds.x += dx;
					bounds.width -= dx;
					bounds.y += dy;
					bounds.height -= dy;
				}
				else if (cursor == Cursor.NE_RESIZE_CURSOR)
				{
					bounds.width += dx;
					bounds.y += dy;
					bounds.height -= dy;
				}
				else if (cursor == Cursor.SW_RESIZE_CURSOR)
				{
					bounds.x += dx;
					bounds.width -= dx;
					bounds.height += dy;
				}
				else if (cursor == Cursor.SE_RESIZE_CURSOR)
				{
					bounds.width += dx;
					bounds.height += dy;
				}

				setBounds(bounds);
				getParent().revalidate();
			}
		}

		private boolean isOnBorder(MouseEvent e)
		{
			int x = e.getX();
			int y = e.getY();
			int width = getWidth();
			int height = getHeight();

			if (x < BORDER && y < BORDER)
			{
				cursor = Cursor.NW_RESIZE_CURSOR;
			}
			else if (x > width - BORDER && y < BORDER)
			{
				cursor = Cursor.NE_RESIZE_CURSOR;
			}
			else if (x < BORDER && y > height - BORDER)
			{
				cursor = Cursor.SW_RESIZE_CURSOR;
			}
			else if (x > width - BORDER && y > height - BORDER)
			{
				cursor = Cursor.SE_RESIZE_CURSOR;
			}
			else if (x < BORDER)
			{
				cursor = Cursor.W_RESIZE_CURSOR;
			}
			else if (x > width - BORDER)
			{
				cursor = Cursor.E_RESIZE_CURSOR;
			}
			else if (y < BORDER)
			{
				cursor = Cursor.N_RESIZE_CURSOR;
			}
			else if (y > height - BORDER)
			{
				cursor = Cursor.S_RESIZE_CURSOR;
			}
			else
			{
				cursor = Cursor.DEFAULT_CURSOR;
				return false;
			}
			return true;
		}
	}

	// TransferHandler for drag-and-drop
	private class PanelTransferHandler extends TransferHandler
	{

		private Shape[] calculateDropShapes(Rectangle panelBounds)
		{
			int w = panelBounds.width;
			int h = panelBounds.height;
			int x = panelBounds.x;
			int y = panelBounds.y;

			int w25 = w / 4;
			int h25 = h / 4;
			int w75 = 3 * w / 4;
			int h75 = 3 * h / 4;

			Point TL = new Point(x, y);
			Point TR = new Point(x + w, y);
			Point BL = new Point(x, y + h);
			Point BR = new Point(x + w, y + h);

			// Top Trapezoid
			Polygon topTrapezoid = new Polygon();
			topTrapezoid.addPoint(TL.x, TL.y);
			topTrapezoid.addPoint(TR.x, TR.y);
			topTrapezoid.addPoint(TR.x - w25, TR.y + h25);
			topTrapezoid.addPoint(TL.x + w25, TL.y + h25);

			// Bottom Trapezoid
			Polygon bottomTrapezoid = new Polygon();
			bottomTrapezoid.addPoint(BL.x, BL.y);
			bottomTrapezoid.addPoint(BR.x, BR.y);
			bottomTrapezoid.addPoint(BR.x - w25, BR.y - h25);
			bottomTrapezoid.addPoint(BL.x + w25, BL.y - h25);

			// Left Trapezoid
			Polygon leftTrapezoid = new Polygon();
			leftTrapezoid.addPoint(TL.x, TL.y);
			leftTrapezoid.addPoint(BL.x, BL.y);
			leftTrapezoid.addPoint(BL.x + w25, BL.y - h25);
			leftTrapezoid.addPoint(TL.x + w25, TL.y + h25);

			// Right Trapezoid
			Polygon rightTrapezoid = new Polygon();
			rightTrapezoid.addPoint(TR.x, TR.y);
			rightTrapezoid.addPoint(BR.x, BR.y);
			rightTrapezoid.addPoint(BR.x - w25, BR.y - h25);
			rightTrapezoid.addPoint(TR.x - w25, TR.y + h25);

			// Center Rectangle
			Polygon centerRectangle = new Polygon();
			centerRectangle.addPoint(x + w25, y + h25);
			centerRectangle.addPoint(x + w75, y + h25);
			centerRectangle.addPoint(x + w75, y + h75);
			centerRectangle.addPoint(x + w25, y + h75);

			return new Shape[]{topTrapezoid, rightTrapezoid, bottomTrapezoid, leftTrapezoid, centerRectangle};
		}

		@Override
		public int getSourceActions(JComponent c)
		{
			return MOVE;
		}

		@Override
		protected Transferable createTransferable(JComponent c)
		{
			return new PanelTransferable(CustomPanel.this);
		}

		@Override
		public boolean canImport(TransferSupport support)
		{
			if (!support.isDataFlavorSupported(PANEL_FLAVOR))
			{
				return false;
			}
			if (support.getComponent() instanceof CustomPanel)
			{
				return true;
			}
			return false;
		}

		@Override
		public boolean importData(TransferSupport support)
		{
			if (!canImport(support))
			{
				return false;
			}

			try
			{
				CustomPanel draggedPanel = (CustomPanel) support.getTransferable().getTransferData(PANEL_FLAVOR);
				Component dropComponent = support.getComponent();
				Point dropPoint = support.getDropLocation().getDropPoint();

				// Get the glass pane and highlighted index
				OverlayPane overlayPane = (OverlayPane) SwingUtilities.getRootPane(dropComponent).getGlassPane();
				int highlightedIndex = overlayPane.getHighlightedIndex();

				// Hide the overlay
				overlayPane.setVisible(false);

				// Perform the drop action based on highlightedIndex
				if (highlightedIndex == -1)
				{
					// No valid drop target, cancel the drop
					return false;
				}

				// Remove the dragged panel from its parent
				Container draggedParent = draggedPanel.getParent();
				if (draggedParent instanceof MultiSplitPane)
				{
					MultiSplitPane draggedSplitPane = (MultiSplitPane) draggedParent;
					draggedSplitPane.removeComponent(draggedPanel);
				}
				else if (draggedParent instanceof JTabbedPane)
				{
					JTabbedPane tabbedPane = (JTabbedPane) draggedParent;
					tabbedPane.remove(draggedPanel);
				}

				// Get the target MultiSplitPane
				MultiSplitPane targetSplitPane = getAncestorSplitPane(dropComponent);
				if (targetSplitPane == null)
				{
					return false;
				}

				// Insert the dragged panel into the targetSplitPane based on highlightedIndex
				// 0: Top, 1: Right, 2: Bottom, 3: Left, 4: Center
				switch (highlightedIndex)
				{
					case 0: // Top
						targetSplitPane.addComponent(draggedPanel, true, true);
						break;
					case 1: // Right
						targetSplitPane.addComponent(draggedPanel, true, false);
						break;
					case 2: // Bottom
						targetSplitPane.addComponent(draggedPanel, true, true);
						break;
					case 3: // Left
						targetSplitPane.addComponent(draggedPanel, true, false);
						break;
					case 4: // Center
						// Handle center drop (e.g., merge into tabbed pane)
						// ... (implementation depends on your desired behavior)
						break;
					default:
						return false;
				}

				return true;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			return false;
		}

		private MultiSplitPane getAncestorSplitPane(Component comp)
		{
			while (comp != null)
			{
				if (comp instanceof MultiSplitPane)
				{
					return (MultiSplitPane) comp;
				}
				comp = comp.getParent();
			}
			return null;
		}


	}

	// MouseAdapter to initiate drag
// In CustomPanel class
	private class DragMouseAdapter extends MouseAdapter
	{
		@Override
		public void mousePressed(MouseEvent e)
		{
			JComponent c = (JComponent) e.getSource();
			c.getTransferHandler().exportAsDrag(c, e, TransferHandler.MOVE);
		}
	}

	public static class PanelTransferable implements Transferable
	{
		private CustomPanel panel;

		public PanelTransferable(CustomPanel panel)
		{
			this.panel = panel;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors()
		{
			return new DataFlavor[]{PANEL_FLAVOR};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor)
		{
			return flavor.equals(PANEL_FLAVOR);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
		{
			if (!isDataFlavorSupported(flavor))
			{
				throw new UnsupportedFlavorException(flavor);
			}
			return panel;
		}
	}

}

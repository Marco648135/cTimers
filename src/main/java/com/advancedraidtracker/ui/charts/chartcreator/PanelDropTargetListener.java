package com.advancedraidtracker.ui.charts.chartcreator;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

public class PanelDropTargetListener implements DropTargetListener
{

	private static Shape[] calculateDropShapes(Rectangle panelBounds)
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

	private OverlayPane overlayPane;
	private CustomPanel targetPanel;

	public PanelDropTargetListener(OverlayPane overlayPane, CustomPanel targetPanel)
	{
		this.overlayPane = overlayPane;
		this.targetPanel = targetPanel;
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde)
	{
		// Get the location of the targetPanel relative to the overlayPane
		Point panelLocation = SwingUtilities.convertPoint(targetPanel.getParent(), targetPanel.getLocation(), overlayPane);

		// Create a new Rectangle with the converted point and the size of the targetPanel
		Rectangle boundsInOverlayPane = new Rectangle(panelLocation, targetPanel.getSize());

		// Calculate and set the overlay shapes using the converted bounds
		Shape[] shapes = calculateDropShapes(boundsInOverlayPane);
		overlayPane.setShapes(shapes);
		overlayPane.setVisible(true);
	}


	@Override
	public void dragOver(DropTargetDragEvent dtde)
	{
		// Update the overlay pane's mouse position
		Point mouseLocation = dtde.getLocation();
		overlayPane.updateMousePosition(mouseLocation);
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde)
	{
		// Not needed for our purpose
	}

	@Override
	public void dragExit(DropTargetEvent dte)
	{
		// Hide the overlay when exiting the drop target
		overlayPane.setVisible(false);
	}

	@Override
	public void drop(DropTargetDropEvent dtde)
	{
		// Handle the drop action
		overlayPane.setVisible(false);
		Transferable transferable = dtde.getTransferable();
		try
		{
			CustomPanel draggedPanel = (CustomPanel) transferable.getTransferData(CustomPanel.PANEL_FLAVOR);

			int highlightedIndex = overlayPane.getHighlightedIndex();

			if (highlightedIndex == -1)
			{
				// No valid drop target, reject drop
				dtde.rejectDrop();
				return;
			}

			dtde.acceptDrop(DnDConstants.ACTION_MOVE);

			// Remove the dragged panel from its parent
			Container draggedParent = draggedPanel.getParent();
			if (draggedParent instanceof MultiSplitPane)
			{
				((MultiSplitPane) draggedParent).removeComponent(draggedPanel);
			}
			else if (draggedParent instanceof JTabbedPane)
			{
				((JTabbedPane) draggedParent).remove(draggedPanel);
			}

			// Insert the dragged panel into the target panel based on highlightedIndex
			MultiSplitPane targetSplitPane = getAncestorSplitPane(targetPanel);

			if (targetSplitPane == null)
			{
				// Create a new MultiSplitPane if necessary
				boolean orientation = true; // default to vertical
				MultiSplitPane parentSplitPane = getAncestorSplitPane(targetPanel.getParent());
				if (parentSplitPane != null)
				{
					orientation = parentSplitPane.isVerticalOrientation();
				}
				targetSplitPane = new MultiSplitPane(orientation);
				targetSplitPane.addComponent(targetPanel);
				Container parent = targetPanel.getParent();
				parent.remove(targetPanel);
				parent.add(targetSplitPane);
			}

			// Implement splitting logic based on highlightedIndex
			// For simplicity, let's assume splitting the target panel
			// 0: Top, 1: Right, 2: Bottom, 3: Left, 4: Center
			switch (highlightedIndex)
			{
				case 0: // Top
					targetSplitPane.splitComponent(targetPanel, draggedPanel, true, true);
					break;
				case 1: // Right
					targetSplitPane.splitComponent(targetPanel, draggedPanel, false, false);
					break;
				case 2: // Bottom
					targetSplitPane.splitComponent(targetPanel, draggedPanel, true, false);
					break;
				case 3: // Left
					targetSplitPane.splitComponent(targetPanel, draggedPanel, false, true);
					break;
				case 4: // Center (merge into tabbed pane)
					targetSplitPane.mergeIntoTabbedPane(targetPanel, draggedPanel);
					break;
			}

			dtde.dropComplete(true);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			dtde.dropComplete(false);
		}
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


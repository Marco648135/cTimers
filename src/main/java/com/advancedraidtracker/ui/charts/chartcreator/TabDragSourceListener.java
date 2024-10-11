package com.advancedraidtracker.ui.charts.chartcreator;

import java.awt.Point;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import javax.swing.SwingUtilities;

public class TabDragSourceListener implements DragSourceListener
{
	private OverlayPane overlayPane;

	public TabDragSourceListener(OverlayPane overlayPane)
	{
		this.overlayPane = overlayPane;
	}

	@Override
	public void dragEnter(DragSourceDragEvent dsde)
	{
		// Not needed for our purpose
	}

	@Override
	public void dragOver(DragSourceDragEvent dsde)
	{
		// Update the overlay pane's position and visibility
		Point mouseLocation = dsde.getLocation();
		SwingUtilities.convertPointFromScreen(mouseLocation, overlayPane);
		overlayPane.updateMousePosition(mouseLocation);
	}

	@Override
	public void dropActionChanged(DragSourceDragEvent dsde)
	{
		// Not needed for our purpose
	}

	@Override
	public void dragExit(DragSourceEvent dse)
	{
		// Hide the overlay when exiting drag
		overlayPane.setVisible(false);
	}

	@Override
	public void dragDropEnd(DragSourceDropEvent dsde)
	{
		// Hide the overlay when drag ends
		overlayPane.setVisible(false);
	}
}

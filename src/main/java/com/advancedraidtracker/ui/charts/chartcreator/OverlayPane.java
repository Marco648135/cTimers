package com.advancedraidtracker.ui.charts.chartcreator;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JComponent;

public class OverlayPane extends JComponent
{
	private Shape[] shapes;
	private int highlightedIndex = -1;

	public OverlayPane()
	{
		setOpaque(false);
	}

	public void setShapes(Shape[] shapes)
	{
		this.shapes = shapes;
		repaint();
	}

	public void updateMousePosition(Point p)
	{
		if (shapes == null)
		{
			return;
		}
		int newIndex = -1;
		for (int i = 0; i < shapes.length; i++)
		{
			if (shapes[i].contains(p))
			{
				newIndex = i;
				break;
			}
		}
		if (newIndex != highlightedIndex)
		{
			highlightedIndex = newIndex;
			repaint();
		}
	}


	@Override
	protected void paintComponent(Graphics g)
	{
		if (shapes == null)
		{
			return;
		}
		Graphics2D g2d = (Graphics2D) g.create();
		for (int i = 0; i < shapes.length; i++)
		{
			if (i == highlightedIndex)
			{
				g2d.setColor(new Color(45, 140, 235, 100)); // Blue with 50% opacity
			}
			else
			{
				Color base = config.boxColor();
				g2d.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 100)); // Gray with 50% opacity
			}
			g2d.fill(shapes[i]);
		}
		g2d.dispose();
	}

	public int getHighlightedIndex()
	{
		return highlightedIndex;
	}
}


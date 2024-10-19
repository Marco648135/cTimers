package com.advancedraidtracker.ui.charts.chartcreator;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import javax.swing.JComponent;
import lombok.Getter;

public class OverlayPane extends JComponent
{
	private Shape[] shapes;
	@Getter
	private int highlightedIndex = -1;

	public OverlayPane()
	{
		setOpaque(false);
		setFocusable(false);
		enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
	}

	public void setShapes(Shape[] shapes)
	{
		this.shapes = shapes;
		shapesNeedUpdate = false; // Prevent overriding shapes during painting
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
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g.create();
		Composite oldComposite = g2d.getComposite();
		Composite newComposite = AlphaComposite.SrcOver.derive(0.5f);
		g2d.setComposite(newComposite);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Color hoveredColor = new Color(45, 140, 235, 100);
		Color highlightedColor = new Color(45, 140, 235, 255);
		Color outlineColor = new Color(160, 160, 160, 200);
		Color inactiveColor = new Color(50, 50, 50, 230);

		if (shapes == null || shapesNeedUpdate)
		{
			defineShapes(getWidth(), getHeight());
			shapesNeedUpdate = false;
		}

		for (int i = 0; i < shapes.length; i++)
		{
			Shape shape = shapes[i];
			if (shape != null)
			{
				if (highlightedIndex == i)
				{
					g2d.setColor(hoveredColor);
					g2d.fill(shape);
					g2d.setStroke(new BasicStroke(1));
					g2d.setComposite(oldComposite);
					g2d.setColor(highlightedColor);
					g2d.draw(shape);
					g2d.setComposite(newComposite);
				}
				else
				{
					if(shapes.length > 4) //only draw inactive shapes if its trapezoids, not rectangles
					{
						g2d.setColor(inactiveColor);
						g2d.fill(shape);
						g2d.setColor(outlineColor);
						g2d.draw(shape);
					}
				}
			}
		}

		g2d.dispose();
	}

	private boolean shapesNeedUpdate = true;

	private void defineShapes(int width, int height)
	{
		shapes = DockingUtility.getTrapezoids(width, height, 0, 0);
	}

	@Override
	public void setSize(int width, int height)
	{
		super.setSize(width, height);
		if (shapes == null)
		{
			shapesNeedUpdate = true;
		}
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		super.setBounds(x, y, width, height);
		if (shapes == null)
		{
			shapesNeedUpdate = true;
		}
	}

}



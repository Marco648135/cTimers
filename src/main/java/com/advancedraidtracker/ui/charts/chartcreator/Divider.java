package com.advancedraidtracker.ui.charts.chartcreator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Divider extends JPanel
{
	public static final int DIVIDER_SIZE = 1;
	private final boolean verticalOrientation;
	private final MultiSplitPane parentPane;
	private Point initialClick;

	public Divider(boolean verticalOrientation, MultiSplitPane parentPane)
	{
		this.verticalOrientation = verticalOrientation;
		this.parentPane = parentPane;
		setBackground(new Color(45, 140, 235, 100));
		setCursor(verticalOrientation ? Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR) : Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));

		if (verticalOrientation)
		{
			setMaximumSize(new Dimension(1, Integer.MAX_VALUE));
		}
		else
		{
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		}

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				initialClick = e.getPoint();
			}
		});

		addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent e)
			{
				int delta = verticalOrientation ? e.getY() - initialClick.y : e.getX() - initialClick.x;
				parentPane.resizeComponents(Divider.this, delta);
			}
		});
	}
}


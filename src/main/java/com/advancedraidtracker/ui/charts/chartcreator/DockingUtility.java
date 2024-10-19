package com.advancedraidtracker.ui.charts.chartcreator;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;

public class DockingUtility
{
	public static Shape[] getTrapezoids(int width, int height, int x, int y)
	{
		Shape[] shapes = new Shape[5];
		int marginX = Math.min(Math.max(50, (int) (width * 0.1)), width / 2);
		int marginY = Math.min(Math.max(50, (int) (height * 0.1)), height / 2);

		int rectWidth = width - 2 * marginX;
		int rectHeight = height - 2 * marginY;
		Rectangle centerRectangle = new Rectangle(marginX + x, marginY + y, rectWidth, rectHeight);
		shapes[4] = centerRectangle;

		Polygon topTrapezoid = new Polygon();
		topTrapezoid.addPoint(x, y);
		topTrapezoid.addPoint(x + width, y);
		topTrapezoid.addPoint(x + width - marginX, y + marginY);
		topTrapezoid.addPoint(x + marginX, y + marginY);
		shapes[0] = topTrapezoid;

		Polygon rightTrapezoid = new Polygon();
		rightTrapezoid.addPoint(x + width, y);
		rightTrapezoid.addPoint(x + width, y + height);
		rightTrapezoid.addPoint(x + width - marginX, y + height - marginY);
		rightTrapezoid.addPoint(x + width - marginX, y + marginY);
		shapes[1] = rightTrapezoid;

		Polygon bottomTrapezoid = new Polygon();
		bottomTrapezoid.addPoint(x, height + y);
		bottomTrapezoid.addPoint(x + width, y + height);
		bottomTrapezoid.addPoint(x + width - marginX, y + height - marginY);
		bottomTrapezoid.addPoint(x + marginX, y + height - marginY);
		shapes[2] = bottomTrapezoid;

		Polygon leftTrapezoid = new Polygon();
		leftTrapezoid.addPoint(x, y);
		leftTrapezoid.addPoint(x, y + height);
		leftTrapezoid.addPoint(x + marginX, y + height - marginY);
		leftTrapezoid.addPoint(x + marginX, y + marginY);
		shapes[3] = leftTrapezoid;
		return shapes;
	}
}

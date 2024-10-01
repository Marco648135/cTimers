package com.advancedraidtracker.ui.charts.chartresources;

import com.advancedraidtracker.ui.charts.ChartPanel;
import java.awt.Graphics2D;

public class ChartAuto implements ChartElement
{
	public int tick;

	public ChartAuto(int tick)
	{
		this.tick = tick;
	}

	@Override
	public int getTick()
	{
		return tick;
	}

	@Override
	public void draw(Graphics2D g, ChartPanel chartPanel)
	{
		// Drawing logic for autos

	}
}
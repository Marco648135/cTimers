package com.advancedraidtracker.ui.charts.chartresources;

import com.advancedraidtracker.ui.charts.ChartPanel;
import java.awt.Graphics2D;

public class ChartLine implements ChartElement
{
	public String text;
	public int tick;

	public ChartLine(String text, int tick)
	{
		this.text = text;
		this.tick = tick;
	}

	@Override
	public int getTick() {
		return tick;
	}

	@Override
	public void draw(Graphics2D g, ChartPanel chartPanel)
	{
		// Drawing logic for lines
	}
}
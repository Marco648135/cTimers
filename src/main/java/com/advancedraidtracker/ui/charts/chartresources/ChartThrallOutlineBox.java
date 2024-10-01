package com.advancedraidtracker.ui.charts.chartresources;

import com.advancedraidtracker.ui.charts.ChartPanel;
import java.awt.Graphics2D;

public class ChartThrallOutlineBox implements ChartElement {

	public int spawnTick;

	@Override
	public int getTick() {
		return spawnTick;
	}

	@Override
	public void draw(Graphics2D g, ChartPanel chartPanel)
	{

	}
}

package com.advancedraidtracker.ui.charts.chartresources;

import com.advancedraidtracker.ui.charts.ChartPanel;
import java.awt.Graphics2D;

public interface ChartElement
{
	int getTick();
	void draw(Graphics2D g, ChartPanel chartPanel);
}

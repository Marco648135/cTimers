package com.advancedraidtracker.ui.nylostalls;

import com.advancedraidtracker.AdvancedRaidTrackerConfig;
import com.advancedraidtracker.ui.BaseFrame;
import java.util.List;
import javax.swing.JPanel;

public class ViewNyloStalls extends BaseFrame
{
	public ViewNyloStalls(List<Integer> data, int raids, AdvancedRaidTrackerConfig config)
	{
		JPanel panel = new ViewNyloStallsPanel(data, raids, config);
		setResizable(false);
		setTitle("Nylo Stalls");
		add(panel);
		pack();
		open();
	}
}

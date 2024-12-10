package com.advancedraidtracker.ui.advancedstatistics;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;
import javax.swing.JPanel;

public abstract class DataTab extends JPanel
{
	protected AdvancedData dataSource;
	public DataTab(AdvancedData dataSource)
	{
		setBackground(config.primaryDark());
		setOpaque(true);
		this.dataSource = dataSource;
		dataSource.addRefreshListener(this::refresh);
	}

	public abstract void refresh();
}

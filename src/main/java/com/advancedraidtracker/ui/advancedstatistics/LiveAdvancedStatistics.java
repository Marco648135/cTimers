package com.advancedraidtracker.ui.advancedstatistics;

import com.advancedraidtracker.ui.BaseFrame;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedTabbedPane;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTabbedPane;

public class LiveAdvancedStatistics extends BaseFrame
{
	private JTabbedPane tabbedPane;

	private AdvancedNylocasPanel nylocasPanel;

	public LiveAdvancedStatistics(AdvancedData dataSource)
	{
		nylocasPanel = new AdvancedNylocasPanel(dataSource);

		tabbedPane = getThemedTabbedPane();

		tabbedPane.addTab("Nylocas", nylocasPanel);
		tabbedPane.setPreferredSize(new Dimension(800, 600));
		setSize(800, 600);
		add(tabbedPane);
		pack();
	}
}

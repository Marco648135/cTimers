package com.advancedraidtracker.ui.setups;

import static com.advancedraidtracker.ui.RaidTrackerSidePanel.config;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedButton;
import static com.advancedraidtracker.utility.UISwingUtility.getThemedLabel;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public class SetupsToolbar extends JPanel
{
	public SetupsToolbar()
	{
		setBackground(config.primaryDark());
		setOpaque(true);
		add(getThemedLabel("Test?"), BorderLayout.WEST);
		add(getThemedButton("Test"), BorderLayout.CENTER);
		add(getThemedLabel("Test2?"), BorderLayout.EAST);
	}
}

package com.advancedraidtracker.ui.dpsanalysis;

import java.awt.Dimension;
import javax.swing.JButton;

public class SquareButton extends JButton
{
	public SquareButton(String text)
	{
		super(text);
	}

	@Override
	public Dimension getPreferredSize()
	{
		Dimension size = super.getPreferredSize();
		int max = Math.max(size.width, size.height);
		return new Dimension(max, max);
	}

	@Override
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	@Override
	public Dimension getMaximumSize()
	{
		return getPreferredSize();
	}
}
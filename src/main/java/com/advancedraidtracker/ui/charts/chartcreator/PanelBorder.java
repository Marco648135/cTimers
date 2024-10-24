package com.advancedraidtracker.ui.charts.chartcreator;

// PanelBorder.java
import java.awt.*;
import javax.swing.border.AbstractBorder;

// PanelBorder.java
import java.awt.*;
import javax.swing.border.AbstractBorder;

public class PanelBorder extends AbstractBorder {
	private boolean isSelected;
	private Color borderColor;
	private Color highlightColor;
	private int borderWidth;

	public PanelBorder(Color borderColor, Color highlightColor, int borderWidth) {
		this.borderColor = borderColor;
		this.highlightColor = highlightColor;
		this.borderWidth = borderWidth; // Should be 3
	}

	public void setSelected(boolean selected) {
		this.isSelected = selected;
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(borderWidth, borderWidth, borderWidth, borderWidth);
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		int bw = borderWidth;
		Graphics2D g2d = (Graphics2D) g.create();

		// Draw the 3-pixel border
		g2d.setColor(borderColor);
		for (int i = 0; i < bw; i++) {
			g2d.drawRect(x + i, y + i, width - i - i - 1, height - i - i -1);
		}

		// Draw the highlight if selected
		if (isSelected) {
			int mid = bw / 2;
			g2d.setColor(highlightColor);
			g2d.drawRect(x + mid, y + mid, width - mid * 2 - 1, height - mid * 2 - 1);
		}

		g2d.dispose();
	}
}



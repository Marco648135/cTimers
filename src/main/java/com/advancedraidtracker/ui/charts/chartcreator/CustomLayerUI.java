package com.advancedraidtracker.ui.charts.chartcreator;

import java.beans.PropertyChangeEvent;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.plaf.LayerUI;


public class CustomLayerUI extends LayerUI<JComponent> {
	private boolean isOverlayVisible = false;
	private Shape[] overlayShapes;
	private int highlightedIndex = -1;

	public void setOverlayVisible(boolean visible) {
		boolean old = isOverlayVisible;
		isOverlayVisible = visible;
		firePropertyChange("overlayVisible", old, visible);
	}

	public void setOverlayShapes(Shape[] shapes) {
		Shape[] oldShapes = overlayShapes;
		overlayShapes = shapes;
		firePropertyChange("overlayShapes", oldShapes, shapes);
	}

	public void updateOverlayMousePosition(Point p) {
		if (overlayShapes == null) {
			return;
		}
		int newIndex = -1;
		for (int i = 0; i < overlayShapes.length; i++) {
			if (overlayShapes[i].contains(p)) {
				newIndex = i;
				break;
			}
		}
		if (newIndex != highlightedIndex) {
			int oldIndex = highlightedIndex;
			highlightedIndex = newIndex;
			firePropertyChange("highlightedIndex", oldIndex, newIndex);
		}
	}

	public int getHighlightedIndex() {
		return highlightedIndex;
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		super.paint(g, c);
		if (isOverlayVisible && overlayShapes != null) {
			Graphics2D g2d = (Graphics2D) g.create();
			Composite oldComposite = g2d.getComposite();
			Composite newComposite = AlphaComposite.SrcOver.derive(0.5f);
			g2d.setComposite(newComposite);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			Color hoveredColor = new Color(45, 140, 235, 100);
			Color highlightedColor = new Color(45, 140, 235, 255);
			Color outlineColor = new Color(160, 160, 160, 200);
			Color inactiveColor = new Color(50, 50, 50, 230);

			for (int i = 0; i < overlayShapes.length; i++) {
				Shape shape = overlayShapes[i];
				if (shape != null) {
					if (highlightedIndex == i) {
						g2d.setColor(hoveredColor);
						g2d.fill(shape);
						g2d.setStroke(new BasicStroke(1));
						g2d.setComposite(oldComposite);
						g2d.setColor(highlightedColor);
						g2d.draw(shape);
						g2d.setComposite(newComposite);
					} else {
						if (overlayShapes.length > 4) { // Only draw inactive shapes if it's trapezoids, not rectangles
							g2d.setColor(inactiveColor);
							g2d.fill(shape);
							g2d.setColor(outlineColor);
							g2d.draw(shape);
						}
					}
				}
			}
			g2d.dispose();
		}
	}

	@Override
	protected void processMouseEvent(MouseEvent e, JLayer<? extends JComponent> l) {
		if (isOverlayVisible) {
			updateOverlayMousePosition(e.getPoint());
			e.consume(); // Consume the event to prevent underlying components from receiving it during overlay
		} else {
			super.processMouseEvent(e, l);
		}
	}

	@Override
	protected void processMouseMotionEvent(MouseEvent e, JLayer<? extends JComponent> l) {
		if (isOverlayVisible) {
			updateOverlayMousePosition(e.getPoint());
			e.consume();
		} else {
			super.processMouseMotionEvent(e, l);
		}
	}

	@Override
	public void applyPropertyChange(PropertyChangeEvent pce, JLayer<? extends JComponent> l) {
		if ("overlayVisible".equals(pce.getPropertyName()) ||
			"overlayShapes".equals(pce.getPropertyName()) ||
			"highlightedIndex".equals(pce.getPropertyName())) {
			l.repaint();
		}
	}
}


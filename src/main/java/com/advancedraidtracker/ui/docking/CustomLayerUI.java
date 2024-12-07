package com.advancedraidtracker.ui.docking;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.SwingUtilities;
import javax.swing.plaf.LayerUI;


public class CustomLayerUI extends LayerUI<JComponent>
{
	private CustomLayerListener listener;

	private boolean resizing = false;
	private int dividerIndex = -1;
	private Point lastMousePosition;
	private boolean verticalResize = false;
	private MultiSplitPane resizingPane = null;

	private MultiSplitPane rootMultiSplitPane;

	public void setRootMultiSplitPane(MultiSplitPane rootMultiSplitPane)
	{
		this.rootMultiSplitPane = rootMultiSplitPane;
	}

	public CustomLayerUI()
	{
		super();
	}

	public void setCustomLayerListener(CustomLayerListener listener)
	{
		this.listener = listener;
	}

	private boolean isOverlayVisible = false;
	private Shape[] overlayShapes;
	private int highlightedIndex = -1;

	public void setOverlayVisible(boolean visible)
	{
		boolean old = isOverlayVisible;
		isOverlayVisible = visible;
		firePropertyChange("overlayVisible", old, visible);
	}

	public void setOverlayShapes(Shape[] shapes)
	{
		Shape[] oldShapes = overlayShapes;
		overlayShapes = shapes;
		firePropertyChange("overlayShapes", oldShapes, shapes);
	}

	public void updateOverlayMousePosition(Point p)
	{
		if (overlayShapes == null)
		{
			return;
		}
		int newIndex = -1;
		for (int i = 0; i < overlayShapes.length; i++)
		{
			if (overlayShapes[i].contains(p))
			{
				newIndex = i;
				break;
			}
		}
		if (newIndex != highlightedIndex)
		{
			int oldIndex = highlightedIndex;
			highlightedIndex = newIndex;
			firePropertyChange("highlightedIndex", oldIndex, newIndex);
		}
	}

	public int getHighlightedIndex()
	{
		return highlightedIndex;
	}

	@Override
	public void paint(Graphics g, JComponent c)
	{
		super.paint(g, c);
		if (isOverlayVisible && overlayShapes != null)
		{
			Graphics2D g2d = (Graphics2D) g.create();
			Composite oldComposite = g2d.getComposite();
			Composite newComposite = AlphaComposite.SrcOver.derive(0.5f);
			g2d.setComposite(newComposite);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			Color hoveredColor = new Color(45, 140, 235, 100);
			Color highlightedColor = new Color(45, 140, 235, 255);
			Color outlineColor = new Color(160, 160, 160, 200);
			Color inactiveColor = new Color(50, 50, 50, 230);

			for (int i = 0; i < overlayShapes.length; i++)
			{
				Shape shape = overlayShapes[i];
				if (shape != null)
				{
					if (highlightedIndex == i)
					{
						g2d.setColor(hoveredColor);
						g2d.fill(shape);
						g2d.setStroke(new BasicStroke(1));
						g2d.setComposite(oldComposite);
						g2d.setColor(highlightedColor);
						g2d.draw(shape);
						g2d.setComposite(newComposite);
					}
					else
					{
						if (overlayShapes.length > 4)
						{
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
	protected void processMouseEvent(MouseEvent e, JLayer<? extends JComponent> l)
	{
		if (isOverlayVisible)
		{
			updateOverlayMousePosition(e.getPoint());
			e.consume();
		}
		else
		{
			if (e.getID() == MouseEvent.MOUSE_PRESSED)
			{
				if (dividerIndex >= 0 && e.getButton() == MouseEvent.BUTTON1)
				{
					resizing = true;
					lastMousePosition = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), resizingPane);
					e.consume();
				}
				else
				{
					Point p = e.getPoint();
					Point pointInView = SwingUtilities.convertPoint(e.getComponent(), p, l);
					Component comp = SwingUtilities.getDeepestComponentAt(l, pointInView.x, pointInView.y);
					if (comp != null)
					{
						CustomPanel customPanel = findCustomPanel(comp);
						if (customPanel != null && listener != null)
						{
							listener.onCustomPanelClicked(customPanel);
						}
					}
				}
			}
			else if (e.getID() == MouseEvent.MOUSE_RELEASED)
			{
				if (resizing)
				{
					resizing = false;
					dividerIndex = -1;
					e.getComponent().setCursor(Cursor.getDefaultCursor());
					resizingPane = null;
					e.consume();
				}
			}
		}
	}


	private void handleMouseDragged(MouseEvent e)
	{
		if (resizing && resizingPane != null)
		{
			Point currentPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), resizingPane);
			int delta = verticalResize ? currentPoint.y - lastMousePosition.y : currentPoint.x - lastMousePosition.x;

			resizingPane.resizeComponentsAtDivider(dividerIndex, delta);

			lastMousePosition = currentPoint;
			e.consume();
		}
	}


	private CustomPanel findCustomPanel(Component comp)
	{
		while (comp != null)
		{
			if (comp instanceof CustomPanel)
			{
				return (CustomPanel) comp;
			}
			comp = comp.getParent();
		}
		return null;
	}

	@Override
	protected void processMouseMotionEvent(MouseEvent e, JLayer<? extends JComponent> l)
	{
		if (isOverlayVisible)
		{
			updateOverlayMousePosition(e.getPoint());
			e.consume();
		}
		else if (resizing)
		{
			handleMouseDragged(e);
		}
		else
		{
			Point p = e.getPoint();
			handleMouseMovedOverMultiSplitPanes(p, e);
		}
	}


	private void handleMouseMovedOverMultiSplitPanes(Point p, MouseEvent e)
	{
		Component comp = SwingUtilities.getDeepestComponentAt(e.getComponent(), p.x, p.y);
		Point compPoint = p;

		Set<Component> visitedComponents = new HashSet<>();  // To prevent infinite loops in case of cyclic references

		while (comp != null && !visitedComponents.contains(comp))
		{
			visitedComponents.add(comp);

			if (comp instanceof MultiSplitPane)
			{
				MultiSplitPane pane = (MultiSplitPane) comp;
				Point panePoint = SwingUtilities.convertPoint(e.getComponent(), p, pane);

				List<Integer> dividerPositions = pane.getDividerPositions();

				int mousePos = pane.isVerticalOrientation() ? panePoint.y : panePoint.x;
				int threshold = 5;  // Adjust as needed

				for (int i = 0; i < dividerPositions.size(); i++)
				{
					int position = dividerPositions.get(i);

					if (Math.abs(mousePos - position) <= threshold)
					{
						dividerIndex = i;
						verticalResize = pane.isVerticalOrientation();

						e.getComponent().setCursor(Cursor.getPredefinedCursor(
							verticalResize ? Cursor.N_RESIZE_CURSOR : Cursor.E_RESIZE_CURSOR));

						resizingPane = pane;
						return;
					}
				}
			}

			Component parent = comp.getParent();
			if (parent != null)
			{
				compPoint = SwingUtilities.convertPoint(comp, compPoint, parent);
			}
			comp = parent;
		}

		e.getComponent().setCursor(Cursor.getDefaultCursor());
		dividerIndex = -1;
		resizingPane = null;
	}


	@Override
	public void applyPropertyChange(PropertyChangeEvent pce, JLayer<? extends JComponent> l)
	{
		if ("overlayVisible".equals(pce.getPropertyName()) ||
			"overlayShapes".equals(pce.getPropertyName()) ||
			"highlightedIndex".equals(pce.getPropertyName()))
		{
			l.repaint();
		}
	}
}


package com.advancedraidtracker.ui.charts.chartcreator;
// BorderPanel.java
import java.awt.*;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BorderPanel extends JPanel {
	private boolean isSelected;
	private PanelBorder panelBorder;
	private boolean isResizing = false;
	private int resizeEdge = Cursor.DEFAULT_CURSOR;
	private Point lastPoint;

	private static final int EDGE_MARGIN = 5; // Sensitivity margin

	public BorderPanel(Component content, Color borderColor, Color highlightColor, int borderWidth) {
		super(new BorderLayout());
		this.panelBorder = new PanelBorder(borderColor, highlightColor, borderWidth);
		setBorder(panelBorder);
		add(content, BorderLayout.CENTER);

		MouseAdapter mouseAdapter = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				handleMouseMoved(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				handleMousePressed(e);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				handleMouseDragged(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				handleMouseReleased(e);
			}
		};

		addMouseListener(mouseAdapter);
		addMouseMotionListener(mouseAdapter);
		addMouseAdapterRecursively(content, mouseAdapter);
	}

	private void resizeHorizontally(int dx, boolean isEastEdge) {
		MultiSplitPane parentPane = (MultiSplitPane) getParent();
		parentPane.resizeComponentHorizontally(this, dx, isEastEdge);
	}

	private void resizeVertically(int dy, boolean isSouthEdge) {
		MultiSplitPane parentPane = (MultiSplitPane) getParent();
		parentPane.resizeComponentVertically(this, dy, isSouthEdge);
	}

	private int getCursorType(Point p) {
		int width = getWidth();
		int height = getHeight();
		int x = p.x;
		int y = p.y;
		int margin = EDGE_MARGIN; // Should be a small value like 5

		boolean left = x < margin;
		boolean right = x > width - margin;
		boolean top = y < margin;
		boolean bottom = y > height - margin;

		if (left && top) {
			return Cursor.NW_RESIZE_CURSOR;
		} else if (right && top) {
			return Cursor.NE_RESIZE_CURSOR;
		} else if (left && bottom) {
			return Cursor.SW_RESIZE_CURSOR;
		} else if (right && bottom) {
			return Cursor.SE_RESIZE_CURSOR;
		} else if (left) {
			return Cursor.W_RESIZE_CURSOR;
		} else if (right) {
			return Cursor.E_RESIZE_CURSOR;
		} else if (top) {
			return Cursor.N_RESIZE_CURSOR;
		} else if (bottom) {
			return Cursor.S_RESIZE_CURSOR;
		} else {
			return Cursor.DEFAULT_CURSOR;
		}
	}



	private void deselectOthers() {
		Container parent = getParent();
		if (parent instanceof MultiSplitPane) {
			((MultiSplitPane) parent).deselectOtherPanels(this);
		}
	}

	public void setSelected(boolean selected) {
		if (this.isSelected != selected) {
			this.isSelected = selected;
			panelBorder.setSelected(selected);
			log.info("setting selected: " + selected);
			repaint();
		}
	}

	private void handleMouseMoved(MouseEvent e) {
		Point p = SwingUtilities.convertMouseEvent(e.getComponent(), e, this).getPoint();
		int cursorType = getCursorType(p);
		setCursor(Cursor.getPredefinedCursor(cursorType));
	}

	private void handleMousePressed(MouseEvent e) {
		log.info("mouse pressed! ");
		System.out.println("mouse pressed! ");
		Point p = SwingUtilities.convertMouseEvent(e.getComponent(), e, this).getPoint();
		int cursorType = getCursorType(p);
		if (cursorType == Cursor.DEFAULT_CURSOR)
		{
			// Handle selection
			setSelected(true);
			deselectOthers();
		} else
		{
			// Handle resizing
			isResizing = true;
			resizeEdge = cursorType;
			lastPoint = SwingUtilities.convertPoint(this, p, getParent());
		}
	}

	private void handleMouseDragged(MouseEvent e) {
		if (isResizing) {
			Point pt = SwingUtilities.convertMouseEvent(e.getComponent(), e, getParent()).getPoint();
			int dx = pt.x - lastPoint.x;
			int dy = pt.y - lastPoint.y;
			// Handle resizing
			if (resizeEdge == Cursor.E_RESIZE_CURSOR || resizeEdge == Cursor.W_RESIZE_CURSOR) {
				resizeHorizontally(dx, resizeEdge == Cursor.E_RESIZE_CURSOR);
			} else if (resizeEdge == Cursor.N_RESIZE_CURSOR || resizeEdge == Cursor.S_RESIZE_CURSOR) {
				resizeVertically(dy, resizeEdge == Cursor.S_RESIZE_CURSOR);
			}
			lastPoint = pt;
		}
	}

	private void handleMouseReleased(MouseEvent e) {
		isResizing = false;
		resizeEdge = Cursor.DEFAULT_CURSOR;
	}

	private void addMouseAdapterRecursively(Component component, MouseAdapter adapter) {
		component.addMouseListener(adapter);
		component.addMouseMotionListener(adapter);
		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				addMouseAdapterRecursively(child, adapter);
			}
		}
	}
}


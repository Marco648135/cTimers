package com.advancedraidtracker.ui.dpsanalysis;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

// Define a custom JTableHeader class that supports multi-row headers
class MultiRowTableHeader extends JTableHeader
{
	private int headerRows;

	public MultiRowTableHeader(TableColumnModel model, int headerRows) {
		super(model);
		this.headerRows = headerRows;
		setUI(new MultiRowHeaderUI());
	}

	public int getHeaderRows() {
		return headerRows;
	}
}

// Custom UI for the multi-row table header
class MultiRowHeaderUI extends BasicTableHeaderUI
{
	@Override
	public void paint(Graphics g, JComponent c) {
		// Custom painting code to render multiple header rows
		JTableHeader header = (JTableHeader) c;
		TableColumnModel columnModel = header.getColumnModel();
		int columnCount = columnModel.getColumnCount();
		Rectangle clipBounds = g.getClipBounds();

		for (int i = 0; i < columnCount; i++) {
			Rectangle cellRect = header.getHeaderRect(i);
			if (cellRect.intersects(clipBounds)) {
				paintCell(g, cellRect, i);
			}
		}
	}

	private void paintCell(Graphics g, Rectangle cellRect, int columnIndex) {
		// Draw the background
		g.setColor(UIManager.getColor("TableHeader.background"));
		g.fillRect(cellRect.x, cellRect.y, cellRect.width, cellRect.height);

		// Draw the header text
		g.setColor(UIManager.getColor("TableHeader.foreground"));
		TableColumn column = header.getColumnModel().getColumn(columnIndex);

		Object headerValue = column.getHeaderValue();
		String[] headerLines = headerValue.toString().split("\n");

		FontMetrics fm = g.getFontMetrics();
		int rowHeight = fm.getHeight();

		for (int i = 0; i < headerLines.length; i++) {
			int y = cellRect.y + ((i + 1) * rowHeight) - fm.getDescent();
			g.drawString(headerLines[i], cellRect.x + 5, y);
		}

		// Draw the cell border
		g.setColor(UIManager.getColor("TableHeader.borderColor"));
		g.drawRect(cellRect.x, cellRect.y, cellRect.width, cellRect.height);
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		// Calculate the preferred size based on the number of header rows
		Dimension size = super.getPreferredSize(c);
		int headerRows = ((MultiRowTableHeader) header).getHeaderRows();
		size.height = headerRows * c.getFontMetrics(c.getFont()).getHeight();
		return size;
	}
}

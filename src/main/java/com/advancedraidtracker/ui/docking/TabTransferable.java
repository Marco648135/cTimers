package com.advancedraidtracker.ui.docking;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import javax.swing.JTabbedPane;

class TabTransferable implements Transferable
{

	public static final DataFlavor TAB_INDEX_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.lang.Integer", "Tab Index");
	public static final DataFlavor TABBED_PANE_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=javax.swing.JTabbedPane", "Tabbed Pane");


	private final DataFlavor[] flavors = {TAB_INDEX_FLAVOR, TABBED_PANE_FLAVOR};

	private final JTabbedPane tabbedPane;
	private final int tabIndex;

	public TabTransferable(JTabbedPane tabbedPane, int tabIndex)
	{
		this.tabbedPane = tabbedPane;
		this.tabIndex = tabIndex;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors()
	{
		return flavors;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
		for (DataFlavor f : flavors)
		{
			if (f.equals(flavor))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
	{
		if (flavor.equals(TAB_INDEX_FLAVOR))
		{
			return tabIndex;
		}
		else if (flavor.equals(TABBED_PANE_FLAVOR))
		{
			return tabbedPane;
		}
		else
		{
			throw new UnsupportedFlavorException(flavor);
		}
	}
}

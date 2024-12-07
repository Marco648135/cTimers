package com.advancedraidtracker.ui.docking;

import java.util.List;

class LayoutNode
{
	String type;
	boolean vertical;
	int width;
	int height;
	String title;
	List<LayoutNode> children;
	List<LayoutNode> registryPanels;
}

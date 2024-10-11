package com.advancedraidtracker.ui.charts.chartcreator;

import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MultiSplitPane extends JPanel
{
	private static final int DIVIDER_SIZE = 2;
	private final boolean verticalOrientation;
	private final List<Component> components = new ArrayList<>();

	public MultiSplitPane(boolean verticalOrientation)
	{
		this.verticalOrientation = verticalOrientation;
		setLayout(null); // We'll manage layout manually
	}

	public void addComponent(Component comp)
	{
		if (!components.isEmpty())
		{
			// Add a divider before adding the new component
			Divider divider = new Divider(verticalOrientation, this);
			components.add(divider);
			add(divider);
		}
		components.add(comp);
		add(comp);
		revalidate();
		repaint();
	}

	public void removeComponent(Component comp)
	{
		int index = components.indexOf(comp);
		if (index == -1)
		{
			return; // Component not found
		}

		// Find adjacent dividers before removing the component
		Component previousComponent = index > 0 ? components.get(index - 1) : null;
		Component nextComponent = index + 1 < components.size() ? components.get(index + 1) : null;

		// Remove the component
		remove(comp);
		components.remove(index);

		// Remove adjacent dividers if necessary
		if (previousComponent instanceof Divider)
		{
			remove(previousComponent);
			components.remove(index - 1);
			index--; // Adjust index because we've removed an element before it
		}
		else if (nextComponent instanceof Divider)
		{
			remove(nextComponent);
			components.remove(index); // Index is the same because we already removed the component at index
		}

		// Remove remaining dividers if only one component is left
		if (components.size() == 1)
		{
			for (int i = components.size() - 1; i >= 0; i--)
			{
				if (components.get(i) instanceof Divider)
				{
					remove(components.get(i));
					components.remove(i);
				}
			}
		}

		revalidate();
		repaint();
	}


	public void resizeComponents(Divider divider, int delta)
	{
		int dividerIndex = components.indexOf(divider);
		if (dividerIndex == -1)
		{
			return;
		}

		Component comp1 = components.get(dividerIndex - 1);
		Component comp2 = components.get(dividerIndex + 1);

		if (verticalOrientation)
		{
			int newHeight1 = comp1.getHeight() + delta;
			int newHeight2 = comp2.getHeight() - delta;

			if (newHeight1 < 50 || newHeight2 < 50)
			{
				return; // Minimum size
			}

			comp1.setPreferredSize(new Dimension(comp1.getWidth(), newHeight1));
			comp2.setPreferredSize(new Dimension(comp2.getWidth(), newHeight2));
		}
		else
		{
			int newWidth1 = comp1.getWidth() + delta;
			int newWidth2 = comp2.getWidth() - delta;

			if (newWidth1 < 50 || newWidth2 < 50)
			{
				return; // Minimum size
			}

			comp1.setPreferredSize(new Dimension(newWidth1, comp1.getHeight()));
			comp2.setPreferredSize(new Dimension(newWidth2, comp2.getHeight()));
		}
		revalidate();
		repaint();
	}

	public void splitComponent(Component existingComponent, Component newComponent, boolean verticalSplit, boolean insertBefore)
	{
		int index = components.indexOf(existingComponent);
		if (index == -1)
		{
			return; // Component not found
		}

		// Create a new MultiSplitPane with the desired orientation
		MultiSplitPane newSplitPane = new MultiSplitPane(verticalSplit);

		// Remove the existing component and replace it with the new split pane
		remove(existingComponent);
		components.set(index, newSplitPane);
		add(newSplitPane);

		// Add the components to the new split pane
		if (insertBefore)
		{
			newSplitPane.addComponent(newComponent);
			newSplitPane.addComponent(existingComponent);
		}
		else
		{
			newSplitPane.addComponent(existingComponent);
			newSplitPane.addComponent(newComponent);
		}

		revalidate();
		repaint();
	}

	public void mergeIntoTabbedPane(Component comp1, Component comp2)
	{
		int index = components.indexOf(comp1);
		if (index == -1)
		{
			return; // Component not found
		}

		// Create a new JTabbedPane
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab(getTitle(comp1), comp1);
		tabbedPane.addTab(getTitle(comp2), comp2);

		// Remove the existing component and replace it with the tabbed pane
		remove(comp1);
		components.set(index, tabbedPane);
		add(tabbedPane);

		revalidate();
		repaint();
	}

	private String getTitle(Component comp)
	{
		if (comp instanceof CustomPanel)
		{
			return ((CustomPanel) comp).title;
		}
		return comp.getName() != null ? comp.getName() : "Tab";
	}


	@Override
	public void doLayout()
	{
		layoutComponents();
	}

	private void layoutComponents()
	{
		if (components.size() == 1 && !(components.get(0) instanceof Divider))
		{
			// Only one component, fill the entire space
			Component comp = components.get(0);
			comp.setBounds(0, 0, getWidth(), getHeight());
			return;
		}

		// Existing layout logic for multiple components
		int totalSize = verticalOrientation ? getHeight() : getWidth();
		int position = 0;

		int totalDividerSize = 0;
		int totalPreferredSize = 0;
		int numFlexibleComponents = 0;

		// Map to hold calculated sizes without modifying preferred sizes
		Map<Component, Integer> componentSizes = new HashMap<>();

		// Compute total divider size and total preferred size
		for (Component comp : components)
		{
			if (comp instanceof Divider)
			{
				totalDividerSize += DIVIDER_SIZE;
			}
			else
			{
				Dimension prefSize = comp.getPreferredSize();
				if (prefSize == null)
				{
					prefSize = new Dimension(0, 0);
				}
				int size = verticalOrientation ? prefSize.height : prefSize.width;
				if (size > 0)
				{
					totalPreferredSize += size;
					componentSizes.put(comp, size);
				}
				else
				{
					numFlexibleComponents++;
				}
			}
		}

		int availableSize = totalSize - totalDividerSize;
		int flexibleSpace = availableSize - totalPreferredSize;

		// Adjust sizes if total preferred sizes exceed available space
		if (totalPreferredSize > availableSize)
		{
			double scale = (double) availableSize / totalPreferredSize;

			// Scale down the sizes without modifying preferred sizes
			totalPreferredSize = 0;
			for (Map.Entry<Component, Integer> entry : componentSizes.entrySet())
			{
				int size = (int) (entry.getValue() * scale);
				componentSizes.put(entry.getKey(), size);
				totalPreferredSize += size;
			}

			// Recalculate flexibleSpace after scaling
			flexibleSpace = availableSize - totalPreferredSize;
		}

		// Now, allocate flexible space to flexible components
		int flexibleComponentSize = numFlexibleComponents > 0 ? flexibleSpace / numFlexibleComponents : 0;

		for (Component comp : components)
		{
			if (comp instanceof Divider)
			{
				if (verticalOrientation)
				{
					comp.setBounds(0, position, getWidth(), DIVIDER_SIZE);
					position += DIVIDER_SIZE;
				}
				else
				{
					comp.setBounds(position, 0, DIVIDER_SIZE, getHeight());
					position += DIVIDER_SIZE;
				}
			}
			else
			{
				int compSize;
				if (componentSizes.containsKey(comp))
				{
					compSize = componentSizes.get(comp);
				}
				else
				{
					compSize = flexibleComponentSize;
				}

				if (verticalOrientation)
				{
					comp.setBounds(0, position, getWidth(), compSize);
					position += compSize;
				}
				else
				{
					comp.setBounds(position, 0, compSize, getHeight());
					position += compSize;
				}
			}
		}
	}


	public void addComponent(Component comp, boolean insertDividerBefore, boolean verticalSplit)
	{
		if (insertDividerBefore)
		{
			Divider divider = new Divider(verticalSplit, this);
			components.add(divider);
			add(divider);
		}
		components.add(comp);
		add(comp);
		revalidate();
		repaint();
	}


	public boolean isVerticalOrientation()
	{
		return verticalOrientation;
	}
}

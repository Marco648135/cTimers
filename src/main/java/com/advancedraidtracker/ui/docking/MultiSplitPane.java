package com.advancedraidtracker.ui.docking;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MultiSplitPane extends JPanel
{
	private final boolean verticalOrientation;
	private final List<Component> components = new ArrayList<>();
	private final boolean isFlexible;
	private boolean hasDoneLayout = false;

	public List<Integer> getDividerPositions()
	{
		List<Integer> dividerPositions = new ArrayList<>();
		int position = 0;
		for (Component comp : components)
		{
			int compSize = verticalOrientation ? comp.getHeight() : comp.getWidth();
			position += compSize;
			if (comp != components.get(components.size() - 1))
			{
				dividerPositions.add(position);
			}
		}
		return dividerPositions;
	}

	public boolean isVerticalOrientation()
	{
		return verticalOrientation;
	}

	public void resizeComponentsAtDivider(int dividerIndex, int delta)
	{
		if (dividerIndex < 0 || dividerIndex >= components.size() - 1)
		{
			return;
		}

		Component comp = components.get(dividerIndex);
		Component nextComp = components.get(dividerIndex + 1);

		int currentCompSize = verticalOrientation ? comp.getHeight() : comp.getWidth();
		int currentNextCompSize = verticalOrientation ? nextComp.getHeight() : nextComp.getWidth();

		int minSize = 50;

		int newCompSize = currentCompSize + delta;
		int newNextCompSize = currentNextCompSize - delta;


		if (newCompSize < minSize || newNextCompSize < minSize)
		{
			return;
		}

		if (verticalOrientation)
		{
			comp.setPreferredSize(new Dimension(getWidth(), newCompSize));
			nextComp.setPreferredSize(new Dimension(getWidth(), newNextCompSize));
		}
		else
		{
			comp.setPreferredSize(new Dimension(newCompSize, getHeight()));
			nextComp.setPreferredSize(new Dimension(newNextCompSize, getHeight()));
		}

		Map<Component, Integer> otherComponentSizes = new LinkedHashMap<>();
		for (int i = 0; i < components.size(); i++)
		{
			if (i != dividerIndex && i != dividerIndex + 1)
			{
				Component c = components.get(i);
				otherComponentSizes.put(c, verticalOrientation ? c.getHeight() : c.getWidth());
			}
		}

		revalidate();
		repaint();

		for (Map.Entry<Component, Integer> entry : otherComponentSizes.entrySet())
		{
			Component c = entry.getKey();
			int size = entry.getValue();
			if (verticalOrientation)
			{
				c.setPreferredSize(new Dimension(getWidth(), size));
			}
			else
			{
				c.setPreferredSize(new Dimension(size, getHeight()));
			}
		}
	}


	public MultiSplitPane(boolean verticalOrientation)
	{
		this(verticalOrientation, false);
	}

	public MultiSplitPane(boolean verticalOrientation, boolean isFlexible)
	{
		this.verticalOrientation = verticalOrientation;
		this.isFlexible = isFlexible;
		setLayout(null);
	}

	public void addComponent(Component comp)
	{
		components.add(comp);
		add(comp);
		revalidate();
		repaint();
	}

	public void addComponentAt(int index, Component comp)
	{
		add(comp);
		components.add(index, comp);
		revalidate();
		repaint();
	}

	public void removeComponent(Component comp)
	{
		int index = components.indexOf(comp);
		if (index == -1)
		{
			return;
		}

		remove(comp);
		components.remove(index);


		if (components.isEmpty())
		{
			Container parent = getParent();
			if (parent instanceof MultiSplitPane)
			{
				MultiSplitPane multiSplitPane = (MultiSplitPane) parent;
				multiSplitPane.removeComponent(this);
			}
			else
			{
				Window window = SwingUtilities.getWindowAncestor(this);
				if (window != null)
				{
					window.dispose();
				}
			}
		}

		revalidate();
		repaint();
	}

	public void splitComponent(Component existingComponent, Component newComponent, boolean verticalSplit, boolean insertBefore)
	{
		int index = components.indexOf(existingComponent);
		if (index == -1)
		{
			return;
		}

		MultiSplitPane newSplitPane = new MultiSplitPane(verticalSplit);

		Dimension existingPreferredSize = existingComponent.getPreferredSize();
		newSplitPane.setPreferredSize(Objects.requireNonNullElseGet(existingPreferredSize, () -> new Dimension(existingComponent.getWidth(), existingComponent.getHeight())));
		addComponentAt(index, newSplitPane);
		removeComponent(existingComponent);

		if (existingPreferredSize != null)
		{
			if (verticalSplit)
			{
				int totalHeight = existingPreferredSize.height;
				int halfHeight = totalHeight / 2;
				existingComponent.setPreferredSize(new Dimension(existingPreferredSize.width, halfHeight));
				newComponent.setPreferredSize(new Dimension(existingPreferredSize.width, halfHeight));
			}
			else
			{
				int totalWidth = existingPreferredSize.width;
				int halfWidth = totalWidth / 2;
				existingComponent.setPreferredSize(new Dimension(halfWidth, existingPreferredSize.height));
				newComponent.setPreferredSize(new Dimension(halfWidth, existingPreferredSize.height));
			}
		}
		else
		{
			existingComponent.setPreferredSize(new Dimension(0, 0));
			newComponent.setPreferredSize(new Dimension(0, 0));
		}

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

	@Override
	public void doLayout()
	{
		layoutComponents();
	}

	//This has gotten beyond fucked I will come back to fix this I swear
	private void layoutComponents()
	{
		if (components.isEmpty())
		{
			return;
		}
		int totalSize = verticalOrientation ? getHeight() : getWidth();
		if (!hasDoneLayout)
		{
			for (Component component : getComponents())
			{
				Dimension preferredSize = component.getPreferredSize();
				if (preferredSize == null || (preferredSize.height == 0 && preferredSize.width == 0))
				{
					component.setPreferredSize(new Dimension(50, 50));
				}
				else
				{
					if (preferredSize.width == 0)
					{
						component.setPreferredSize(new Dimension(50, preferredSize.height));
					}
					else if (preferredSize.height == 0)
					{
						component.setPreferredSize(new Dimension(preferredSize.width, 50));
					}
				}
			}
			int totalPreferredSize = 0;

			for (Component component : getComponents())
			{
				Dimension preferredSize = component.getPreferredSize();
				totalPreferredSize += verticalOrientation ? preferredSize.height : preferredSize.width;
			}

			for (Component component : getComponents())
			{
				Dimension preferredSize = component.getPreferredSize();
				int preferredDimension = verticalOrientation ? preferredSize.height : preferredSize.width;

				int newSize = (int) ((preferredDimension / (double) totalPreferredSize) * totalSize);

				if (verticalOrientation)
				{
					component.setSize(getWidth(), newSize);
				}
				else
				{
					component.setSize(newSize, getHeight());
				}
			}
			hasDoneLayout = true;
		}
		int position = 0;
		int minSize = 50;

		List<Component> componentsWithPreferredSize = new ArrayList<>();
		List<Component> componentsWithoutPreferredSize = new ArrayList<>();

		int totalPreferredSize = 0;

		for (Component comp : components)
		{
			Dimension preferredSize = comp.getPreferredSize();
			int size = verticalOrientation ? preferredSize.height : preferredSize.width;

			if (size > 0)
			{
				componentsWithPreferredSize.add(comp);
				totalPreferredSize += size;
			}
			else
			{
				componentsWithoutPreferredSize.add(comp);
			}
		}

		Map<Component, Integer> componentSizes = new LinkedHashMap<>();

		int numComponents = components.size();

		if (componentsWithPreferredSize.isEmpty())
		{
			int sizePerComponent = totalSize / numComponents;
			int remainder = totalSize % numComponents;

			for (int i = 0; i < numComponents; i++)
			{
				Component comp = components.get(i);
				int assignedSize = sizePerComponent + (i == numComponents - 1 ? remainder : 0);
				componentSizes.put(comp, assignedSize);
			}
		}
		else if (componentsWithPreferredSize.size() == numComponents)
		{
			if (totalPreferredSize == totalSize)
			{
				for (Component comp : componentsWithPreferredSize)
				{
					Dimension preferredSize = comp.getPreferredSize();
					int size = verticalOrientation ? preferredSize.height : preferredSize.width;
					componentSizes.put(comp, size);
				}
			}
			else
			{
				double scalingFactor = (double) totalSize / totalPreferredSize;

				int totalAssignedSize = 0;
				for (int i = 0; i < componentsWithPreferredSize.size(); i++)
				{
					Component comp = componentsWithPreferredSize.get(i);
					Dimension preferredSize = comp.getPreferredSize();
					int size = verticalOrientation ? preferredSize.height : preferredSize.width;
					int adjustedSize;

					if (i == componentsWithPreferredSize.size() - 1)
					{
						adjustedSize = totalSize - totalAssignedSize;
					}
					else
					{
						adjustedSize = (int) Math.round(size * scalingFactor);
						totalAssignedSize += adjustedSize;
					}
					componentSizes.put(comp, adjustedSize);
				}
			}
		}
		else
		{
			int numNonPreferred = componentsWithoutPreferredSize.size();
			int totalMinNonPreferredSize = numNonPreferred * minSize;
			int totalMinimumRequiredSize = totalPreferredSize + totalMinNonPreferredSize;

			if (totalMinimumRequiredSize > totalSize)
			{
				int availableSizeForPreferred = totalSize - totalMinNonPreferredSize;
				double scalingFactor = (double) availableSizeForPreferred / totalPreferredSize;

				for (Component comp : componentsWithPreferredSize)
				{
					Dimension preferredSize = comp.getPreferredSize();
					int size = verticalOrientation ? preferredSize.height : preferredSize.width;
					int adjustedSize = (int) Math.max(minSize, Math.round(size * scalingFactor));
					componentSizes.put(comp, adjustedSize);
				}

				for (Component comp : componentsWithoutPreferredSize)
				{
					componentSizes.put(comp, minSize);
				}
			}
			else
			{
				for (Component comp : componentsWithPreferredSize)
				{
					Dimension preferredSize = comp.getPreferredSize();
					int size = verticalOrientation ? preferredSize.height : preferredSize.width;
					componentSizes.put(comp, size);
				}

				int remainingSize = totalSize - totalPreferredSize;

				int sizeAssigned = 0;
				for (int i = 0; i < componentsWithoutPreferredSize.size(); i++)
				{
					Component comp = componentsWithoutPreferredSize.get(i);
					int size;

					if (i == componentsWithoutPreferredSize.size() - 1)
					{
						size = remainingSize - sizeAssigned;
					}
					else
					{
						size = remainingSize / numNonPreferred;
						sizeAssigned += size;
					}

					componentSizes.put(comp, size);
				}
			}
		}

		for (Component comp : components)
		{
			int size = componentSizes.get(comp);
			if (verticalOrientation)
			{
				comp.setBounds(0, position, getWidth(), size);
			}
			else
			{
				comp.setBounds(position, 0, size, getHeight());
			}
			position += size;
		}
		notifyLayoutChanged();
	}

	private void notifyLayoutChanged()
	{
		Container c = getParent();
		while (c != null && !(c instanceof DockingPanel))
		{
			c = c.getParent();
		}
		if (c instanceof DockingPanel)
		{
			((DockingPanel) c).onLayoutChanged();
		}
	}

}

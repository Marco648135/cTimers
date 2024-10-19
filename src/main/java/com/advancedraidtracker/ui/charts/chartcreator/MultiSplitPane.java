package com.advancedraidtracker.ui.charts.chartcreator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MultiSplitPane extends JPanel
{
	private static final int DIVIDER_SIZE = 2;
	@Getter
	private final boolean verticalOrientation;
	private final List<Component> components = new ArrayList<>();
	private final boolean isFlexible;
	@Setter
	@Getter
	private boolean isPrimaryContainer = false;

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
		if (!components.isEmpty())
		{
			Divider divider = new Divider(verticalOrientation, this);
			components.add(divider);
			add(divider);
		}
		components.add(comp);
		add(comp);
		revalidate();
		repaint();
	}

	public void addComponentAt(int index, Component comp)
	{
		if (!components.isEmpty() && index > 0)
		{
			Divider divider = new Divider(verticalOrientation, this);
			components.add(index, divider);
			add(divider);
		}
		components.add(index, comp);
		add(comp);
		revalidate();
		repaint();
	}

	public void splitPane(Component newComponent, boolean verticalSplit, boolean insertBefore)
	{
		if (this.verticalOrientation == verticalSplit)
		{
			// We can simply add the new component to this pane
			if (insertBefore)
			{
				addComponentAt(0, newComponent);
			}
			else
			{
				addComponent(newComponent);
			}
		}
		else
		{
			// Need to wrap existing components into a new pane with the desired orientation
			MultiSplitPane newSplitPane = new MultiSplitPane(verticalSplit, true);
			List<Component> existingComponents = new ArrayList<>(components);

			components.clear();
			removeAll();

			if (insertBefore)
			{
				newSplitPane.addComponent(newComponent);
				for (Component comp : existingComponents)
				{
					newSplitPane.addComponent(comp);
				}
			}
			else
			{
				for (Component comp : existingComponents)
				{
					newSplitPane.addComponent(comp);
				}
				newSplitPane.addComponent(newComponent);
			}
			components.add(newSplitPane);
			add(newSplitPane);
		}
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

		if (!components.isEmpty())
		{
			if (index > 0 && components.get(index - 1) instanceof Divider)
			{
				remove(components.get(index - 1));
				components.remove(index - 1);
				index--;
			}
			if (index < components.size() && components.get(index) instanceof Divider)
			{
				remove(components.get(index));
				components.remove(index);
			}
		}


		if (components.isEmpty())
		{
			Container parent = getParent();
			if (parent instanceof MultiSplitPane)
			{
				MultiSplitPane parentSplitPane = (MultiSplitPane) parent;
				parentSplitPane.removeComponent(this);
			}
			else if (parent != null)
			{
				parent.remove(this);
				JPanel placeholder = new JPanel();
				placeholder.setBackground(Color.GRAY);
				parent.add(placeholder);
				parent.revalidate();
				parent.repaint();
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
				return;
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
				return;
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
			return;
		}

		MultiSplitPane newSplitPane = new MultiSplitPane(verticalSplit);

		Dimension existingPreferredSize = existingComponent.getPreferredSize();
		newSplitPane.setPreferredSize(Objects.requireNonNullElseGet(existingPreferredSize, () -> new Dimension(0, 0)));

		remove(existingComponent);
		components.set(index, newSplitPane);
		add(newSplitPane);

		Container newComponentParent = newComponent.getParent();
		if (newComponentParent != null && newComponentParent != newSplitPane)
		{
			if (newComponentParent instanceof MultiSplitPane)
			{
				((MultiSplitPane) newComponentParent).removeComponent(newComponent);
			}
			else
			{
				newComponentParent.remove(newComponent);
			}
		}

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

	private void layoutComponents()
	{
		if (components.size() == 1 && !(components.get(0) instanceof Divider))
		{
			// Only one component, fill the entire space
			Component comp = components.get(0);
			comp.setBounds(0, 0, getWidth(), getHeight());
			return;
		}

		int totalSize = verticalOrientation ? getHeight() : getWidth();
		int position = 0;

		int totalDividerSize = 0;
		int totalPreferredSize = 0;
		int numFlexibleComponents = 0;

		Map<Component, Integer> componentSizes = new HashMap<>();

		for (Component comp : components)
		{
			if (comp instanceof Divider)
			{
				totalDividerSize += DIVIDER_SIZE;
			}
			else
			{
				boolean isFlexibleComponent = false;

				// Treat JTabbedPane and flexible MultiSplitPane as flexible components
				if (comp instanceof JTabbedPane)
				{
					isFlexibleComponent = true;
				}
				else if (comp instanceof MultiSplitPane)
				{
					if (((MultiSplitPane) comp).isFlexible)
					{
						isFlexibleComponent = true;
					}
				}

				if (isFlexibleComponent)
				{
					numFlexibleComponents++;
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
		}

		int availableSize = totalSize - totalDividerSize;
		int flexibleSpace = availableSize - totalPreferredSize;

		// Adjust sizes if total preferred sizes exceed available space
		if (totalPreferredSize > availableSize)
		{
			double scale = (double) availableSize / totalPreferredSize;

			totalPreferredSize = 0;
			for (Map.Entry<Component, Integer> entry : componentSizes.entrySet())
			{
				int size = (int) (entry.getValue() * scale);
				componentSizes.put(entry.getKey(), size);
				totalPreferredSize += size;
			}

			flexibleSpace = availableSize - totalPreferredSize;
		}

		int flexibleComponentSize = numFlexibleComponents > 0 ? flexibleSpace / numFlexibleComponents : 0;

		for (Component comp : components)
		{
			if (comp instanceof Divider)
			{
				if (verticalOrientation)
				{
					comp.setBounds(0, position, getWidth(), DIVIDER_SIZE);
				}
				else
				{
					comp.setBounds(position, 0, DIVIDER_SIZE, getHeight());
				}
				position += DIVIDER_SIZE;
			}
			else
			{
				int compSize;
				compSize = componentSizes.getOrDefault(comp, flexibleComponentSize);

				if (verticalOrientation)
				{
					comp.setBounds(0, position, getWidth(), compSize);
				}
				else
				{
					comp.setBounds(position, 0, compSize, getHeight());
				}
				position += compSize;
			}
		}
	}

}

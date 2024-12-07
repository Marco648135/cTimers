package com.advancedraidtracker.ui.docking;

import com.google.gson.Gson;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class DockingPanel extends JPanel implements CustomLayerListener
{
	private CustomPanel selectedPanel;
	private Map<String, CustomPanel> registry = new LinkedHashMap<>();
	private Map<String, CustomPanel> customPanelMap = new LinkedHashMap<>(); // This map is for reconstructed panels from tabs.
	private String layoutFilePath;
	MultiSplitPane mainPane;

	private Function<String, CustomPanel> panelFactory;

	public void setPanelFactory(Function<String, CustomPanel> factory)
	{
		this.panelFactory = factory;
	}

	private boolean loadingLayout = false;

	private String backupLayoutFilePath;

	public DockingPanel(String layoutFilePath)
	{
		this.layoutFilePath = layoutFilePath;

		int dotIndex = layoutFilePath.lastIndexOf('.');
		if (dotIndex == -1)
		{
			backupLayoutFilePath = layoutFilePath + "_backup";
		}
		else
		{
			String baseName = layoutFilePath.substring(0, dotIndex);
			String extension = layoutFilePath.substring(dotIndex);
			backupLayoutFilePath = baseName + "_backup" + extension;
		}
	}

	public void registerPanel(CustomPanel panel)
	{
		registry.put(panel.getTitle(), panel);
	}

	public void init(MultiSplitPane mainPane)
	{
		this.mainPane = mainPane;
		LayoutNode rootNode = createLayoutFromMultiSplitPane(mainPane);

		collectExistingPanels(mainPane);

		File f = new File(layoutFilePath);
		if (!f.exists())
		{
			saveLayout(rootNode, layoutFilePath);
			saveLayout(rootNode, backupLayoutFilePath);
			loadAndReconstruct();
		}
		else
		{
			loadAndReconstruct();
		}
	}

	public void resetToDefault()
	{
		LayoutNode backupLayout = loadLayout(backupLayoutFilePath);
		if (backupLayout == null)
		{
			JOptionPane.showMessageDialog(this, "Failed to load backup layout.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		saveLayout(backupLayout, layoutFilePath);

		registry.clear();
		customPanelMap.clear();
		collectExistingPanels(mainPane);
		removeAll();
		revalidate();
		repaint();

		loadAndReconstruct();
	}

	private void collectExistingPanels(MultiSplitPane pane)
	{
		for (Component c : pane.getComponents())
		{
			if (c instanceof MultiSplitPane)
			{
				collectExistingPanels((MultiSplitPane) c);
			}
			else if (c instanceof CustomPanel)
			{
				CustomPanel cp = (CustomPanel) c;
				if (!customPanelMap.containsKey(cp.getTitle()))
				{
					customPanelMap.put(cp.getTitle(), cp);

					int tabCount = cp.getTabbedPane().getTabCount();
					for (int i = 1; i < tabCount; i++)
					{
						String childTitle = cp.getTabbedPane().getTitleAt(i);
						Component comp = cp.getTabbedPane().getComponentAt(i);

						CustomPanel childCP = new CustomPanel(childTitle);
						childCP.getContentPanel().setLayout(new BorderLayout());
						childCP.getContentPanel().add(comp);
						customPanelMap.put(childTitle, childCP);
					}
				}
			}
		}
	}


	public Map<String, CustomPanel> getCustomPanelMap()
	{
		return customPanelMap;
	}


	public CustomPanel createOrGetPanel(String name)
	{
		if (customPanelMap.containsKey(name))
		{
			return customPanelMap.get(name);
		}
		if (registry.containsKey(name))
		{
			CustomPanel cp = registry.remove(name);
			customPanelMap.put(name, cp);
			return cp;
		}
		if (panelFactory == null)
		{
			throw new IllegalStateException("No panelFactory set to create new panels.");
		}
		CustomPanel cp = panelFactory.apply(name);
		customPanelMap.put(name, cp);
		return cp;
	}


	private void loadAndReconstruct()
	{
		loadingLayout = true;
		try
		{
			LayoutNode root = loadLayout(layoutFilePath);
			removeAll();

			if (root.registryPanels != null)
			{
				for (LayoutNode regPanelNode : root.registryPanels)
				{
					CustomPanel cp = buildCustomPanelFromNode(regPanelNode);
					registry.put(cp.getTitle(), cp);
				}
			}

			MultiSplitPane reconstructed = rebuildFromLayoutNode(root);
			this.mainPane = reconstructed;

			CustomLayerUI layerUI = new CustomLayerUI();
			layerUI.setRootMultiSplitPane(mainPane);

			JLayer<JComponent> layer = new JLayer<>(mainPane, layerUI);
			layer.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
			layerUI.setCustomLayerListener(this);

			new GlobalDropTarget(layer, layerUI, mainPane, this);
			setLayout(new BorderLayout());
			add(layer, BorderLayout.CENTER);

			revalidate();
			repaint();
		}
		finally
		{
			loadingLayout = false;
		}
	}

	@Override
	public void onCustomPanelClicked(CustomPanel panel)
	{
		if (selectedPanel != null && selectedPanel != panel)
		{
			selectedPanel.setDeselected();
		}
		selectedPanel = panel;
		selectedPanel.setSelected();
	}

	public void onLayoutChanged()
	{
		if (loadingLayout)
		{
			return;
		}

		LayoutNode layoutNode = createLayoutFromMultiSplitPane(mainPane);
		layoutNode.registryPanels = new ArrayList<>();
		for (CustomPanel cp : registry.values())
		{
			layoutNode.registryPanels.add(createNodeFromCustomPanel(cp));
		}
		saveLayout(layoutNode, layoutFilePath);
	}

	private LayoutNode createLayoutFromMultiSplitPane(MultiSplitPane pane)
	{
		LayoutNode node = new LayoutNode();
		node.type = "MultiSplitPane";
		node.vertical = pane.isVerticalOrientation();

		Dimension pref = pane.getPreferredSize();
		if (pref == null)
		{
			pref = new Dimension(0, 0);
		}
		node.width = pref.width;
		node.height = pref.height;

		node.children = new ArrayList<>();

		for (Component c : pane.getComponents())
		{
			if (c instanceof MultiSplitPane)
			{
				node.children.add(createLayoutFromMultiSplitPane((MultiSplitPane) c));
			}
			else if (c instanceof CustomPanel)
			{
				node.children.add(createNodeFromCustomPanelHierarchy((CustomPanel) c));
			}
		}
		return node;
	}

	private LayoutNode createNodeFromCustomPanelHierarchy(CustomPanel panel)
	{
		LayoutNode node = new LayoutNode();
		node.type = "CustomPanel";
		node.title = panel.getTitle();
		node.width = panel.getWidth();
		node.height = panel.getHeight();

		int tabCount = panel.getTabbedPane().getTabCount();
		node.children = new ArrayList<>();
		if (tabCount > 0)
		{
			for (int i = 1; i < tabCount; i++)
			{
				String childTitle = panel.getTabbedPane().getTitleAt(i);
				Component comp = panel.getTabbedPane().getComponentAt(i);

				LayoutNode childNode = new LayoutNode();
				childNode.type = "CustomPanel";
				childNode.title = childTitle;
				childNode.width = comp.getWidth();
				childNode.height = comp.getHeight();
				childNode.children = new ArrayList<>();

				node.children.add(childNode);
			}
		}

		return node;
	}

	private LayoutNode createNodeFromCustomPanel(CustomPanel panel)
	{
		LayoutNode node = new LayoutNode();
		node.type = "CustomPanel";
		node.title = panel.getTitle();
		node.width = panel.getWidth();
		node.height = panel.getHeight();
		node.children = new ArrayList<>();
		return node;
	}

	private MultiSplitPane rebuildFromLayoutNode(LayoutNode node)
	{
		if (!"MultiSplitPane".equals(node.type))
		{
			throw new IllegalArgumentException("Expected MultiSplitPane node");
		}

		MultiSplitPane pane = new MultiSplitPane(node.vertical, true);
		if (node.width != 0 || node.height != 0)
		{
			pane.setPreferredSize(new Dimension(node.width, node.height));
		}

		for (LayoutNode childNode : node.children)
		{
			if ("MultiSplitPane".equals(childNode.type))
			{
				pane.addComponent(rebuildFromLayoutNode(childNode));
			}
			else if ("CustomPanel".equals(childNode.type))
			{
				CustomPanel cp = buildCustomPanelFromNode(childNode);
				pane.addComponent(cp);
			}
		}

		return pane;
	}

	private CustomPanel buildCustomPanelFromNode(LayoutNode node)
	{
		if (!"CustomPanel".equals(node.type))
		{
			throw new IllegalArgumentException("Expected CustomPanel node");
		}

		CustomPanel cp;
		if (customPanelMap.containsKey(node.title))
		{
			cp = customPanelMap.get(node.title);
		}
		else if (registry.containsKey(node.title))
		{
			cp = registry.remove(node.title);
		}
		else
		{
			// Panel not found, create it using panelFactory
			if (panelFactory == null)
			{
				throw new IllegalStateException("No panelFactory set to create new panels.");
			}
			cp = panelFactory.apply(node.title);
			customPanelMap.put(node.title, cp);
		}

		if (node.children != null && !node.children.isEmpty())
		{
			for (LayoutNode childNode : node.children)
			{
				CustomPanel childPanel = buildCustomPanelFromNode(childNode);
				cp.addCustomPanelAsTab(childPanel);
			}
		}

		return cp;
	}


	private void saveLayout(LayoutNode rootNode, String path)
	{
		Gson gson = new Gson();
		String json = gson.toJson(rootNode);
		try
		{
			Files.write(Paths.get(path), json.getBytes());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private LayoutNode loadLayout(String path)
	{
		Gson gson = new Gson();
		try
		{
			String json = new String(Files.readAllBytes(Paths.get(path)));
			return gson.fromJson(json, LayoutNode.class);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public void registerClosedTabAsPanel(String title, Component comp)
	{
		CustomPanel newPanel = new CustomPanel(title);
		newPanel.getContentPanel().setLayout(new BorderLayout());
		newPanel.getContentPanel().add(comp);
		registerPanel(newPanel);
		// After changing registry, rewrite layout
		onLayoutChanged();
	}

	public Map<String, CustomPanel> getRegistry()
	{
		return registry;
	}

	public void removeFromRegistry(String title)
	{
		registry.remove(title);
		onLayoutChanged();
	}
}

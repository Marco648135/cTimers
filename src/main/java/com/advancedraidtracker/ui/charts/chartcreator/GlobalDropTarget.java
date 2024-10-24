// GlobalDropTarget.java
package com.advancedraidtracker.ui.charts.chartcreator;

import static com.advancedraidtracker.ui.charts.chartcreator.DockingUtility.getTrapezoids;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.io.IOException;

public class GlobalDropTarget implements DropTargetListener {
	private final CustomLayerUI layerUI;
	private final JLayer<JComponent> layer;
	private MultiSplitPane mainPane;
	private final JFrame frame;
	private final ChartCreatorFrame chartCreatorFrame;
	private static final int margin = 40;

	public GlobalDropTarget(JComponent component, CustomLayerUI layerUI, MultiSplitPane mainPane, JFrame frame, ChartCreatorFrame chartCreatorFrame) {
		this.layerUI = layerUI;
		this.layer = (JLayer<JComponent>) component;
		this.mainPane = mainPane;
		this.frame = frame;
		this.chartCreatorFrame = chartCreatorFrame;
		new DropTarget(component, DnDConstants.ACTION_MOVE, this, true, null);
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		dtde.acceptDrag(DnDConstants.ACTION_MOVE);
		updateOverlay(dtde.getLocation());
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		dtde.acceptDrag(DnDConstants.ACTION_MOVE);
		updateOverlay(dtde.getLocation());
	}

	private void updateOverlay(Point location) {
		// location is in the coordinate space of the JLayer
		Point mouseLocationInMainPane = SwingUtilities.convertPoint(layer, location, mainPane);

		Component componentUnderMouse = SwingUtilities.getDeepestComponentAt(mainPane, mouseLocationInMainPane.x, mouseLocationInMainPane.y);

		boolean isOverPanel = isCustomPanel(componentUnderMouse);

		if (isOverPanel && !isNearMainPaneEdge(mouseLocationInMainPane)) {
			CustomPanel targetPanel = getCustomPanelFromComponent(componentUnderMouse);
			if (targetPanel != null) {
				Rectangle panelBoundsInMainPane = SwingUtilities.convertRectangle(targetPanel.getParent(), targetPanel.getBounds(), mainPane);

				// Convert panelBounds to the coordinate space of the JLayer
				Rectangle panelBoundsInLayer = SwingUtilities.convertRectangle(mainPane, panelBoundsInMainPane, layer);

				Shape[] shapes = getTrapezoids(panelBoundsInLayer.width, panelBoundsInLayer.height, panelBoundsInLayer.x, panelBoundsInLayer.y);
				layerUI.setOverlayShapes(shapes);
				layerUI.setOverlayVisible(true);
				updateOverlayMousePosition(location);
			}
		} else if (isNearMainPaneEdge(mouseLocationInMainPane)) {
			Rectangle mainPaneBoundsInLayer = new Rectangle(0, 0, layer.getWidth(), layer.getHeight());

			Shape[] shapes = calculateEdgeDropShapes(mainPaneBoundsInLayer);
			layerUI.setOverlayShapes(shapes);
			layerUI.setOverlayVisible(true);
			updateOverlayMousePosition(location);
		} else {
			layerUI.setOverlayVisible(false);
		}
	}

	private void updateOverlayMousePosition(Point mouseLocationInLayer) {
		layerUI.updateOverlayMousePosition(mouseLocationInLayer);
	}

	private boolean isCustomPanel(Component comp) {
		while (comp != null) {
			if (comp instanceof CustomPanel) {
				return true;
			}
			comp = comp.getParent();
		}
		return false;
	}

	private CustomPanel getCustomPanelFromComponent(Component comp) {
		while (comp != null) {
			if (comp instanceof CustomPanel) {
				return (CustomPanel) comp;
			}
			comp = comp.getParent();
		}
		return null;
	}

	private boolean isNearMainPaneEdge(Point pointInMainPane) {
		Rectangle rect = new Rectangle(0, 0, mainPane.getWidth(), mainPane.getHeight());
		int x = pointInMainPane.x;
		int y = pointInMainPane.y;

		return x >= rect.x - margin && x <= rect.x + margin
			|| x >= rect.x + rect.width - margin && x <= rect.x + rect.width + margin
			|| y >= rect.y - margin && y <= rect.y + margin
			|| y >= rect.y + rect.height - margin && y <= rect.y + rect.height + margin;
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
		layerUI.setOverlayVisible(false);
	}

	@Override
	public void drop(DropTargetDropEvent dtde) {
		try {
			Transferable transferable = dtde.getTransferable();

			if (!transferable.isDataFlavorSupported(TabTransferable.TAB_INDEX_FLAVOR) ||
				!transferable.isDataFlavorSupported(TabTransferable.TABBED_PANE_FLAVOR)) {
				dtde.rejectDrop();
				return;
			}

			dtde.acceptDrop(DnDConstants.ACTION_MOVE);

			int draggedTabIndex = (Integer) transferable.getTransferData(TabTransferable.TAB_INDEX_FLAVOR);
			JTabbedPane sourceTabbedPane = (JTabbedPane) transferable.getTransferData(TabTransferable.TABBED_PANE_FLAVOR);
			CustomPanel sourceCustomPanel = (CustomPanel) SwingUtilities.getAncestorOfClass(CustomPanel.class, sourceTabbedPane);

			if (sourceCustomPanel == null) {
				dtde.rejectDrop();
				return;
			}

			Component draggedComponent = sourceTabbedPane.getComponentAt(draggedTabIndex);
			String draggedTitle = sourceTabbedPane.getTitleAt(draggedTabIndex);
			Component draggedTabComponent = sourceTabbedPane.getTabComponentAt(draggedTabIndex);

			Point mouseLocationInMainPane = SwingUtilities.convertPoint(dtde.getDropTargetContext().getComponent(), dtde.getLocation(), mainPane);

			Component componentUnderMouse = SwingUtilities.getDeepestComponentAt(mainPane, mouseLocationInMainPane.x, mouseLocationInMainPane.y);

			boolean isOverPanel = isCustomPanel(componentUnderMouse);
			boolean nearMainPaneEdge = isNearMainPaneEdge(mouseLocationInMainPane);

			CustomPanel targetPanel = getCustomPanelFromComponent(componentUnderMouse);
			int highlightedIndex = layerUI.getHighlightedIndex();

			if (highlightedIndex == -1 || (targetPanel == sourceCustomPanel && sourceTabbedPane.getTabCount() == 1)) {
				dtde.rejectDrop();
				return;
			}

			sourceTabbedPane.remove(draggedComponent);
			if (sourceTabbedPane.getTabCount() == 0) {
				sourceCustomPanel.closePanel();
			}

			boolean verticalSplit;
			boolean insertBefore;

			switch (highlightedIndex) {
				case 0: // Top
					verticalSplit = true;
					insertBefore = true;
					break;
				case 1: // Right
					verticalSplit = false;
					insertBefore = false;
					break;
				case 2: // Bottom
					verticalSplit = true;
					insertBefore = false;
					break;
				case 3: // Left
					verticalSplit = false;
					insertBefore = true;
					break;
				case 4: // Center
					verticalSplit = false;
					insertBefore = false;
					break;
				default:
					dtde.dropComplete(false);
					return;
			}

			if (nearMainPaneEdge) {
				CustomPanel newPanel = new CustomPanel(draggedTitle);
				newPanel.addTab(draggedTitle, draggedComponent);
				newPanel.getTabbedPane().remove(0);
				newPanel.getTabbedPane().setSelectedIndex(0);

				if (mainPane.isVerticalOrientation()) {
					mainPane.splitPane(newPanel, verticalSplit, insertBefore);
				} else {
					MultiSplitPane newRootPane = new MultiSplitPane(verticalSplit, true);

					if (insertBefore) {
						newRootPane.addComponent(newPanel);
						newRootPane.addComponent(mainPane);
					} else {
						newRootPane.addComponent(mainPane);
						newRootPane.addComponent(newPanel);
					}

					chartCreatorFrame.setMainPane(newRootPane);
					this.mainPane = newRootPane; // Update the reference to mainPane
				}
			} else if (isOverPanel) {
				if (highlightedIndex == 4) { // Center
					if (targetPanel != null) {
						JTabbedPane targetTabbedPane = targetPanel.getTabbedPane();
						int newIndex = targetTabbedPane.getTabCount();
						targetTabbedPane.addTab(draggedTitle, draggedComponent);
						targetTabbedPane.setTabComponentAt(newIndex, draggedTabComponent);
						targetPanel.setCustomTabComponent(targetTabbedPane, newIndex);
						targetTabbedPane.setSelectedIndex(newIndex);
						targetTabbedPane.revalidate();
						targetTabbedPane.repaint();
					}
				} else {
					if (targetPanel != null) {
						CustomPanel newPanel = new CustomPanel(draggedTitle);
						newPanel.addTab(draggedTitle, draggedComponent);
						newPanel.getTabbedPane().remove(0);
						newPanel.getTabbedPane().setSelectedIndex(0);
						MultiSplitPane targetSplitPane = getAncestorSplitPane(targetPanel);
						if (targetSplitPane != null) {
							targetSplitPane.splitComponent(targetPanel, newPanel, verticalSplit, insertBefore);
						} else {
							dtde.dropComplete(false);
							return;
						}
					}
				}
			} else {
				dtde.rejectDrop();
				return;
			}

			dtde.dropComplete(true);

		} catch (UnsupportedFlavorException | IOException ex) {
			dtde.dropComplete(false);
		} finally {
			layerUI.setOverlayVisible(false);
		}
	}

	private MultiSplitPane getAncestorSplitPane(Component comp) {
		while (comp != null) {
			if (comp instanceof MultiSplitPane) {
				return (MultiSplitPane) comp;
			}
			comp = comp.getParent();
		}
		return null;
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
	}

	private Shape[] calculateEdgeDropShapes(Rectangle panelBounds) {
		int w = panelBounds.width;
		int h = panelBounds.height;
		int x = panelBounds.x;
		int y = panelBounds.y;

		Shape[] shapes = new Shape[4];
		shapes[0] = new Rectangle(x, y, w, margin);
		shapes[1] = new Rectangle(x + w - margin, y, margin, h);
		shapes[2] = new Rectangle(x, y + h - margin, w, margin);
		shapes[3] = new Rectangle(x, y, margin, h);
		return shapes;
	}
}

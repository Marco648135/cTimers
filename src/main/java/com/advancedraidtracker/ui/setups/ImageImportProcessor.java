package com.advancedraidtracker.ui.setups;

import com.advancedraidtracker.utility.UISwingUtility;
import java.awt.datatransfer.Clipboard;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class ImageImportProcessor
{
	private static final int SLOT_SIZE_MIN = 20;
	private static final int INVENTORY_ROWS = 7;
	private static final int INVENTORY_COLS = 4;
	private static final int RUNEPOUCH_ROWS = 1;
	private static final int RUNEPOUCH_COLS = 4;
	private static final int EQUIP_ROWS = 5;
	private static final int EQUIP_COLS = 3;

	private boolean debugMode = false;
	private List<DebugSlotInfo> debugData = new ArrayList<>();

	public ImageImportProcessor()
	{
	}

	public void setDebugMode(boolean debug)
	{
		this.debugMode = debug;
	}

	public void importFromClipboardAsync(SetupsContainer container)
	{
		CompletableFuture.runAsync(() -> {
			Image clipboardImage = getClipboardImage();
			if (clipboardImage == null)
			{
				SwingUtilities.invokeLater(() ->
					showError(container, "No image found in clipboard.")
				);
				return;
			}

			BufferedImage image = toBufferedImage(clipboardImage);
			int width = image.getWidth();
			int height = image.getHeight();

			if (width < SLOT_SIZE_MIN || height < SLOT_SIZE_MIN)
			{
				SwingUtilities.invokeLater(() ->
					showError(container, "Image too small.")
				);
				return;
			}

			int baseColor = image.getRGB(1,1);

			List<DetectedSquare> squares = new ArrayList<>();
			boolean[][] usedAsStart = new boolean[height][width];

			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					if (usedAsStart[y][x]) continue;
					int c = image.getRGB(x,y);
					if (c == baseColor) continue;
					DetectedSquare sq = identifySquareOutline(image, x, y, baseColor);
					if (sq != null)
					{
						squares.add(sq);
						for (int yy = sq.y; yy < sq.y + sq.size; yy++)
						{
							for (int xx = sq.x; xx < sq.x + sq.size; xx++)
							{
								if (yy < height && xx < width)
									usedAsStart[yy][xx] = true;
							}
						}
					}
				}
			}

			if (squares.isEmpty())
			{
				SwingUtilities.invokeLater(() ->
					showError(container, "Failed to identify any squares. This feature is expecting an image that is from either this plugin or inventory setups.")
				);
				return;
			}


			squares.sort(Comparator.comparingInt(s -> s.x));

			List<List<DetectedSquare>> verticalSetups = new ArrayList<>();
			List<DetectedSquare> currentSet = new ArrayList<>();
			int lastX = -1;

			for (DetectedSquare sq : squares)
			{
				if (!currentSet.isEmpty() && (sq.x - lastX) > 5)
				{
					verticalSetups.add(new ArrayList<>(currentSet));
					currentSet.clear();
				}
				currentSet.add(sq);
				lastX = sq.x + sq.size + 3;
			}
			if (!currentSet.isEmpty())
			{
				verticalSetups.add(currentSet);
			}

			for (List<DetectedSquare> vSet : verticalSetups)
			{
				int count = vSet.size();
				if (count != 43 && count != 44 && count != 47 && count != 48 && count != 45 && count != 49)
				{
					SwingUtilities.invokeLater(() ->
						showError(container, "A vertical setup has " + count + " squares, expected 43-45 or 47-49")
					);
					return;
				}
			}

			List<SetupExtractedData> extractedSetups = new ArrayList<>();
			Map<Integer, BufferedImage> loadedItemImages = getLoadedItemImages();
			ImageSimilarity comparator = new ImageSimilarity();

			for (int i = 0; i < verticalSetups.size(); i++)
			{
				List<DetectedSquare> vSet = verticalSetups.get(i);
				try
				{
					SetupExtractedData data = processVerticalSetup(vSet, image, loadedItemImages, comparator, i);
					extractedSetups.add(data);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			String textFormat = buildTextFormatFromExtracted(extractedSetups);

			if (debugMode && !debugData.isEmpty())
			{
				SwingUtilities.invokeLater(() -> {
					DebugWindow dw = new DebugWindow(debugData, () -> {
						container.importSetupsFromText(textFormat);
					});
					dw.setVisible(true);
				});
			}
			else
			{
				SwingUtilities.invokeLater(() -> {
					container.importSetupsFromText(textFormat);
				});
			}

		});
	}

	private SetupExtractedData processVerticalSetup(List<DetectedSquare> vSet, BufferedImage image,
													Map<Integer, BufferedImage> loadedItemImages, ImageSimilarity comparator, int setupIndex)
	{
		SetupExtractedData data = new SetupExtractedData();
		vSet.sort(Comparator.comparingInt((DetectedSquare s)->s.y).thenComparingInt(s->s.x));

		int total = vSet.size();
		boolean hasExtraSkip = (total == 43 || total == 47);

		int rowThreshold = 10;
		List<List<DetectedSquare>> rows = clusterRows(vSet, rowThreshold);
		if (rows.size() < 13)
		{
			log.debug("Not enough rows in vertical setup " + setupIndex);
			return data;
		}

		for (int r = 0; r < INVENTORY_ROWS; r++)
		{
			List<DetectedSquare> rowSq = rows.get(r);

			int finalR = r;
			List<Integer> rowResults = IntStream.range(0, INVENTORY_COLS)
				.parallel()
				.mapToObj(c -> {
					int itemId = -1;
					if (c < rowSq.size())
					{
						BufferedImage slotImg = extractSquareImage(image, rowSq.get(c));
						itemId = findBestMatch(slotImg, loadedItemImages, comparator, "Inventory", setupIndex, finalR, c);
					}
					return itemId;
				})
				.collect(Collectors.toList());

			data.inventory.addAll(rowResults);
		}

		{
			List<DetectedSquare> rowSq = rows.get(INVENTORY_ROWS);

			List<Integer> rpResults = IntStream.range(0, RUNEPOUCH_COLS)
				.parallel()
				.mapToObj(c -> {
					int itemId = -1;
					if (c < rowSq.size())
					{
						BufferedImage slotImg = extractSquareImage(image, rowSq.get(c));
						itemId = findBestMatch(slotImg, loadedItemImages, comparator, "Runepouch", setupIndex, 0, c);
					}
					return itemId;
				})
				.collect(Collectors.toList());

			for (int i = 0; i < RUNEPOUCH_COLS; i++)
			{
				data.runepouch[i] = rpResults.get(i);
			}
		}

		int equipStartRow = INVENTORY_ROWS + RUNEPOUCH_ROWS;
		int equipCountNeeded = hasExtraSkip ? 11 : 12;

		for (int r = 0; r < EQUIP_ROWS; r++)
		{
			List<DetectedSquare> rowSq = rows.get(equipStartRow + r);
			if(r == 0)
			{
				rowSq.add(0, new DetectedSquare(0, 0, 0));
			}
			if(r == 3)
			{
				rowSq.add(new DetectedSquare(0, 0, 0));
				rowSq.add(0, new DetectedSquare(0, 0, 0));
			}
			int finalR = r;
			List<Integer> rowResults = IntStream.range(0, EQUIP_COLS)
				.parallel()
				.mapToObj(c ->
				{
					if ((finalR == 0 && c == 0) || (finalR == 3 && c == 0) || (finalR == 3 && c == 2))
					{
						return -1;
					}
					if (hasExtraSkip && finalR == 0 && c == 2)
					{
						return -1;
					}

					int itemId = -1;
					if (c < rowSq.size())
					{
						BufferedImage slotImg = extractSquareImage(image, rowSq.get(c));
						itemId = findBestMatch(slotImg, loadedItemImages, comparator, "Equipment", setupIndex, finalR, c);
					}
					return itemId;
				})
				.collect(Collectors.toList());
			data.equipment.addAll(rowResults);
		}

		int equipCount = (int) data.equipment.stream().filter(id->id != -1).count();
		if (equipCount != equipCountNeeded)
		{
			log.debug("Unexpected equip count in setup {}. Got {} items, expected {}",
				setupIndex, equipCount, equipCountNeeded);
		}

		return data;
	}

	private DetectedSquare identifySquareOutline(BufferedImage image, int startX, int startY, int baseColor)
	{
		int width = image.getWidth();
		int height = image.getHeight();
		int lineColor = image.getRGB(startX,startY);

		int x2 = startX;
		while (x2+1<width && image.getRGB(x2+1,startY)==lineColor) x2++;
		int topLineLength = x2 - startX + 1;

		int y2 = startY;
		while (y2+1<height && image.getRGB(startX,y2+1)==lineColor) y2++;
		int leftLineLength = y2 - startY + 1;

		int size = Math.min(topLineLength, leftLineLength);
		if (size < SLOT_SIZE_MIN || (Math.abs(topLineLength - leftLineLength) > 6))
		{
			return null;
		}

		int rightX = startX + topLineLength - 1;
		if (rightX >= width) return null;
		for (int yy = startY; yy < startY + leftLineLength; yy++)
		{
			if (yy >= height || image.getRGB(rightX, yy) != lineColor) return null;
		}

		int bottomY = startY + leftLineLength - 1;
		if (bottomY >= height) return null;
		for (int xx = startX; xx < startX + topLineLength; xx++)
		{
			if (xx >= width || image.getRGB(xx, bottomY) != lineColor) return null;
		}

		if (startY - 1 < 0) return null;
		for (int xx = startX; xx < startX + topLineLength; xx++)
		{
			if (image.getRGB(xx, startY - 1) != baseColor) return null;
		}
		if (bottomY + 1 >= height) return null;
		for (int xx = startX; xx < startX + topLineLength; xx++)
		{
			if (image.getRGB(xx, bottomY + 1) != baseColor) return null;
		}
		if (startX - 1 < 0) return null;
		for (int yy = startY; yy < startY + leftLineLength; yy++)
		{
			if (image.getRGB(startX - 1, yy) != baseColor) return null;
		}
		if (rightX + 1 >= width) return null;
		for (int yy = startY; yy < startY + leftLineLength; yy++)
		{
			if (image.getRGB(rightX + 1, yy) != baseColor) return null;
		}
		return new DetectedSquare(startX+(Math.abs(topLineLength-size)/2), startY, size);
	}

	private List<List<DetectedSquare>> clusterRows(List<DetectedSquare> squares, int threshold)
	{
		List<Integer> rowYs = new ArrayList<>();
		List<List<DetectedSquare>> rows = new ArrayList<>();

		for (DetectedSquare s : squares)
		{
			int found = -1;
			for (int i = 0; i < rowYs.size(); i++)
			{
				if (Math.abs(s.y - rowYs.get(i)) < threshold)
				{
					found = i;
					break;
				}
			}
			if (found == -1)
			{
				rowYs.add(s.y);
				rows.add(new ArrayList<>());
				rows.get(rows.size() - 1).add(s);
			}
			else
			{
				rows.get(found).add(s);
			}
		}
		for (List<DetectedSquare> row : rows)
		{
			row.sort(Comparator.comparingInt(a -> a.x));
		}
		List<RowPair> rowPairs = new ArrayList<>();
		for (int i = 0; i < rowYs.size(); i++)
		{
			rowPairs.add(new RowPair(rowYs.get(i), rows.get(i)));
		}
		rowPairs.sort(Comparator.comparingInt(r -> r.y));
		rows.clear();
		for (RowPair rp : rowPairs)
		{
			rows.add(rp.squares);
		}

		return rows;
	}

	private static class RowPair
	{
		int y;
		List<DetectedSquare> squares;
		RowPair(int y, List<DetectedSquare> s)
		{
			this.y = y;
			this.squares = s;
		}
	}

	private BufferedImage extractSquareImage(BufferedImage image, DetectedSquare sq)
	{
		return image.getSubimage(sq.x, sq.y, sq.size, sq.size);
	}

	private Image getClipboardImage()
	{
		try
		{
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Clipboard clipboard = toolkit.getSystemClipboard();
			if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor))
			{
				return (Image)clipboard.getData(DataFlavor.imageFlavor);
			}
		}
		catch (Exception e)
		{
			log.debug("Error retrieving image from clipboard", e);
		}
		return null;
	}

	private BufferedImage toBufferedImage(Image img)
	{
		if (img instanceof BufferedImage)
		{
			return (BufferedImage) img;
		}
		BufferedImage bimage = new BufferedImage(
			img.getWidth(null), img.getHeight(null),
			BufferedImage.TYPE_INT_ARGB
		);
		Graphics2D g = bimage.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		return bimage;
	}

	private void showError(SetupsContainer container, String message)
	{
		JOptionPane.showMessageDialog(container, message, "Error", JOptionPane.ERROR_MESSAGE);
	}


	private BufferedImage normalizeBackground(BufferedImage slotImg)
	{
		Map<Integer, Integer> freq = new HashMap<>();
		int width = slotImg.getWidth();
		int height = slotImg.getHeight();

		for (int yy = 0; yy < height; yy++)
		{
			for (int xx = 0; xx < width; xx++)
			{
				int rgb = slotImg.getRGB(xx, yy);
				freq.put(rgb, freq.getOrDefault(rgb, 0) + 1);
			}
		}

		int maxCount = -1;
		int mostCommonColor = 0;
		for (Map.Entry<Integer, Integer> e : freq.entrySet())
		{
			if (e.getValue() > maxCount)
			{
				maxCount = e.getValue();
				mostCommonColor = e.getKey();
			}
		}

		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int yy = 0; yy < height; yy++)
		{
			for (int xx = 0; xx < width; xx++)
			{
				int rgb = slotImg.getRGB(xx, yy);
				if (rgb == mostCommonColor)
				{
					result.setRGB(xx, yy, 0x00000000);
				}
				else
				{
					result.setRGB(xx, yy, rgb);
				}
			}
		}

		return result;
	}

	private static final int STANDARD_SLOT_WIDTH = 40;
	private static final int STANDARD_SLOT_HEIGHT = 40;

	private BufferedImage resizeImage(BufferedImage original, int width, int height) {
		BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(original, 0, 0, width, height, null);
		g.dispose();
		return resized;
	}




	private int findBestMatch(BufferedImage slotImg, Map<Integer, BufferedImage> loadedItemImages, ImageSimilarity comparator,
							  String sectionName, int setupIndex, int cellR, int cellC)
	{
		if (slotImg.getWidth() != STANDARD_SLOT_WIDTH || slotImg.getHeight() != STANDARD_SLOT_HEIGHT)
		{
			slotImg = resizeImage(slotImg, STANDARD_SLOT_WIDTH, STANDARD_SLOT_HEIGHT);
		}

		slotImg = normalizeBackground(slotImg);

		List<Candidate> candidates = new ArrayList<>();
		int totalPixels = slotImg.getWidth() * slotImg.getHeight();

		for (Map.Entry<Integer, BufferedImage> entry : loadedItemImages.entrySet())
		{
			BufferedImage itemImage = entry.getValue();
			ImageDifferenceData imageDiff = comparator.getDifference(slotImg, itemImage);
			double diff = imageDiff.getComputed();
			double avgDiffPerPixel = diff / totalPixels;
			candidates.add(new Candidate(entry.getKey(), avgDiffPerPixel, imageDiff.getStructure(), imageDiff.getColor()));
		}

		candidates.sort(Comparator.comparingDouble(c -> c.diff));
		int bestId = candidates.get(0).id;
		double bestScore = candidates.get(0).diff;

		if (bestScore > .0003d)
		{
			bestId = -1;
		}

		if (debugMode)
		{
			DebugSlotInfo info = new DebugSlotInfo();
			info.slotImage = slotImg;
			info.setupIndex = setupIndex;
			info.sectionName = sectionName;
			info.cellR = cellR;
			info.cellC = cellC;

			for (int i = 0; i < Math.min(5, candidates.size()); i++)
			{
				Candidate c = candidates.get(i);
				info.topCandidates.add(c);
				BufferedImage candidateImg = loadedItemImages.get(c.id);
				info.topCandidateImages.add(candidateImg);
			}
			debugData.add(info);
		}

		return bestId;
	}

	private Map<Integer, BufferedImage> getLoadedItemImages()
	{
		Map<Integer, AsyncBufferedImage> itemImages = ImageManager.imageMap;
		Map<Integer, BufferedImage> loadedItemImages = new HashMap<>();
		for (Map.Entry<Integer, AsyncBufferedImage> entry : itemImages.entrySet())
		{
			BufferedImage bi = entry.getValue();
			if (bi != null)
			{
				BufferedImage resized = normalizeImageSize(bi, STANDARD_SLOT_WIDTH, STANDARD_SLOT_HEIGHT);
				loadedItemImages.put(entry.getKey(), resized);
			}
		}
		return loadedItemImages;
	}

	private BufferedImage normalizeImageSize(BufferedImage original, int targetWidth, int targetHeight) {
		BufferedImage normalized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

		int widthPadding = targetWidth - original.getWidth();
		int heightPadding = targetHeight - original.getHeight();

		int leftPadding = widthPadding / 2;
		int topPadding = heightPadding / 2;

		Graphics2D g = normalized.createGraphics();
		g.drawImage(original, leftPadding, topPadding, null);
		g.dispose();

		return normalized;
	}


	private String buildTextFormatFromExtracted(List<SetupExtractedData> extractedSetups)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < extractedSetups.size(); i++)
		{
			if (i > 0) sb.append(",");
			SetupExtractedData d = extractedSetups.get(i);
			String setupName = "Imported Setup " + (i + 1);
			sb.append("{").append(setupName).append("}");
			sb.append("{");
			for (int idx=0; idx<d.inventory.size(); idx++)
			{
				sb.append(d.inventory.get(idx));
				if (idx < d.inventory.size()-1) sb.append(",");
			}
			sb.append("}");
			sb.append("{");
			for (int c=0; c<4; c++)
			{
				sb.append(d.runepouch[c]);
				if (c < 3) sb.append(",");
			}
			sb.append("}");
			sb.append("{");
			for (int idx=0; idx<d.equipment.size(); idx++)
			{
				sb.append(d.equipment.get(idx));
				if (idx < d.equipment.size()-1) sb.append(",");
			}
			sb.append("}");
			sb.append("{").append(setupName).append("}");
		}
		return sb.toString();
	}

	private static class SetupExtractedData
	{
		List<Integer> inventory = new ArrayList<>(INVENTORY_ROWS * INVENTORY_COLS);
		int[] runepouch = new int[4];
		List<Integer> equipment = new ArrayList<>(15);
	}

	private static class Candidate
	{
		int id;
		double diff;
		double structureDiff;
		double colorDiff;
		Candidate(int id, double diff, double structureDiff, double colorDiff)
		{
			this.id = id;
			this.diff = diff;
			this.structureDiff = structureDiff;
			this.colorDiff = colorDiff;
		}
	}

	private static class DebugSlotInfo
	{
		BufferedImage slotImage;
		List<Candidate> topCandidates = new ArrayList<>();
		List<BufferedImage> topCandidateImages = new ArrayList<>();
		int setupIndex;
		String sectionName;
		int cellR;
		int cellC;
	}

	private static class DetectedSquare
	{
		int x,y,size;
		DetectedSquare(int x,int y,int size)
		{
			this.x=x; this.y=y; this.size=size;
		}
	}

	private class DebugWindow extends JFrame
	{
		private List<DebugSlotInfo> data;
		private Runnable onComplete;
		private int currentIndex = 0;

		private DebugPanel debugPanel;
		private JButton nextButton;

		DebugWindow(List<DebugSlotInfo> data, Runnable onComplete)
		{
			this.data = data;
			this.onComplete = onComplete;
			setTitle("Debug Viewer");
			setSize(500,500);
			setLocationRelativeTo(null);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

			debugPanel = new DebugPanel();
			nextButton = new JButton("Next");
			nextButton.addActionListener(e -> {
				currentIndex++;
				if (currentIndex >= data.size())
				{
					dispose();
					onComplete.run();
				}
				else
				{
					debugPanel.repaint();
				}
			});

			getContentPane().setLayout(new BorderLayout());
			getContentPane().add(debugPanel, BorderLayout.CENTER);
			getContentPane().add(nextButton, BorderLayout.SOUTH);
		}

		private class DebugPanel extends JPanel
		{
			private static final int SCALE = 6;
			private static final int GAP = 200;

			DebugPanel()
			{
				setPreferredSize(new Dimension(1800, 400));
			}

			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);

				g.setColor(Color.WHITE);
				g.fillRect(0, 0, getWidth(), getHeight());

				if (currentIndex >= data.size())
				{
					return;
				}

				DebugSlotInfo info = data.get(currentIndex);

				String header = "Setup " + info.setupIndex + ", " + info.sectionName +
					", (" + info.cellR + "," + info.cellC + ")";
				g.setColor(Color.BLACK);
				g.drawString(header, 10, 20);

				int slotX = 10;
				int slotY = 40;
				int originalWidth = info.slotImage.getWidth();
				int originalHeight = info.slotImage.getHeight();
				int scaledWidth = originalWidth * SCALE;
				int scaledHeight = originalHeight * SCALE;

				g.drawImage(info.slotImage, slotX, slotY, scaledWidth, scaledHeight, null);

				g.setColor(Color.BLACK);
				g.drawRect(slotX, slotY, scaledWidth, scaledHeight);

				g.setColor(UISwingUtility.getTransparentColor(Color.LIGHT_GRAY, 16));
				for (int i = 1; i < originalWidth; i++)
				{
					int lineX = slotX + i * SCALE;
					g.drawLine(lineX, slotY, lineX, slotY + scaledHeight);
				}
				for (int j = 1; j < originalHeight; j++)
				{
					int lineY = slotY + j * SCALE;
					g.drawLine(slotX, lineY, slotX + scaledWidth, lineY);
				}

				int maxCandidates = Math.min(info.topCandidates.size(), 5);
				int candidateX = slotX + scaledWidth + GAP;
				int candidateY = slotY;

				for (int i = 0; i < maxCandidates; i++)
				{
					Candidate c = info.topCandidates.get(i);
					BufferedImage candImg = info.topCandidateImages.get(i);
					if (candImg == null)
					{
						continue;
					}

					int candOriginalW = candImg.getWidth();
					int candOriginalH = candImg.getHeight();
					int candScaledW = candOriginalW * SCALE;
					int candScaledH = candOriginalH * SCALE;

					g.drawImage(candImg, candidateX, candidateY, candScaledW, candScaledH, null);

					g.setColor(Color.BLACK);
					g.drawRect(candidateX, candidateY, candScaledW, candScaledH);

					g.setColor(UISwingUtility.getTransparentColor(Color.LIGHT_GRAY, 16));
					for (int col = 1; col < candOriginalW; col++)
					{
						int lineX = candidateX + col * SCALE;
						g.drawLine(lineX, candidateY, lineX, candidateY + candScaledH);
					}
					for (int row = 1; row < candOriginalH; row++)
					{
						int lineY = candidateY + row * SCALE;
						g.drawLine(candidateX, lineY, candidateX + candScaledW, lineY);
					}

					g.setColor(Color.BLACK);
					g.drawString("ID=" + c.id + ", diff=" + String.format("%.6f", c.diff) +", structure diff=" +String.format("%.6f", c.structureDiff) + ", color diff =" + String.format("%.6f",c.colorDiff),
						candidateX, candidateY - 5);

					candidateX += candScaledW + GAP;
				}
			}
		}
	}
}

package com.advancedraidtracker.ui.setups;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageSimilarity
{
	private static final int GRID_SIZE = 4;
	private static final int ORIENTATION_BINS = 8;
	private static final int COLOR_BINS = 8;
	private static final int HUE_BINS = 36;

	private final Map<BufferedImage, double[]> descriptorCache = new ConcurrentHashMap<>();
	private final Map<BufferedImage, double[]> colorHistCache  = new ConcurrentHashMap<>();
	private final Map<BufferedImage, double[]> hueCache        = new ConcurrentHashMap<>();

	public ImageDifferenceData getDifference(BufferedImage img1, BufferedImage img2)
	{
		if (img1 == null || img2 == null)
		{
			return new ImageDifferenceData(1, 1, 1);
		}

		double[] desc1 = descriptorCache.computeIfAbsent(img1,
			key -> computeDescriptor(toGrayscale(img1))
		);
		double[] desc2 = descriptorCache.computeIfAbsent(img2,
			key -> computeDescriptor(toGrayscale(img2))
		);
		double structuralDiff = compareDescriptors(desc1, desc2);

		double[] colHist1 = colorHistCache.computeIfAbsent(img1,
			key -> computeColorHistogram(img1)
		);
		double[] colHist2 = colorHistCache.computeIfAbsent(img2,
			key -> computeColorHistogram(img2)
		);
		double oldColorDiff = compareColorHistograms(colHist1, colHist2);

		double[] hueHist1 = hueCache.computeIfAbsent(img1,
			key -> computeHueHistogram(img1)
		);
		double[] hueHist2 = hueCache.computeIfAbsent(img2,
			key -> computeHueHistogram(img2)
		);
		double hueDiff = compareHueHistograms(hueHist1, hueHist2);

		double scaledHueDiff = Math.min(hueDiff / 2.0, 1.0);

		double combinedDiff =
			0.85 * structuralDiff
				+ 0 * oldColorDiff
				+ 0.15 * scaledHueDiff;

		return new ImageDifferenceData(structuralDiff, scaledHueDiff, combinedDiff);
	}

	private BufferedImage toGrayscale(BufferedImage input)
	{
		BufferedImage output = new BufferedImage(
			input.getWidth(),
			input.getHeight(),
			BufferedImage.TYPE_BYTE_GRAY
		);
		Graphics2D g2d = output.createGraphics();
		g2d.drawImage(input, 0, 0, null);
		g2d.dispose();
		return output;
	}

	private double[] computeDescriptor(BufferedImage grayImage)
	{
		int width = grayImage.getWidth();
		int height = grayImage.getHeight();

		double[][] magnitudes   = new double[height][width];
		double[][] orientations = new double[height][width];
		computeGradients(grayImage, magnitudes, orientations);

		double[] descriptor = new double[GRID_SIZE * GRID_SIZE * ORIENTATION_BINS];
		int cellWidth  = width  / GRID_SIZE;
		int cellHeight = height / GRID_SIZE;

		for (int i = 0; i < GRID_SIZE; i++)
		{
			for (int j = 0; j < GRID_SIZE; j++)
			{
				int startX = j * cellWidth;
				int startY = i * cellHeight;

				double[] histogram = computeOrientationHistogram(
					magnitudes, orientations,
					startX, startY, cellWidth, cellHeight
				);

				System.arraycopy(histogram, 0,
					descriptor, (i * GRID_SIZE + j) * ORIENTATION_BINS,
					ORIENTATION_BINS
				);
			}
		}

		normalizeDescriptor(descriptor);
		return descriptor;
	}

	private void computeGradients(BufferedImage img, double[][] magnitudes, double[][] orientations)
	{
		int width  = img.getWidth();
		int height = img.getHeight();

		float[] sobelX = {-1, 0, 1, -2, 0, 2, -1, 0, 1};
		float[] sobelY = {-1, -2, -1,  0, 0, 0,  1,  2, 1};

		for (int y = 1; y < height - 1; y++)
		{
			for (int x = 1; x < width - 1; x++)
			{
				double gx = 0, gy = 0;
				int index = 0;

				for (int i = -1; i <= 1; i++)
				{
					for (int j = -1; j <= 1; j++)
					{
						int pixel = img.getRGB(x + j, y + i) & 0xFF;
						gx += pixel * sobelX[index];
						gy += pixel * sobelY[index];
						index++;
					}
				}

				magnitudes[y][x]   = Math.sqrt(gx * gx + gy * gy);
				orientations[y][x] = Math.atan2(gy, gx);
			}
		}
	}

	private double[] computeOrientationHistogram(
		double[][] magnitudes,
		double[][] orientations,
		int startX,
		int startY,
		int cellWidth,
		int cellHeight)
	{
		double[] histogram = new double[ORIENTATION_BINS];

		for (int yy = startY; yy < startY + cellHeight && yy < magnitudes.length; yy++)
		{
			for (int xx = startX; xx < startX + cellWidth && xx < magnitudes[0].length; xx++)
			{
				double orientation = orientations[yy][xx];
				double magnitude   = magnitudes[yy][xx];

				double degrees = Math.toDegrees(orientation) + 180;
				int bin = (int)(degrees * ORIENTATION_BINS / 360) % ORIENTATION_BINS;
				histogram[bin] += magnitude;
			}
		}

		return histogram;
	}

	private void normalizeDescriptor(double[] descriptor)
	{
		double sum = 0;
		for (double value : descriptor)
		{
			sum += value * value;
		}
		double magnitude = Math.sqrt(sum);

		if (magnitude > 0)
		{
			for (int i = 0; i < descriptor.length; i++)
			{
				descriptor[i] /= magnitude;
				descriptor[i] = Math.min(descriptor[i], 0.2);
			}
		}
	}

	private double compareDescriptors(double[] d1, double[] d2)
	{
		if (d1.length != d2.length)
		{
			return 1.0;
		}

		double dot = 0;
		double norm1 = 0;
		double norm2 = 0;
		for (int i = 0; i < d1.length; i++)
		{
			dot   += d1[i] * d2[i];
			norm1 += d1[i] * d1[i];
			norm2 += d2[i] * d2[i];
		}

		if (norm1 == 0 || norm2 == 0)
		{
			return 1.0;
		}

		double similarity = dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
		return 1.0 - similarity;
	}

	private double[] computeColorHistogram(BufferedImage img)
	{
		int width  = img.getWidth();
		int height = img.getHeight();
		double[] histogram = new double[COLOR_BINS * COLOR_BINS * COLOR_BINS];

		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				int rgb = img.getRGB(x, y);
				Color color = new Color(rgb);

				int r = color.getRed()   * COLOR_BINS / 256;
				int g = color.getGreen() * COLOR_BINS / 256;
				int b = color.getBlue()  * COLOR_BINS / 256;

				r = Math.min(r, COLOR_BINS - 1);
				g = Math.min(g, COLOR_BINS - 1);
				b = Math.min(b, COLOR_BINS - 1);

				int index = (r * COLOR_BINS * COLOR_BINS) + (g * COLOR_BINS) + b;
				histogram[index]++;
			}
		}

		double totalPixels = width * (double)height;
		for (int i = 0; i < histogram.length; i++)
		{
			histogram[i] /= totalPixels;
		}

		return histogram;
	}

	private double compareColorHistograms(double[] h1, double[] h2)
	{
		if (h1.length != h2.length)
		{
			return 1.0;
		}

		double sumMin = 0;
		for (int i = 0; i < h1.length; i++)
		{
			sumMin += Math.min(h1[i], h2[i]);
		}
		return 1.0 - sumMin;
	}
	private double[] computeHueHistogram(BufferedImage img)
	{
		int width  = img.getWidth();
		int height = img.getHeight();
		double[] histogram = new double[HUE_BINS];

		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				int rgb = img.getRGB(x, y);
				float[] hsb = Color.RGBtoHSB(
					(rgb >> 16) & 0xFF,
					(rgb >>  8) & 0xFF,
					(rgb      ) & 0xFF,
					null
				);
				int bin = (int) (hsb[0] * HUE_BINS);
				if (bin >= HUE_BINS) {
					bin = HUE_BINS - 1;
				}
				histogram[bin]++;
			}
		}

		double totalPixels = width * (double)height;
		for (int i = 0; i < HUE_BINS; i++)
		{
			histogram[i] /= totalPixels;
		}

		return histogram;
	}

	private double compareHueHistograms(double[] h1, double[] h2)
	{
		if (h1.length != h2.length)
		{
			return 1.0;
		}

		double sumSq = 0.0;
		for (int i = 0; i < h1.length; i++)
		{
			double diff = (h1[i] - h2[i]);
			sumSq += diff * diff;
		}
		return sumSq;
	}
}

package com.advancedraidtracker.ui.nylostalls;

import com.advancedraidtracker.AdvancedRaidTrackerConfig;
import com.advancedraidtracker.utility.UISwingUtility;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ViewNyloStallsPanel extends JPanel
{
	private final List<Integer> data;
	private final AdvancedRaidTrackerConfig config;
	private final BufferedImage img;
	private final int raidCount;
	private int highestCount;
	private List<Integer> stalls;
	int width = 800;
	int height = 450;
	ViewNyloStallsPanel(List<Integer> data, int raids, AdvancedRaidTrackerConfig config)
	{
		this.data = data;
		this.config = config;
		raidCount = raids;
		setHighestCount();
		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		drawPanel();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		if (img != null)
		{
			g.drawImage(img, 0, 0, null);
		}
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(img.getWidth(), img.getHeight());
	}

	private void setHighestCount()
	{
		List<Integer> stallCounts = new ArrayList<>();
		for(int i = 0; i < 32; i++)
		{
			stallCounts.add(0);
		}
		for(Integer i : data)
		{
			if(i > 0 && i < 32)
			{
				stallCounts.set(i, stallCounts.get(i)+1);
			}
		}
		int max = 0;
		for(int i = 0; i < 32; i++)
		{
			if(stallCounts.get(i) > max)
			{
				max = stallCounts.get(i);
			}
			log.info("Wave " + i + " has " + stallCounts.get(i) + " stalls");
		}
		highestCount = max;
		stalls = stallCounts;
		log.info("Highest Count :" + highestCount);
	}

	private void drawPanel()
	{
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setColor(config.primaryDark());
		g.fillRect(0, 0, width, height);

		drawGraph();
	}

	private void drawGraph()
	{
		Graphics2D g = (Graphics2D) img.getGraphics();

		String title = "Stalls Per Wave (" + raidCount + " raids)";
		int titleWidth = UISwingUtility.getStringWidth(g, title);

		int graphMargin = 40;

		g.setColor(config.fontColor());
		g.drawString(title, (width/2)-(titleWidth/2), graphMargin/2);

		g.setColor(config.boxColor());
		g.drawRect(graphMargin, graphMargin, width-(graphMargin*2), height-(graphMargin*2));
		g.setColor(config.primaryMiddle());
		g.fillRect(graphMargin+1, graphMargin+1, width-(graphMargin*2)-2, height-(graphMargin*2)-2);

		int graphWidth = width-graphMargin*2;
		int graphHeight = height-graphMargin*2;

		double xWidth = graphWidth/31.0; //31 waves

		double yWidth = (double) graphHeight /highestCount;

		g.setColor(config.primaryLight());
		for(int i = 0; i < 31; i++)
		{
			g.drawLine((int)(graphMargin + (xWidth) + i*xWidth)-1, graphMargin+1, (int)(graphMargin + (xWidth) + i*xWidth)-1, height-graphMargin-2);
		}
		for(int i = 0; i < highestCount; i++)
		{
			if(highestCount > 25)
			{
				if(i% ((highestCount < 100) ? 5 : (((int)Math.log10(highestCount))*10)) !=0)
				{
					continue;
				}
			}
			g.drawLine(graphMargin+1, (int)(height-graphMargin-((i+1)*yWidth)-1), width-graphMargin-2, (int)(height-graphMargin-((i+1)*yWidth)-1));
		}

		g.setColor(config.fontColor());

		for(int i = 1; i < 32; i++)
		{
			String value = String.valueOf(i);
			int numberWidth = UISwingUtility.getStringWidth(g, value);
			g.drawString(value, graphMargin + (int)((i-1)*xWidth+xWidth/2-numberWidth/2), height-20-UISwingUtility.getStringHeight(g)/2);
		}

		for(int i = 1; i < highestCount+1; i++)
		{
			if(highestCount > 25)
			{
				if(i% ((highestCount < 100) ? 5 : (((int)Math.log10(highestCount))*10)) !=0)
				{
					continue;
				}
			}
			String value = String.valueOf(i);
			int numberWidth = UISwingUtility.getStringWidth(g, value);
			int numberHeight = UISwingUtility.getStringHeight(g);
			g.drawString(value, graphMargin-10-numberWidth, (int)(height-graphMargin-(i-1)*yWidth-yWidth/2+(numberHeight/2)));
		}

		for(int i = 1; i < 32; i++)
		{
			if(stalls.get(i) > 0)
			{
				int x1 = (int)(graphMargin+i*xWidth)+1;
				int x2 = (int)(graphMargin + (xWidth) + i*xWidth)-2;
				int y1 = height-graphMargin-1;
				int y2 = (int) (height-graphMargin-(yWidth*stalls.get(i)));
				drawBar(x1, x2, y1, y2);
			}
		}

	}

	private void drawBar(int x1, int x2, int y1, int y2)
	{
		Graphics2D g = (Graphics2D) img.getGraphics();
		int barWidth = x2-x1;
		int barHeight = y1-y2;

		Color baseStart = config.markerColor().darker().darker();
		Color baseEnd = config.markerColor();

		Color gradientStart = new Color(baseStart.getRed(), baseStart.getGreen(), baseStart.getBlue(), 90);
		Color gradientEnd = new Color(baseEnd.getRed(), baseEnd.getGreen(), baseEnd.getBlue(), 90);

		g.setColor(config.markerColor());

		g.drawRect(x1, y2, barWidth, barHeight);
		GradientPaint gradient = new GradientPaint(x1, y1, gradientStart, x1, y2, gradientEnd);

		g.setPaint(gradient);

		g.fillRect(x1, y2, barWidth, barHeight);
	}
}

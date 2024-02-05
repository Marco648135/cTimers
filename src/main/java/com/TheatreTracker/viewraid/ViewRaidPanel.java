package com.TheatreTracker.viewraid;

import com.TheatreTracker.RoomData;
import com.TheatreTracker.utility.RoomUtil;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.FontTypeFace;
import net.runelite.client.config.FontType;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
@Slf4j
public class ViewRaidPanel extends JPanel implements MouseListener, MouseMotionListener
{
    Color primaryLight = new Color(83, 74, 62);
    Color primaryDark = new Color(62,53,41);

    Color primaryMiddle = new Color(73, 64, 52);

    Color primaryDarkest = new Color(50, 40, 30);
    Color primaryText = new Color(255, 152, 31);

    Color darkOutline = new Color(14, 14, 12);
    Color lightOutline = new Color(71, 71, 69);
    int hoveredRoom = -1;
    int selectedRoom = 0;

    int offsetY = 67;
    int offsetX = 10;


    Color[] alternating = new Color[] {primaryLight, primaryDark};

    private BufferedImage img;

    private RoomData roomData;

    public ViewRaidPanel(RoomData roomData)
    {
        this.roomData = roomData;
        img = new BufferedImage(900, 733, BufferedImage.TYPE_INT_ARGB);
        drawPanel();
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if(img != null)
        {
            g.drawImage(img, 0, 0, null);
        }
    }

    String[] roomNames = new String[] {"Challenge", "Maiden", "Bloat", "Nylocas", "Sotetseg", "Xarpus", "Verzik"};

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(img.getWidth(), img.getHeight());
    }

    private void drawShadowedString(Graphics2D g, String string, int x, int y)
    {
        Color oldColor = g.getColor();
        g.setColor(Color.BLACK);
        g.drawString(string, x+2, y+2);
        g.setColor(oldColor);
        g.drawString(string, x, y);
    }

    private Rectangle getStringBounds(Graphics2D g2, String str,
                                      float x, float y)
    {
        FontRenderContext frc = g2.getFontRenderContext();
        GlyphVector gv = g2.getFont().createGlyphVector(frc, str);
        return gv.getPixelBounds(null, x, y);
    }

    private void drawTwoLevelBorder(Graphics2D g, int x, int y, int width, int height)
    {
        g.setColor(darkOutline);
        g.drawRect(x, y, width, height);
        g.setColor(lightOutline);
        g.drawRect(x+1, y + 2, width-2, height-3);
        g.setColor(primaryDarkest);
        g.fillRect(x+2, y+3, width-4, height-5);
    }

    private void drawPanel()
    {
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        Color oldColor = g.getColor();

        g.setColor(primaryMiddle);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setFont(FontManager.getRunescapeBoldFont().deriveFont(36.0f));

        g.setColor(primaryText);
        drawShadowedString(g,"Performance Details", 300, 40);

        drawTwoLevelBorder(g, offsetX+1, offsetY+1, 300, 633);

        g.setFont(FontManager.getRunescapeFont().deriveFont(24.0f));
        int fontHeight;
        for(int i = 0; i < roomNames.length; i++)
        {
            g.setColor(alternating[(i + 1) % 2]);
            g.fillRect(offsetX+3, offsetY + (90*i)+2, 297, 90);

            g.setFont(FontManager.getRunescapeFont().deriveFont(24.0f));
            fontHeight = getStringBounds(g, "a", 0, 0).height;
            g.setColor(primaryText);
            drawShadowedString(g, roomNames[i], offsetX+13, offsetY+(90*i)+2+45+(fontHeight/2));

            g.setFont(FontManager.getRunescapeFont().deriveFont(20.0f));
            fontHeight = getStringBounds(g, "a", 0, 0).height;
            g.setColor(Color.WHITE);
            drawShadowedString(g, RoomUtil.time(roomData.getValue(roomNames[i] + " Time")), offsetX+200, offsetY+(90*i)+2+45+(fontHeight/2));

            if(i == selectedRoom)
            {
                g.setColor(Color.WHITE.darker());
                g.drawRect(offsetX+2, offsetY+(90*i)+1, 297, 89);
            }

            if(i == hoveredRoom)
            {
                g.setColor(Color.WHITE);
                g.drawRect(offsetX+2, offsetY+(90*i)+1, 297, 89);
            }

            drawTwoLevelBorder(g, offsetX + 300 + 50, offsetY, 530, 633);

            g.setFont(FontManager.getRunescapeBoldFont().deriveFont(36.0f));
            g.setColor(primaryText);
            drawShadowedString(g, roomNames[selectedRoom]+ " summary", 360+265-
                    (getStringBounds(g, roomNames[selectedRoom] + " summary", 0, 0).width)/2, offsetY+50);

        }

        g.setColor(oldColor);
        g.dispose();
        repaint();
    }

    private void setSelectedRoom(int x, int y)
    {
        if(x > 10 && x < 310 && y > 67 && y < (67+(90*7)))
        {
            int yPos = y-67;
            int yBox = yPos/90;
            selectedRoom = yBox;
        }
    }

    private void setHoveredRoom(int x, int y)
    {
        if(x > 10 && x < 310 && y > 67 && y < (67+(90*7)))
        {
            int yPos = y-67;
            int yBox = yPos/90;
            hoveredRoom = yBox;
        }
        else
        {
            hoveredRoom = -1;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        setSelectedRoom(e.getX(), e.getY());
        drawPanel();
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        setHoveredRoom(e.getX(), e.getY());
        drawPanel();
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        setHoveredRoom(e.getX(), e.getY());
        drawPanel();
    }
}
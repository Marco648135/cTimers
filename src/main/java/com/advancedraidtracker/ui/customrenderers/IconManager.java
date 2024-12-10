package com.advancedraidtracker.ui.customrenderers;

import com.advancedraidtracker.AdvancedRaidTrackerPlugin;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.*;

public class IconManager
{
    static ImageIcon outline;
    static ImageIcon filled;
	static ImageIcon save;
	static ImageIcon add;
	static ImageIcon refresh;
	static ImageIcon delete;
	static ImageIcon ischyros;
	static ImageIcon toxobolos;
	static ImageIcon hagios;
    public static ImageIcon getHeartFilled()
    {
        if(filled == null)
        {
            filled = new ImageIcon(ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/heartfilled.png").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        }
        return filled;
    }

    public static ImageIcon getHeartOutline()
    {
        if(outline == null)
        {
            outline = new ImageIcon(ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/heartoutline.png").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        }
        return outline;
    }

	public static ImageIcon getSaveIcon()
	{
		if(save == null)
		{
			save = new ImageIcon(ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/save.png").getScaledInstance(24, 24, Image.SCALE_SMOOTH));
		}
		return save;
	}

	public static ImageIcon getAddIcon()
	{
		if(add == null)
		{
			add = new ImageIcon(ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/add.png").getScaledInstance(24, 24, Image.SCALE_SMOOTH));
		}
		return add;
	}

	public static ImageIcon getDeleteIcon()
	{
		if(delete == null)
		{
			delete = new ImageIcon(ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/delete.png").getScaledInstance(24, 24, Image.SCALE_SMOOTH));
		}
		return delete;
	}

	public static ImageIcon getRefreshIcon()
	{
		if(refresh == null)
		{
			refresh = new ImageIcon(ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/refresh.png").getScaledInstance(24, 24, Image.SCALE_SMOOTH));
		}
		return refresh;
	}

	public static ImageIcon getHagios()
	{
		if(hagios == null)
		{
			hagios = new ImageIcon(ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/hagios.png").getScaledInstance(64, 64, Image.SCALE_SMOOTH));
		}
		return hagios;
	}

	public static ImageIcon getToxobolos()
	{
		if(toxobolos == null)
		{
			toxobolos = new ImageIcon(ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/toxobolos.png").getScaledInstance(64, 64, Image.SCALE_SMOOTH));
		}
		return toxobolos;
	}

	public static ImageIcon getIschyros()
	{
		if(ischyros == null)
		{
			ischyros = new ImageIcon(ImageUtil.loadImageResource(AdvancedRaidTrackerPlugin.class, "/com/advancedraidtracker/ischyros.png").getScaledInstance(64, 64, Image.SCALE_SMOOTH));
		}
		return ischyros;
	}
}

package com.advancedraidtracker.ui.setups;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

public class SuggestionListRenderer extends JPanel implements ListCellRenderer<Trie.Entry>
{
	private JLabel iconLabel = new JLabel();
	private JLabel nameLabel = new JLabel();
	private final ItemManager itemManager;

	public SuggestionListRenderer(ItemManager itemManager)
	{
		setLayout(new BorderLayout(5, 0));
		add(iconLabel, BorderLayout.WEST);
		add(nameLabel, BorderLayout.CENTER);
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		this.itemManager = itemManager;
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends Trie.Entry> list, Trie.Entry value, int index,
												  boolean isSelected, boolean cellHasFocus)
	{
		nameLabel.setText(value.getName());

		if (isSelected)
		{
			setBackground(list.getSelectionBackground());
			nameLabel.setForeground(list.getSelectionForeground());
		}
		else
		{
			setBackground(list.getBackground());
			nameLabel.setForeground(list.getForeground());
		}

		int itemId = value.getId();
		AsyncBufferedImage itemImage = itemManager.getImage(itemId);
		itemImage.addTo(iconLabel);

		return this;
	}
}

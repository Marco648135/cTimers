package com.advancedraidtracker.ui.dpsanalysis;

import com.advancedraidtracker.ui.BaseFrame;
import static com.advancedraidtracker.ui.charts.ChartIO.gson;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.clientThread;
import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.*;
import java.awt.event.*;
import java.awt.*;
import lombok.extern.slf4j.Slf4j;
import javax.swing.event.DocumentListener;
import net.runelite.api.SpriteID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.AsyncBufferedImage;

@Slf4j
public class DPSWindow extends BaseFrame
{
	private ItemManager itemManager;
	private SpriteManager spriteManager;
	List<String> allNPCs = new ArrayList<>();
	List<NPCData> allNPCData = new ArrayList<>();
	private Map<String, List<EquipmentData>> equipmentMap = new HashMap<>();

	private Map<String, Set<String>> trigramIndex = new HashMap<>();

	private Map<String, EquipmentData> selectedEquipment = new HashMap<>();

	// Labels for cumulative bonuses
	// Attack Bonus labels
	private JLabel attackStabLabel;
	private JLabel attackSlashLabel;
	private JLabel attackCrushLabel;
	private JLabel attackMagicLabel;
	private JLabel attackRangeLabel;

	// Defense Bonus labels
	private JLabel defenseStabLabel;
	private JLabel defenseSlashLabel;
	private JLabel defenseCrushLabel;
	private JLabel defenseMagicLabel;
	private JLabel defenseRangeLabel;

	// Other Bonuses labels
	private JLabel meleeStrengthLabel;
	private JLabel rangedStrengthLabel;
	private JLabel magicDamageLabel;
	private JLabel prayerLabel;

	// Target-Specific labels
	private JLabel undeadLabel;
	private JLabel slayerLabel;

	private JLabel prayerAttackBonusLabel;
	private JLabel prayerStrengthBonusLabel;
	private JLabel prayerDefenseBonusLabel;
	private JLabel prayerMagicBonusLabel;
	private JLabel prayerRangeBonusLabel;
	private JLabel prayerMagicDamageBonusLabel;

	public void loadData() throws Exception
	{
		String url = "https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/refs/heads/main/cdn/json/monsters.json";
		Gson reader = gson.newBuilder().create();
		NPCData[] npcData = reader.fromJson(new InputStreamReader(new URL(url).openStream()), NPCData[].class);
		for(NPCData data : npcData)
		{
			String npcName = data.getName() + ", " + data.getVersion() + " (" + data.getLevel() + ")";
			allNPCs.add(npcName);
			allNPCData.add(data);
		}
		buildNPCTrigramIndex();

		String url2 = "https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/refs/heads/main/cdn/json/equipment.json";
		reader = gson.newBuilder().create();
		EquipmentData[] equipmentData = reader.fromJson(new InputStreamReader(new URL(url2).openStream()), EquipmentData[].class);
		// Initialize the equipmentMap
		equipmentMap = new HashMap<>();
		for (EquipmentData data : equipmentData) {
			String slot = data.getSlot();
			if (slot == null || slot.isEmpty()) {
				continue; // Skip equipment without a slot or handle accordingly
			}
			// Get or create the list for this slot
			equipmentMap.computeIfAbsent(slot, k -> new ArrayList<>()).add(data);
		}
	}

	List<String> equipmentSlots = Arrays.asList(
		"head", "cape", "neck", "ammo", "weapon",
		"body", "shield", "legs", "hands", "feet", "ring"
	);

	private NPCData getNPCFromName(String name)
	{
		for(NPCData npc : allNPCData)
		{
			if(name.equals(npc.getName() + ", " + npc.getVersion() + " (" + npc.getLevel() + ")"))
			{
				return npc;
			}
		}
		return null;
	}

	private void buildNPCTrigramIndex() {
		for (String npcName : allNPCs) {
			String normalizedName = npcName.toLowerCase().replaceAll("\\s+", "");
			Set<String> trigrams = getTrigrams(normalizedName);
			for (String trigram : trigrams) {
				trigramIndex.computeIfAbsent(trigram, k -> new HashSet<>()).add(npcName);
			}
		}
	}

	private JPanel createPrayerSummaryPanel()
	{
		JPanel summaryPanel = new JPanel();
		summaryPanel.setLayout(new GridLayout(0, 2, 5, 5));
		summaryPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));

		prayerAttackBonusLabel = new JLabel("Attack Bonus: 0%");
		prayerStrengthBonusLabel = new JLabel("Strength Bonus: 0%");
		prayerDefenseBonusLabel = new JLabel("Defense Bonus: 0%");
		prayerMagicBonusLabel = new JLabel("Magic Bonus: 0%");
		prayerRangeBonusLabel = new JLabel("Ranged Bonus: 0%");
		prayerMagicDamageBonusLabel = new JLabel("Magic Damage Bonus: 0%");

		summaryPanel.add(prayerAttackBonusLabel);
		summaryPanel.add(prayerStrengthBonusLabel);
		summaryPanel.add(prayerDefenseBonusLabel);
		summaryPanel.add(prayerMagicBonusLabel);
		summaryPanel.add(prayerRangeBonusLabel);
		summaryPanel.add(prayerMagicDamageBonusLabel);

		return summaryPanel;
	}

	private void updatePrayerSummary()
	{
		int totalAttackBonus = 0;
		int totalStrengthBonus = 0;
		int totalDefenseBonus = 0;
		int totalMagicBonus = 0;
		int totalRangeBonus = 0;
		int totalMagicDamageBonus = 0;

		for (Map.Entry<Prayers, Boolean> entry : selectedPrayers.entrySet())
		{
			if (entry.getValue())
			{
				Prayers prayer = entry.getKey();
				totalAttackBonus += prayer.att;
				totalStrengthBonus += prayer.str;
				totalDefenseBonus += prayer.def;
				totalMagicBonus += prayer.mag;
				totalRangeBonus += prayer.ran;
				totalMagicDamageBonus += prayer.magD;
			}
		}

		prayerAttackBonusLabel.setText("Attack Bonus: " + totalAttackBonus + "%");
		prayerStrengthBonusLabel.setText("Strength Bonus: " + totalStrengthBonus + "%");
		prayerDefenseBonusLabel.setText("Defense Bonus: " + totalDefenseBonus + "%");
		prayerMagicBonusLabel.setText("Magic Bonus: " + totalMagicBonus + "%");
		prayerRangeBonusLabel.setText("Ranged Bonus: " + totalRangeBonus + "%");
		prayerMagicDamageBonusLabel.setText("Magic Damage Bonus: " + totalMagicDamageBonus + "%");
	}

	private Set<String> getTrigrams(String text) {
		Set<String> trigrams = new HashSet<>();
		int length = text.length();
		for (int i = 0; i < length - 2; i++) {
			String trigram = text.substring(i, i + 3);
			trigrams.add(trigram);
		}
		return trigrams;
	}

	private final Map<Prayers, BufferedImage> disabledPrayerIcons = new HashMap<>();
	private final Map<Prayers, BufferedImage> enabledPrayerIcons = new HashMap<>();

	DefaultTableModel tableModel;

	public DPSWindow(ItemManager itemManager, SpriteManager spriteManager, ClientThread clientThread)
	{
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;

		log.info("is null? " + (spriteManager == null));
		clientThread.invoke(()->
		{
			for(Prayers prayers : Prayers.values())
			{
				disabledPrayerIcons.put(prayers, spriteManager.getSprite(prayers.disabledID, 0));
				enabledPrayerIcons.put(prayers, spriteManager.getSprite(prayers.enabledID, 0));
				log.info("done: " + prayers.name);
				log.info("size: " + disabledPrayerIcons.size());
			}
		});


		try
		{
			loadData();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		setSize(new Dimension(800, 600));

		// Create the table model
		tableModel = new DefaultTableModel();

		// Create the JTable
		JTable table = new JTable(tableModel);

		// Add initial columns
		tableModel.addColumn("Name");

		// Add two empty rows
		tableModel.addRow(new Object[]{"Defense"}); // Row index 1 - the permanent second row

		// Create a JScrollPane to hold the JTable
		JScrollPane scrollPane = new JScrollPane(table);

		// Create a panel to hold the table header and the '+' button
		JPanel headerPanel = new JPanel(new BorderLayout());

		// Get the table header
		JTableHeader tableHeader = table.getTableHeader();

		// Create the '+' button for adding columns
		JButton addColumnButton = new JButton("+");
		addColumnButton.setMargin(new Insets(0, 0, 0, 0));
		addColumnButton.setPreferredSize(new Dimension(30, 20));

		// Add the table header and the '+' button to the header panel
		headerPanel.add(tableHeader, BorderLayout.CENTER);
		headerPanel.add(addColumnButton, BorderLayout.EAST);

		// Create the '+' button for adding rows
		JButton addRowButton = new JButton("+");
		addRowButton.setMargin(new Insets(0, 0, 0, 0));
		addRowButton.setPreferredSize(new Dimension(30, 20));

		// Create a footer panel to hold the '+' button for rows
		JPanel footerPanel = new JPanel();
		footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.X_AXIS));
		footerPanel.add(addRowButton);
		footerPanel.add(Box.createHorizontalGlue());

		// Set the header panel as the column header view
		scrollPane.setColumnHeaderView(headerPanel);

		// Create the main panel and add components
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(headerPanel, BorderLayout.NORTH);
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		mainPanel.add(footerPanel, BorderLayout.SOUTH);

		// Add the main panel to the frame
		add(mainPanel);

		// Add action listeners to the '+' buttons
		addColumnButton.addActionListener(e -> showAddColumnInputBox(addColumnButton, tableModel));
		addRowButton.addActionListener(e -> showAddRowInputBox(addRowButton, table, tableModel));

		pack();
		open();
	}

	private Map<Prayers, JButton> prayerButtons = new HashMap<>();

	private void showAddColumnInputBox(JButton addColumnButton, DefaultTableModel tableModel)
	{
		JLayeredPane layeredPane = getLayeredPane();

		JPanel overlayPanel = new JPanel(null);
		overlayPanel.setOpaque(false);
		overlayPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());

		final JPanel inputPanel = new JPanel(new BorderLayout());
		inputPanel.setBackground(new Color(255, 255, 255, 230));
		inputPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		final int[] preferredWidth = {200};

		overlayPanel.add(inputPanel);

		overlayPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				// Check if click was outside inputPanel
				if (!inputPanel.getBounds().contains(e.getPoint()))
				{
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
			}
		});

		// Consume mouse events on the overlay to prevent them from reaching underlying components
		overlayPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				// Do nothing to consume the event
			}
		});

		JTextField textField = new JTextField();
		inputPanel.add(textField, BorderLayout.NORTH);

		textField.setFocusTraversalKeysEnabled(false);
		textField.setHorizontalAlignment(JTextField.RIGHT);

		DefaultListModel<String> listModel = new DefaultListModel<>();
		JList<String> suggestionList = new JList<>(listModel);
		suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane suggestionScrollPane = new JScrollPane(suggestionList);
		inputPanel.add(suggestionScrollPane, BorderLayout.CENTER);

		suggestionScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		suggestionScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		suggestionList.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
														  boolean isSelected, boolean cellHasFocus)
			{
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				String text = textField.getText();
				String suggestion = value.toString();

				String lowerSuggestion = suggestion.toLowerCase();
				String lowerText = text.toLowerCase();

				int matchIndex = lowerSuggestion.indexOf(lowerText);
				if (matchIndex != -1)
				{
					String beforeMatch = suggestion.substring(0, matchIndex);
					String matchText = suggestion.substring(matchIndex, matchIndex + text.length());
					String afterMatch = suggestion.substring(matchIndex + text.length());
					label.setText("<html>" + beforeMatch + "<b>" + matchText + "</b>" + afterMatch + "</html>");
				}
				else
				{
					label.setText(suggestion);
				}

				label.setHorizontalAlignment(SwingConstants.RIGHT);

				return label;
			}
		});

		// Position the input panel below the '+' button
		Point buttonLocation = SwingUtilities.convertPoint(addColumnButton, new Point(0, addColumnButton.getHeight()), layeredPane);
		int screenWidth = layeredPane.getWidth();
		int x = buttonLocation.x - preferredWidth[0] + addColumnButton.getWidth();

// Adjust x position if it goes off-screen to the left
		if (x < 0) {
			x = 0;
		}
// Adjust x position if it goes off-screen to the right
		if (x + preferredWidth[0] > screenWidth) {
			x = screenWidth - preferredWidth[0];
		}

		inputPanel.setBounds(x, buttonLocation.y, preferredWidth[0], 130);

		layeredPane.add(overlayPanel, JLayeredPane.POPUP_LAYER);
		layeredPane.revalidate();
		layeredPane.repaint();

		textField.requestFocusInWindow();

		textField.addKeyListener(new KeyAdapter() {
			private SwingWorker<Void, List<String>> worker;

			@Override
			public void keyReleased(KeyEvent e) {
				int keyCode = e.getKeyCode();
				if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
					return;
				}

				String text = textField.getText().toLowerCase().replaceAll("\\s+", "");
				if (text.length() < 3) {
					listModel.clear();
					return;
				}

				if (worker != null && !worker.isDone()) {
					worker.cancel(true);
				}
				worker = new SwingWorker<Void, List<String>>() {
					@Override
					protected Void doInBackground() throws Exception {
						try {
							// Generate trigrams from input text
							Set<String> inputTrigrams = getTrigrams(text);

							// Collect candidate NPC names based on trigrams
							Map<String, Integer> candidateScores = new HashMap<>();

							for (String trigram : inputTrigrams) {
								if (isCancelled()) return null;
								Set<String> matches = trigramIndex.get(trigram);
								if (matches != null) {
									for (String match : matches) {
										candidateScores.put(match, candidateScores.getOrDefault(match, 0) + 1);
									}
								}
							}

							// Convert candidateScores to a list and sort by score
							List<Map.Entry<String, Integer>> sortedCandidates = new ArrayList<>(candidateScores.entrySet());
							sortedCandidates.sort((a, b) -> b.getValue().compareTo(a.getValue()));

							// Collect the top suggestions
							List<String> suggestions = new ArrayList<>();
							for (Map.Entry<String, Integer> entry : sortedCandidates) {
								suggestions.add(entry.getKey());
								if (suggestions.size() >= 10) break;
							}

							publish(suggestions);
						} catch (Exception ex) {
							ex.printStackTrace();
							log.error("Error during trigram search", ex);
						}
						return null;
					}

					@Override
					protected void process(List<List<String>> chunks) {
						listModel.clear();
						List<String> suggestions = chunks.get(chunks.size() - 1);
						int maxWidth = 0;
						FontMetrics metrics = suggestionList.getFontMetrics(suggestionList.getFont());

						// Height calculations
						int rowHeight = suggestionList.getFixedCellHeight();
						if (rowHeight == -1) {
							rowHeight = suggestionList.getFontMetrics(suggestionList.getFont()).getHeight();
						}
						int totalHeight = 0;

						for (String suggestion : suggestions) {
							listModel.addElement(suggestion);
							// Calculate the width of the suggestion
							int width = SwingUtilities.computeStringWidth(metrics, suggestion);
							if (width > maxWidth) {
								maxWidth = width;
							}
						}

						// Calculate total height required
						int visibleRowCount = suggestions.size();
						totalHeight = visibleRowCount * rowHeight + 5; // Add a small margin

						// Set maximum height if necessary
						int maxPopupHeight = 300;
						if (totalHeight > maxPopupHeight) {
							totalHeight = maxPopupHeight;
							suggestionList.setVisibleRowCount(maxPopupHeight / rowHeight);
						} else {
							suggestionList.setVisibleRowCount(visibleRowCount);
						}

						// Adjust width settings (same as before)
						int scrollbarWidth = suggestionScrollPane.getVerticalScrollBar().isVisible() ? suggestionScrollPane.getVerticalScrollBar().getWidth() : 17;
						preferredWidth[0] = maxWidth + scrollbarWidth + 20; // 20 pixels for padding
						if (preferredWidth[0] < 200) preferredWidth[0] = 200; // Minimum width
						if (preferredWidth[0] > screenWidth) preferredWidth[0] = screenWidth - 20; // Maximum width

						// Update the sizes
						inputPanel.setPreferredSize(new Dimension(preferredWidth[0], totalHeight + textField.getHeight()));
						suggestionScrollPane.setPreferredSize(new Dimension(preferredWidth[0], totalHeight));
						inputPanel.revalidate();
						inputPanel.repaint();

						// Reposition the inputPanel if necessary
						int x = buttonLocation.x - preferredWidth[0] + addColumnButton.getWidth();
						if (x < 0) {
							x = 0;
						}
						if (x + preferredWidth[0] > screenWidth) {
							x = screenWidth - preferredWidth[0];
						}
						inputPanel.setBounds(x, buttonLocation.y, preferredWidth[0], totalHeight + textField.getHeight());

						if (listModel.getSize() > 0) {
							suggestionList.setSelectedIndex(0);
						}
					}
				};
				worker.execute();
			}

			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					String selectedOption;
					if (!suggestionList.isSelectionEmpty())
					{
						selectedOption = suggestionList.getSelectedValue();
					}
					else
					{
						selectedOption = textField.getText();
					}
					tableModel.addColumn(selectedOption);

// Get the NPCData for this NPC
					NPCData npcData = getNPCFromName(selectedOption);
					if (npcData != null) {
						// Find the index of the new column
						int columnIndex = tableModel.getColumnCount() - 1;

						// Store the NPCData in the map
						columnToNPCDataMap.put(columnIndex, npcData);

						// Ensure the defense row exists
						if (tableModel.getRowCount() < 2) {
							// Add the defense row if it doesn't exist
							Object[] defenseRow = new Object[tableModel.getColumnCount()];
							defenseRow[0] = "Defense Level";
							//tableModel.insertRow(0, defenseRow);
						} else if (tableModel.getValueAt(0, 0) == null) {
							tableModel.setValueAt("Defense Level", 0, 0);
						}

						// Set the defense value in the defense row (row index 1)
						int defenseValue = npcData.getSkills().getDef();
						tableModel.setValueAt(defenseValue, 0, columnIndex);

						// Compute cell values for existing preset rows
						for (int rowIndex = 1; rowIndex < tableModel.getRowCount(); rowIndex++) {
							String presetName = rowToPresetMap.get(rowIndex);
							if (presetName != null) {
								Preset preset = presets.get(presetName);
								if (preset != null) {
									String cellValue = computeCellValue(preset, npcData, defenseValue);
									tableModel.setValueAt(cellValue, rowIndex, columnIndex);
								}
							}
						}
					}

					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
				else if (e.getKeyCode() == KeyEvent.VK_TAB)
				{
					if (!suggestionList.isSelectionEmpty())
					{
						textField.setText(suggestionList.getSelectedValue());
						listModel.clear();
					}
					e.consume();
				}
				else if (e.getKeyCode() == KeyEvent.VK_DOWN)
				{
					int index = suggestionList.getSelectedIndex();
					if (index < listModel.getSize() - 1)
					{
						suggestionList.setSelectedIndex(index + 1);
					}
					e.consume();
				}
				else if (e.getKeyCode() == KeyEvent.VK_UP)
				{
					int index = suggestionList.getSelectedIndex();
					if (index > 0)
					{
						suggestionList.setSelectedIndex(index - 1);
					}
					e.consume();
				}
			}
		});
	}

	private String computeCellValue(Preset preset, NPCData npcData, int defenseValue)
	{
		int effectiveStr = (int)((118.0)*(1.23));
		effectiveStr += 3;
		effectiveStr += 8;

		int strBonus = 0;
		int attBonus = 0;
		for(EquipmentData data : preset.getEquipment().values())
		{
			strBonus += data.getBonuses().getStr();
			attBonus += data.getOffensive().getSlash();
		}
		int maxHit = effectiveStr * (strBonus+64);
		maxHit += 320;
		maxHit = (int) (maxHit/640.0);

		int effectiveAttack = (int)((118.0*1.2));
		effectiveAttack += 8;

		int attackRoll = effectiveAttack * (attBonus+64);

		int defenseRoll = (defenseValue+9)*(64+npcData.getDefensive().getSlash());

		double hitChance;
		if(attackRoll > defenseRoll)
		{
			hitChance = 1-((defenseRoll+2.0)/(2.0*attackRoll+1));
		}
		else
		{
			hitChance = (attackRoll*1.0)/(2*(defenseRoll+1.0));
		}

		double dpa = hitChance * ((double) maxHit /2+((double) 1 /(maxHit+1)));

		double dps = dpa/3.0;

		dps *= 100;

		int dpsFlat = (int)dps;

		String value = ""+dpsFlat/100.0;

		// Implement your computation logic here
		// For now, let's return a placeholder string
		return value;
	}

	private void openCreateFrameWithPreset(String presetName) {
		// Retrieve the Preset object from the presets map
		Preset preset = presets.get(presetName);
		if (preset == null) {
			JOptionPane.showMessageDialog(this, "Preset not found: " + presetName);
			return;
		}

		// Extract equipment, prayers, and selected style from the preset
		Map<String, EquipmentData> presetEquipment = preset.getEquipment();
		Map<Prayers, Boolean> presetPrayers = preset.getPrayers();
		selectedStyle = preset.getSelectedStyle();

		// Update the selectedEquipment and selectedPrayers with the preset data
		selectedEquipment = new HashMap<>(presetEquipment);
		selectedPrayers = new HashMap<>(presetPrayers);

		// Initialize createFrame
		createFrame = new JFrame("Edit Preset - " + presetName);
		createFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Main panel with vertical layout
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		// Panels for equipment, prayer, and style
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));

		// Pass the presetEquipment to createEquipmentPanel
		JPanel equipmentPanel = createEquipmentPanel(presetEquipment);
		JPanel prayerPanel = createPrayerPanel(); // Uses selectedPrayers internally
		JPanel stylePanel = createStylePanel(); // Now uses selectedStyle

		// Add panels to contentPanel
		contentPanel.add(equipmentPanel);
		contentPanel.add(Box.createHorizontalStrut(10));
		contentPanel.add(prayerPanel);
		contentPanel.add(Box.createHorizontalStrut(10));
		contentPanel.add(stylePanel);

		// Add contentPanel to mainPanel
		mainPanel.add(contentPanel, BorderLayout.CENTER);

		// Create the save preset panel at the bottom
		JPanel savePresetPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		// Preset name field (non-editable)
		JTextField presetNameField = new JTextField(presetName, 20);
		presetNameField.setEditable(false);
		JButton savePresetButton = new JButton("Save Changes");

		savePresetPanel.add(new JLabel("Preset Name:"));
		savePresetPanel.add(presetNameField);
		savePresetPanel.add(savePresetButton);

		// Add savePresetPanel to mainPanel
		mainPanel.add(savePresetPanel, BorderLayout.SOUTH);

		// Add mainPanel to frame
		createFrame.add(mainPanel);

		createFrame.pack();
		createFrame.setLocationRelativeTo(null);
		createFrame.setVisible(true);

		// Add ActionListener to the savePresetButton
		savePresetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				// Save the changes to the preset
				Map<String, EquipmentData> updatedEquipment = new HashMap<>(selectedEquipment);
				Map<Prayers, Boolean> updatedPrayers = new HashMap<>(selectedPrayers);
				String updatedSelectedStyle = selectedStyle;

				// Update the preset object with new data
				preset.setEquipment(updatedEquipment);
				preset.setPrayers(updatedPrayers);
				preset.setSelectedStyle(updatedSelectedStyle);

				JOptionPane.showMessageDialog(createFrame, "Preset '" + presetName + "' updated successfully.");
				createFrame.dispose();
			}
		});

		// After the UI is set up, call updatePrayerSummary() to refresh the summary panel
		updatePrayerSummary();
	}

	private JPanel createPresetPanel(String presetName, DefaultTableModel tableModel, JLayeredPane layeredPane, JPanel inputPanel) {
		JPanel presetPanel = new JPanel(new BorderLayout());

		presetPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35)); // Set max height

		// Preset name label
		JLabel nameLabel = new JLabel(presetName);
		nameLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Buttons panel
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));

		// "View" button
		JButton viewButton = new JButton("View");

		// "Select" button
		JButton selectButton = new JButton("Select");

		// Add buttons to buttonsPanel
		buttonsPanel.add(viewButton);
		buttonsPanel.add(selectButton);

		// Add components to presetPanel
		presetPanel.add(nameLabel, BorderLayout.CENTER);
		presetPanel.add(buttonsPanel, BorderLayout.EAST);

		// Action listener for "View" button
		viewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Close the input panel
				layeredPane.remove(overlayPanel);
				layeredPane.repaint();

				// Open the createFrame with the preset data
				openCreateFrameWithPreset(presetName);
			}
		});

		// Action listener for "Select" button
		selectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Add the preset to the table
				addPresetRowToTable(presetName, tableModel);
				layeredPane.remove(inputPanel);
				layeredPane.repaint();
			}
		});

		return presetPanel;
	}

	private JPanel overlayPanel;

	private void showAddRowInputBox(JButton addRowButton, JTable table, DefaultTableModel tableModel)
	{

		JLayeredPane layeredPane = getLayeredPane();

		// Prepare the overlay panel
		overlayPanel = new JPanel(null);
		overlayPanel.setOpaque(false);
		overlayPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());

		// Create the inputPanel
		JPanel inputPanel = new JPanel(new BorderLayout());
		inputPanel.setBackground(new Color(255, 255, 255, 230));
		inputPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));


		// Panel to hold presets
		JPanel presetsPanel = new JPanel();
		presetsPanel.setLayout(new BoxLayout(presetsPanel, BoxLayout.Y_AXIS));

		// Scroll pane for presetsPanel
		JScrollPane scrollPane = new JScrollPane(presetsPanel);
		scrollPane.setPreferredSize(new Dimension(250, 200));

		// Populate the presetsPanel with preset entries
		for (String presetName : presets.keySet())
		{
			JPanel presetPanel = createPresetPanel(presetName, tableModel, layeredPane, inputPanel);
			presetsPanel.add(presetPanel);
		}

		// Create the "Create..." button
		JButton createButton = new JButton("Create...");
		createButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		// Add the "Create..." button below the list
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		buttonPanel.add(Box.createVerticalStrut(5)); // Add some spacing
		buttonPanel.add(createButton);
		buttonPanel.add(Box.createVerticalStrut(5)); // Add some spacing

		inputPanel.add(scrollPane, BorderLayout.CENTER);
		inputPanel.add(buttonPanel, BorderLayout.SOUTH);

		// Position the input panel above the '+' button
		Point buttonLocation = SwingUtilities.convertPoint(addRowButton, new Point(0, -250), layeredPane);
		int inputPanelWidth = 250;
		int x = buttonLocation.x;

		// Adjust x position if it goes off-screen
		if (x + inputPanelWidth > layeredPane.getWidth())
		{
			x = layeredPane.getWidth() - inputPanelWidth;
		}
		if (x < 0)
		{
			x = 0;
		}

		inputPanel.setBounds(x, buttonLocation.y, inputPanelWidth, 250); // Adjusted height

		overlayPanel.add(inputPanel);

		layeredPane.add(overlayPanel, JLayeredPane.POPUP_LAYER);
		layeredPane.revalidate();
		layeredPane.repaint();

		// Action listener for "Create..." button
		createButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				layeredPane.remove(overlayPanel);
				layeredPane.repaint();
				openCreateFrame();
			}
		});

		// Event handling for preset selection
		// Modify the code to pass overlayPanel and layeredPane when creating preset panels
		presetsPanel.removeAll();
		for (String presetName : presets.keySet())
		{
			JPanel presetPanel = createPresetPanel(presetName, tableModel, layeredPane, overlayPanel);
			presetsPanel.add(presetPanel);
		}

		// If there are no presets available, show a message
		if (presets.isEmpty())
		{
			JLabel noPresetsLabel = new JLabel("No presets available. Please create one.");
			noPresetsLabel.setHorizontalAlignment(SwingConstants.CENTER);
			inputPanel.removeAll();
			inputPanel.add(noPresetsLabel, BorderLayout.CENTER);
			inputPanel.add(buttonPanel, BorderLayout.SOUTH); // Add the "Create..." button
		}

		// Add a mouse listener to the overlayPanel to detect clicks outside the inputPanel
		overlayPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				// Check if click was outside inputPanel
				if (!inputPanel.getBounds().contains(e.getPoint()))
				{
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
			}
		});

		// Consume mouse events on the overlay to prevent them from reaching underlying components
		overlayPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				// Do nothing to consume the event
			}
		});
	}

	private void openCreateFrame() {
		selectedEquipment = new HashMap<>();
		selectedPrayers = new HashMap<>();
		selectedStyle = null; // Initialize selectedStyle

		// Initialize createFrame
		createFrame = new JFrame("Create New Preset");
		createFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Main panel with BorderLayout
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		// === New Top Panel with Base Template Selection ===
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		JLabel baseTemplateLabel = new JLabel("Base Template:");
		JComboBox<String> baseTemplateComboBox = new JComboBox<>();

		// Add "None" as the default option
		baseTemplateComboBox.addItem("None");

		// Populate the combo box with existing preset names
		for (String presetName : presets.keySet()) {
			baseTemplateComboBox.addItem(presetName);
		}

		topPanel.add(baseTemplateLabel);
		topPanel.add(baseTemplateComboBox);

		// Add action listener to update the create frame when a preset is selected
		baseTemplateComboBox.addActionListener(e -> {
			String selectedPresetName = (String) baseTemplateComboBox.getSelectedItem();
			if (selectedPresetName != null && !selectedPresetName.equals("None")) {
				// Load the selected preset as the base template
				loadPresetAsBaseTemplate(selectedPresetName);
			} else {
				// Clear the selections if "None" is selected
				selectedEquipment.clear();
				selectedPrayers.clear();
				selectedStyle = null;
				updateEquipmentPanel();
				updatePrayerPanel();
				updateStylePanel();
			}
		});

		// === Content Panels ===
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));

		// Create equipment, prayer, and style panels
		equipmentPanel = createEquipmentPanel(selectedEquipment);
		prayerPanel = createPrayerPanel();
		stylePanel = createStylePanel();

		// Add panels to contentPanel
		contentPanel.add(equipmentPanel);
		contentPanel.add(Box.createHorizontalStrut(10));
		contentPanel.add(prayerPanel);
		contentPanel.add(Box.createHorizontalStrut(10));
		contentPanel.add(stylePanel);

		// Save preset panel at the bottom
		JPanel savePresetPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JTextField presetNameField = new JTextField(20);
		JButton savePresetButton = new JButton("Save Preset");

		savePresetPanel.add(new JLabel("Preset Name:"));
		savePresetPanel.add(presetNameField);
		savePresetPanel.add(savePresetButton);

		// Add components to mainPanel
		mainPanel.add(topPanel, BorderLayout.NORTH);
		mainPanel.add(contentPanel, BorderLayout.CENTER);
		mainPanel.add(savePresetPanel, BorderLayout.SOUTH);

		// Add mainPanel to frame
		createFrame.add(mainPanel);

		createFrame.pack();
		createFrame.setLocationRelativeTo(null);
		createFrame.setVisible(true);

		// Action listener for savePresetButton
		savePresetButton.addActionListener(e -> {
			String presetName = presetNameField.getText().trim();
			if (presetName.isEmpty()) {
				JOptionPane.showMessageDialog(createFrame, "Please enter a preset name.");
				return;
			}
			if (presets.containsKey(presetName)) {
				JOptionPane.showMessageDialog(createFrame, "A preset with that name already exists.");
				return;
			}
			savePreset(presetName);
			JOptionPane.showMessageDialog(createFrame, "Preset '" + presetName + "' created successfully.");
			createFrame.dispose();
		});
	}

	private void loadPresetAsBaseTemplate(String presetName) {
		Preset basePreset = presets.get(presetName);
		if (basePreset == null) {
			JOptionPane.showMessageDialog(createFrame, "Preset not found: " + presetName);
			return;
		}

		// Copy the equipment, prayers, and style from the base preset
		selectedEquipment = new HashMap<>(basePreset.getEquipment());
		selectedPrayers = new HashMap<>(basePreset.getPrayers());
		selectedStyle = basePreset.getSelectedStyle();

		// Update the UI components to reflect the selected base template
		updateEquipmentPanel();
		updatePrayerPanel();
		updateStylePanel();
	}

	private void updateEquipmentPanel() {
		// For each slot, update the button icon/text
		for (String slotName : slotButtons.keySet()) {
			JButton slotButton = slotButtons.get(slotName);
			EquipmentData equipmentData = selectedEquipment.get(slotName);
			if (equipmentData != null) {
				// Update the button with the equipment's image
				updateButtonWithEquipment(slotButton, equipmentData);
			} else {
				// Clear the button or set it to default
				slotButton.setIcon(null);
				slotButton.setText("+");
				slotButton.setToolTipText(slotName);
			}
		}

		// Update the summary panel
		updateSummary();
	}

	private void updatePrayerPanel() {
		// For each prayer, update the button icon
		for (Prayers prayer : prayerButtons.keySet()) {
			JButton button = prayerButtons.get(prayer);
			boolean isEnabled = selectedPrayers.getOrDefault(prayer, false);
			updatePrayerButtonIcon(button, prayer, isEnabled);
		}

		// Update the prayer summary
		updatePrayerSummary();
	}

	private void updateStylePanel() {
		// Update the style buttons
		for (String styleName : styleButtons.keySet()) {
			JButton button = styleButtons.get(styleName);
			if (styleName.equals(selectedStyle)) {
				button.setBackground(Color.RED.darker().darker());
				button.setForeground(Color.WHITE);
			} else {
				button.setBackground(null);
				button.setForeground(Color.BLACK);
			}
		}
	}

	private Map<Integer, String> rowToPresetMap = new HashMap<>();

	private void addPresetRowToTable(String presetName, DefaultTableModel tableModel) {
		Preset preset = presets.get(presetName);
		if (preset == null) {
			JOptionPane.showMessageDialog(this, "Preset not found: " + presetName);
			return;
		}

		// Create a new row with the correct number of columns
		int columnCount = tableModel.getColumnCount();
		Object[] rowData = new Object[columnCount];

		// Set the preset name in the "Name" column (column index 0)
		rowData[0] = presetName;

		// Add the row to the table model
		tableModel.addRow(rowData);

		// Get the index of the newly added row
		int rowIndex = tableModel.getRowCount() - 1;

		// Store the preset associated with this row
		rowToPresetMap.put(rowIndex, presetName);

		// Compute cell values for existing NPC columns
		for (int colIndex = 1; colIndex < columnCount; colIndex++) {
			NPCData npcData = columnToNPCDataMap.get(colIndex);
			if (npcData != null) {
				int defenseValue = getDefenseValueForColumn(tableModel, colIndex);
				String cellValue = computeCellValue(preset, npcData, defenseValue);
				tableModel.setValueAt(cellValue, rowIndex, colIndex);
			}
		}
	}

	private int getDefenseValueForColumn(DefaultTableModel tableModel, int columnIndex) {
		// Assuming defense value is stored in row index 1
		Object defenseObj = tableModel.getValueAt(0, columnIndex);
		if (defenseObj instanceof Integer) {
			return (Integer) defenseObj;
		} else if (defenseObj instanceof String) {
			try {
				return Integer.parseInt((String) defenseObj);
			} catch (NumberFormatException e) {
				// Handle invalid number format
			}
		}
		// Return a default value or handle error
		return 0;
	}

	private void savePreset(String presetName) {
		// Create copies of the selected equipment and prayers
		Map<String, EquipmentData> presetEquipment = new HashMap<>(selectedEquipment);
		Map<Prayers, Boolean> presetPrayers = new HashMap<>(selectedPrayers);
		String presetSelectedStyle = selectedStyle; // Save the selected style

		// Create a new preset object
		Preset preset = new Preset(presetName, presetEquipment, presetPrayers, presetSelectedStyle);

		// Save the preset
		presets.put(presetName, preset);
	}

	Map<String, JButton> slotButtons = new HashMap<>();

	JPanel equipmentPanel;
	private JPanel createEquipmentPanel(Map<String, EquipmentData> presetEquipment) {
		equipmentPanel = new JPanel();
		equipmentPanel.setLayout(new BorderLayout());

		// Set border with padding
		equipmentPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));

		JLabel equipmentLabel = new JLabel("Equipment", SwingConstants.CENTER);
		equipmentLabel.setFont(new Font("Arial", Font.BOLD, 16));
		equipmentPanel.add(equipmentLabel, BorderLayout.NORTH);

		// Equipment grid panel
		JPanel equipmentGrid = new JPanel();
		equipmentGrid.setLayout(new GridBagLayout());
		equipmentPanel.add(equipmentGrid, BorderLayout.CENTER);

		// GridBagConstraints for layout
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5); // Spacing between buttons

		// List of equipment slots
		List<String> equipmentSlots = Arrays.asList(
			"head", "cape", "neck", "ammo", "weapon",
			"body", "shield", "legs", "hands", "feet", "ring"
		);
		int slotIndex = 0;

		// Clear the slotButtons map
		slotButtons.clear();

		// First row, 1 button centered
		gbc.gridy = 0;
		gbc.gridx = 1; // Centered in 3 columns
		JButton button1 = createSquareButton("+");
		String slot = equipmentSlots.get(slotIndex++);
		addEquipmentButtonListener(button1, slot);
		slotButtons.put(slot, button1);
		equipmentGrid.add(button1, gbc);

		// Second row, 3 buttons
		gbc.gridy = 1;
		for (int i = 0; i < 3; i++) {
			gbc.gridx = i;
			JButton button = createSquareButton("+");
			slot = equipmentSlots.get(slotIndex++);
			addEquipmentButtonListener(button, slot);
			slotButtons.put(slot, button);
			equipmentGrid.add(button, gbc);
		}

		// Third row, 3 buttons
		gbc.gridy = 2;
		for (int i = 0; i < 3; i++) {
			gbc.gridx = i;
			JButton button = createSquareButton("+");
			slot = equipmentSlots.get(slotIndex++);
			addEquipmentButtonListener(button, slot);
			slotButtons.put(slot, button);
			equipmentGrid.add(button, gbc);
		}

		// Fourth row, 1 button centered
		gbc.gridy = 3;
		gbc.gridx = 1;
		JButton button10 = createSquareButton("+");
		slot = equipmentSlots.get(slotIndex++);
		addEquipmentButtonListener(button10, slot);
		slotButtons.put(slot, button10);
		equipmentGrid.add(button10, gbc);

		// Fifth row, 3 buttons
		gbc.gridy = 4;
		for (int i = 0; i < 3; i++) {
			gbc.gridx = i;
			JButton button = createSquareButton("+");
			slot = equipmentSlots.get(slotIndex++);
			addEquipmentButtonListener(button, slot);
			slotButtons.put(slot, button);
			equipmentGrid.add(button, gbc);
		}

		// Create and add the summary panel
		JPanel summaryPanel = createSummaryPanel();
		equipmentPanel.add(summaryPanel, BorderLayout.SOUTH);

		// If presetEquipment is provided, update the buttons
		if (presetEquipment != null) {
			for (Map.Entry<String, EquipmentData> entry : presetEquipment.entrySet()) {
				String slotName = entry.getKey();
				EquipmentData equipmentData = entry.getValue();
				JButton slotButton = slotButtons.get(slotName);
				if (slotButton != null) {
					// Update selectedEquipment
					selectedEquipment.put(slotName, equipmentData);
					// Update the button with the equipment's image
					updateButtonWithEquipment(slotButton, equipmentData);
				}
			}
			// Update the summary panel
			updateSummary();
		}

		return equipmentPanel;
	}

	private void updateButtonWithEquipment(JButton button, EquipmentData equipmentData) {
		// Use itemManager to get the image
		int itemId = equipmentData.getId();
		AsyncBufferedImage itemImage = itemManager.getImage(itemId);

		// Clear the button's text since we'll use an image
		button.setText("");
		// Set the tooltip to the equipment name
		button.setToolTipText(equipmentData.getName());

		// Set a placeholder icon if desired
		button.setIcon(new ImageIcon()); // Empty icon for now

		// When the image is loaded, update the button's icon
		itemImage.onLoaded(() -> {
			SwingUtilities.invokeLater(() -> {
				ImageIcon icon = new ImageIcon(itemImage);
				// Resize the icon to fit the button
				int width = button.getWidth();
				int height = button.getHeight();
				if (width == 0 || height == 0) {
					// Fallback to default size if button size not available
					width = 60;
					height = 60;
				}
				Image image = icon.getImage();
				Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
				icon = new ImageIcon(scaledImage);
				button.setIcon(icon);
			});
		});
	}

	private JPanel createSummaryPanel() {
		JPanel summaryPanel = new JPanel();
		summaryPanel.setLayout(new BorderLayout());
		summaryPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));

		// Panels for categories
		JPanel categoriesPanel = new JPanel();
		categoriesPanel.setLayout(new BoxLayout(categoriesPanel, BoxLayout.Y_AXIS));

		// Attack Bonus
		JLabel attackBonusLabel = new JLabel("Attack Bonus", SwingConstants.CENTER);
		attackBonusLabel.setFont(new Font("Arial", Font.BOLD, 14));
		categoriesPanel.add(attackBonusLabel);
		JPanel attackPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		attackStabLabel = new JLabel("Stab: 0");
		attackSlashLabel = new JLabel("Slash: 0");
		attackCrushLabel = new JLabel("Crush: 0");
		attackMagicLabel = new JLabel("Magic: 0");
		attackRangeLabel = new JLabel("Range: 0");
		attackPanel.add(attackStabLabel);
		attackPanel.add(attackSlashLabel);
		attackPanel.add(attackCrushLabel);
		attackPanel.add(attackMagicLabel);
		attackPanel.add(attackRangeLabel);
		categoriesPanel.add(attackPanel);

		// Defense Bonus
		JLabel defenseBonusLabel = new JLabel("Defense Bonus", SwingConstants.CENTER);
		defenseBonusLabel.setFont(new Font("Arial", Font.BOLD, 14));
		categoriesPanel.add(defenseBonusLabel);
		JPanel defensePanel = new JPanel(new GridLayout(0, 2, 5, 5));
		defenseStabLabel = new JLabel("Stab: 0");
		defenseSlashLabel = new JLabel("Slash: 0");
		defenseCrushLabel = new JLabel("Crush: 0");
		defenseMagicLabel = new JLabel("Magic: 0");
		defenseRangeLabel = new JLabel("Range: 0");
		defensePanel.add(defenseStabLabel);
		defensePanel.add(defenseSlashLabel);
		defensePanel.add(defenseCrushLabel);
		defensePanel.add(defenseMagicLabel);
		defensePanel.add(defenseRangeLabel);
		categoriesPanel.add(defensePanel);

		// Other Bonuses
		JLabel otherBonusesLabel = new JLabel("Other Bonuses", SwingConstants.CENTER);
		otherBonusesLabel.setFont(new Font("Arial", Font.BOLD, 14));
		categoriesPanel.add(otherBonusesLabel);
		JPanel otherPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		meleeStrengthLabel = new JLabel("Melee Strength: 0");
		rangedStrengthLabel = new JLabel("Ranged Strength: 0");
		magicDamageLabel = new JLabel("Magic Damage: 0%");
		prayerLabel = new JLabel("Prayer: 0");
		otherPanel.add(meleeStrengthLabel);
		otherPanel.add(rangedStrengthLabel);
		otherPanel.add(magicDamageLabel);
		otherPanel.add(prayerLabel);
		categoriesPanel.add(otherPanel);

		// Target-Specific
		JLabel targetSpecificLabel = new JLabel("Target-Specific", SwingConstants.CENTER);
		targetSpecificLabel.setFont(new Font("Arial", Font.BOLD, 14));
		categoriesPanel.add(targetSpecificLabel);
		JPanel targetPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		undeadLabel = new JLabel("Undead: No");
		slayerLabel = new JLabel("Slayer: No");
		targetPanel.add(undeadLabel);
		targetPanel.add(slayerLabel);
		categoriesPanel.add(targetPanel);

		summaryPanel.add(categoriesPanel, BorderLayout.CENTER);

		return summaryPanel;
	}

	private void updateSummary() {
		// Initialize totals
		int totalAttackStab = 0;
		int totalAttackSlash = 0;
		int totalAttackCrush = 0;
		int totalAttackMagic = 0;
		int totalAttackRange = 0;

		int totalDefenseStab = 0;
		int totalDefenseSlash = 0;
		int totalDefenseCrush = 0;
		int totalDefenseMagic = 0;
		int totalDefenseRange = 0;

		int totalMeleeStrength = 0;
		int totalRangedStrength = 0;
		int totalMagicDamage = 0;
		int totalPrayer = 0;

		boolean hasUndead = false;
		boolean hasSlayer = false;

		// Sum up the bonuses from selected equipment
		for (EquipmentData eqData : selectedEquipment.values()) {
			if (eqData == null) continue;

			// Offensive stats
			EquipmentStats offensive = eqData.getOffensive();
			if (offensive != null) {
				totalAttackStab += offensive.getStab();
				totalAttackSlash += offensive.getSlash();
				totalAttackCrush += offensive.getCrush();
				totalAttackMagic += offensive.getMagic();
				totalAttackRange += offensive.getRanged();
			}

			// Defensive stats
			EquipmentStats defensive = eqData.getDefensive();
			if (defensive != null) {
				totalDefenseStab += defensive.getStab();
				totalDefenseSlash += defensive.getSlash();
				totalDefenseCrush += defensive.getCrush();
				totalDefenseMagic += defensive.getMagic();
				totalDefenseRange += defensive.getRanged();
			}

			// Other bonuses
			EquipmentBonuses bonuses = eqData.getBonuses(); // Assuming EquipmentBonuses has the necessary fields
			if (bonuses != null) {
				totalMeleeStrength += bonuses.getStr();
				totalRangedStrength += bonuses.getRanged_str();
				totalMagicDamage += bonuses.getMagic_str();
				totalPrayer += bonuses.getPrayer();
				//if (bonuses.isUndead()) {
					hasUndead = true;
				//}
				//if (bonuses.isSlayer()) {
					hasSlayer = true;
				//}
			}
		}

		// Update labels
		attackStabLabel.setText("Stab: " + totalAttackStab);
		attackSlashLabel.setText("Slash: " + totalAttackSlash);
		attackCrushLabel.setText("Crush: " + totalAttackCrush);
		attackMagicLabel.setText("Magic: " + totalAttackMagic);
		attackRangeLabel.setText("Range: " + totalAttackRange);

		defenseStabLabel.setText("Stab: " + totalDefenseStab);
		defenseSlashLabel.setText("Slash: " + totalDefenseSlash);
		defenseCrushLabel.setText("Crush: " + totalDefenseCrush);
		defenseMagicLabel.setText("Magic: " + totalDefenseMagic);
		defenseRangeLabel.setText("Range: " + totalDefenseRange);

		meleeStrengthLabel.setText("Melee Strength: " + totalMeleeStrength);
		rangedStrengthLabel.setText("Ranged Strength: " + totalRangedStrength);
		magicDamageLabel.setText("Magic Damage: " + totalMagicDamage + "%");
		prayerLabel.setText("Prayer: " + totalPrayer);

		undeadLabel.setText("Undead: " + (hasUndead ? "Yes" : "No"));
		slayerLabel.setText("Slayer: " + (hasSlayer ? "Yes" : "No"));
	}

	private void addEquipmentButtonListener(JButton button, String slot) {
		// Existing ActionListener for left-click events
		button.addActionListener(e -> {
			showEquipmentSelectionDialog(button, slot);
		});

		// Add a MouseListener to detect right-click events
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					// Remove the equipment from the selectedEquipment map
					selectedEquipment.remove(slot);

					// Reset the button to default appearance
					button.setIcon(null);
					button.setText("+");
					button.setToolTipText(slot);

					// Update the summary panel
					updateSummary();
				}
			}
		});
	}

	private void showEquipmentSelectionDialog(JButton button, String slot) {
		List<EquipmentData> equipmentList = equipmentMap.get(slot);
		if (equipmentList == null || equipmentList.isEmpty()) {
			JOptionPane.showMessageDialog(createFrame, "No equipment available for slot: " + slot);
			return;
		}

		// Create a word bank of equipment names
		List<String> equipmentNames = new ArrayList<>();
		for (EquipmentData eq : equipmentList) {
			equipmentNames.add(eq.getName());
		}
		// Sort equipment names alphabetically
		Collections.sort(equipmentNames);

		// Create the autocomplete panel (inputPanel)
		JPanel inputPanel = new JPanel(new BorderLayout());
		inputPanel.setBackground(new Color(255, 255, 255, 230));
		inputPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

		JTextField textField = new JTextField();
		inputPanel.add(textField, BorderLayout.NORTH);

		textField.setFocusTraversalKeysEnabled(false);

		DefaultListModel<String> listModel = new DefaultListModel<>();
		JList<String> suggestionList = new JList<>(listModel);
		suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane suggestionScrollPane = new JScrollPane(suggestionList);
		suggestionScrollPane.setPreferredSize(new Dimension(200, 150));
		inputPanel.add(suggestionScrollPane, BorderLayout.CENTER);

		// Prepare the overlay panel
		JLayeredPane layeredPane = createFrame.getLayeredPane();

		JPanel overlayPanel = new JPanel(null);
		overlayPanel.setOpaque(false);
		overlayPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());

		// Position the input panel near the button
		Point buttonLocation = SwingUtilities.convertPoint(button, new Point(0, button.getHeight()), layeredPane);
		int inputPanelWidth = 200;
		int x = buttonLocation.x;
		int y = buttonLocation.y;

		// Adjust x position if it goes off-screen
		if (x + inputPanelWidth > layeredPane.getWidth()) {
			x = layeredPane.getWidth() - inputPanelWidth;
		}
		if (x < 0) {
			x = 0;
		}

		inputPanel.setBounds(x, y, inputPanelWidth, 200);

		// Add inputPanel to overlayPanel
		overlayPanel.add(inputPanel);

		// Add overlayPanel to layeredPane
		layeredPane.add(overlayPanel, JLayeredPane.POPUP_LAYER);
		layeredPane.revalidate();
		layeredPane.repaint();

		textField.requestFocusInWindow();

		// Implement autocomplete functionality
		Runnable updateSuggestions = () -> {
			String text = textField.getText();
			listModel.clear();
			List<String> matchingNames = new ArrayList<>();
			List<String> otherNames = new ArrayList<>();

			for (String name : equipmentNames) {
				if (text.isEmpty()) {
					matchingNames.add(name);
				} else if (name.toLowerCase().startsWith(text.toLowerCase())) {
					matchingNames.add(name);
				} else if (name.toLowerCase().contains(text.toLowerCase())) {
					otherNames.add(name);
				}
			}
			// Sort the lists alphabetically
			Collections.sort(matchingNames);
			Collections.sort(otherNames);
			// Add matching names first
			for (String name : matchingNames) {
				listModel.addElement(name);
			}
			// Then add other names
			for (String name : otherNames) {
				listModel.addElement(name);
			}
			if (!listModel.isEmpty()) {
				suggestionList.setSelectedIndex(0);
			}
		};

		// Call updateSuggestions initially to populate the list
		updateSuggestions.run();

		textField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateSuggestions.run();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateSuggestions.run();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateSuggestions.run();
			}
		});

		suggestionList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					String selectedName = suggestionList.getSelectedValue();
					selectEquipmentForSlot(button, slot, selectedName);
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
			}
		});

		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					String selectedName;
					if (!suggestionList.isSelectionEmpty()) {
						selectedName = suggestionList.getSelectedValue();
					} else {
						selectedName = textField.getText();
					}
					selectEquipmentForSlot(button, slot, selectedName);
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					int index = suggestionList.getSelectedIndex();
					if (index < listModel.getSize() - 1) {
						suggestionList.setSelectedIndex(index + 1);
						suggestionList.ensureIndexIsVisible(index + 1);
					}
				} else if (e.getKeyCode() == KeyEvent.VK_UP) {
					int index = suggestionList.getSelectedIndex();
					if (index > 0) {
						suggestionList.setSelectedIndex(index - 1);
						suggestionList.ensureIndexIsVisible(index - 1);
					}
				} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					// Remove the overlayPanel when ESC is pressed
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
			}
		});

		// Add a mouse listener to the overlayPanel to detect clicks outside the inputPanel
		overlayPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// Check if click was outside inputPanel
				if (!inputPanel.getBounds().contains(e.getPoint())) {
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
			}
		});

		// Consume mouse events on the overlay to prevent them from reaching underlying components
		overlayPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				// Do nothing to consume the event
			}
		});
	}

	private JFrame createFrame;

	private void selectEquipmentForSlot(JButton button, String slot, String equipmentName) {
		List<EquipmentData> equipmentList = equipmentMap.get(slot);
		if (equipmentList == null) {
			return;
		}
		EquipmentData selectedEquipmentData = null;
		for (EquipmentData eq : equipmentList) {
			if (eq.getName().equalsIgnoreCase(equipmentName)) {
				selectedEquipmentData = eq;
				break;
			}
		}
		if (selectedEquipmentData == null) {
			JOptionPane.showMessageDialog(createFrame, "Equipment not found: " + equipmentName);
			return;
		}
		// Save the selected equipment
		selectedEquipment.put(slot, selectedEquipmentData);

		// Use itemManager to get the image
		int itemId = selectedEquipmentData.getId();
		AsyncBufferedImage itemImage = itemManager.getImage(itemId);

		// Clear the button's text since we'll use an image
		button.setText("");
		// Optionally, set the tooltip to the equipment name
		button.setToolTipText(selectedEquipmentData.getName());

		// Set a placeholder icon if desired
		button.setIcon(new ImageIcon()); // Empty icon for now

		// When the image is loaded, update the button's icon
		itemImage.onLoaded(() -> {
			SwingUtilities.invokeLater(() -> {
				ImageIcon icon = new ImageIcon(itemImage);
				// Resize the icon to fit the button
				int width = button.getWidth();
				int height = button.getHeight();
				if (width == 0 || height == 0) {
					// Fallback to default size if button size not available
					width = 60;
					height = 60;
				}
				Image image = icon.getImage();
				Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
				icon = new ImageIcon(scaledImage);
				button.setIcon(icon);
			});
		});

		// Save the selected equipment
		selectedEquipment.put(slot, selectedEquipmentData);

		// Update the button with the equipment's image
		updateButtonWithEquipment(button, selectedEquipmentData);

		// Update the summary panel
		updateSummary();
	}

	JPanel prayerPanel;

	private JPanel createPrayerPanel() {
		prayerPanel = new JPanel();
		prayerPanel.setLayout(new BorderLayout());

		// Set border with padding
		prayerPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));

		JLabel prayerLabel = new JLabel("Prayers", SwingConstants.CENTER);
		prayerLabel.setFont(new Font("Arial", Font.BOLD, 16));
		prayerPanel.add(prayerLabel, BorderLayout.NORTH);

		// Prayer grid panel
		JPanel prayerGrid = new JPanel();
		GridBagLayout gridBagLayout = new GridBagLayout();
		prayerGrid.setLayout(gridBagLayout);
		prayerPanel.add(prayerGrid, BorderLayout.CENTER);

		// Clear the prayerButtons map
		prayerButtons.clear();

		// Create buttons for each prayer
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5); // Spacing between buttons
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		gbc.weighty = 0;

		int prayersPerRow = 5; // Adjust as needed
		int row = 0;
		int col = 0;

		for (Prayers prayer : Prayers.values()) {
			SquareButton button = new SquareButton("");
			button.setToolTipText(prayer.name);
			prayerButtons.put(prayer, button);

			// Add action listener to toggle prayer state
			button.addActionListener(e -> {
				boolean isEnabled = selectedPrayers.getOrDefault(prayer, false);
				selectedPrayers.put(prayer, !isEnabled);
				updatePrayerButtonIcon(button, prayer, !isEnabled);
				// Update the summary when a prayer is toggled
				updatePrayerSummary();
			});

			gbc.gridx = col;
			gbc.gridy = row;
			prayerGrid.add(button, gbc);

			col++;
			if (col >= prayersPerRow) {
				col = 0;
				row++;
			}
		}

		// After all buttons are added, update their icons
		for (Prayers prayer : Prayers.values()) {
			boolean isEnabled = selectedPrayers.getOrDefault(prayer, false);
			JButton button = prayerButtons.get(prayer);
			updatePrayerButtonIcon(button, prayer, isEnabled);
		}

		// Create and add the summary panel at the bottom
		JPanel summaryPanel = createPrayerSummaryPanel();
		prayerPanel.add(summaryPanel, BorderLayout.SOUTH);

		return prayerPanel;
	}


	private Map<String, Preset> presets = new HashMap<>();

	private void updatePrayerButtonIcon(JButton button, Prayers prayer, boolean isEnabled)
	{
		int spriteId = isEnabled ? prayer.enabledID : prayer.disabledID;

		// Clear current icon
		button.setIcon(null);

		// Fetch the sprite on the client thread
		clientThread.invokeLater(() -> {
			BufferedImage sprite = spriteManager.getSprite(spriteId, 0);
			if (sprite != null)
			{
				int size = button.getWidth();
				if (size <= 0)
				{
					// Use preferred size if actual size is not available
					Dimension dim = button.getPreferredSize();
					size = Math.max(dim.width, dim.height);
				}

				// Resize the image to fit the button
				Image scaledImage = sprite.getScaledInstance(size, size, Image.SCALE_SMOOTH);

				// Update the UI on the EDT
				SwingUtilities.invokeLater(() -> {
					button.setIcon(new ImageIcon(scaledImage));
				});
			}
		});
	}
	private Map<Prayers, Boolean> selectedPrayers = new HashMap<>();

	// Helper method to create the Style panel
// Map to hold style buttons
	private Map<String, JButton> styleButtons = new HashMap<>();

	// Variable to store the selected style
	private String selectedStyle;

	JPanel stylePanel;
	private JPanel createStylePanel() {
		stylePanel = new JPanel();
		stylePanel.setLayout(new BorderLayout());

		// Set border with padding
		stylePanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));

		JLabel styleLabel = new JLabel("Styles", SwingConstants.CENTER);
		styleLabel.setFont(new Font("Arial", Font.BOLD, 16));
		stylePanel.add(styleLabel, BorderLayout.NORTH);

		// Main panel with vertical BoxLayout to hold the groups
		JPanel groupsPanel = new JPanel();
		groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.Y_AXIS));
		stylePanel.add(groupsPanel, BorderLayout.CENTER);

		// Clear the styleButtons map
		styleButtons.clear();

		// Define the style groups
		String[][] styleGroups = {
			{ "Accurate", "Aggressive", "Defensive", "Controlled" },
			{ "Accurate (Ranged)", "Rapid", "Longrange" },
			{ "Standard", "Defensive (Magic)" }
		};

		String[] groupLabels = {
			"Melee Styles",
			"Ranged Styles",
			"Magic Styles"
		};

		// Loop through the groups to create sections
		for (int i = 0; i < styleGroups.length; i++) {
			String[] styles = styleGroups[i];
			String groupLabel = groupLabels[i];

			// Section label
			JLabel sectionLabel = new JLabel(groupLabel);
			sectionLabel.setFont(new Font("Arial", Font.BOLD, 14));
			sectionLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0)); // Top and bottom padding
			groupsPanel.add(sectionLabel);

			// Panel for the styles in this group
			JPanel stylesGroupPanel = new JPanel();
			stylesGroupPanel.setLayout(new BoxLayout(stylesGroupPanel, BoxLayout.Y_AXIS));

			for (String styleName : styles) {
				JButton button = createStyleButton(styleName);
				stylesGroupPanel.add(button);
				stylesGroupPanel.add(Box.createVerticalStrut(5)); // Spacing between buttons
			}

			// Add the group panel to the main groups panel
			groupsPanel.add(stylesGroupPanel);
		}

		// Add glue to push content to the top
		groupsPanel.add(Box.createVerticalGlue());

		return stylePanel;
	}

	private Map<Integer, NPCData> columnToNPCDataMap = new HashMap<>();
	private JButton createStyleButton(String styleName) {
		JButton button = new JButton(styleName);
		button.setFocusPainted(false); // Optional: Remove focus border
		button.setAlignmentX(Component.LEFT_ALIGNMENT);

		// Set button dimensions (wider than tall)
		Dimension buttonSize = new Dimension(200, 30); // Adjust width and height as needed
		button.setPreferredSize(buttonSize);
		button.setMaximumSize(buttonSize);

		// Add action listener to handle selection
		button.addActionListener(e -> {
			// Deselect previous selection
			if (selectedStyle != null && styleButtons.containsKey(selectedStyle)) {

				JButton previousButton = styleButtons.get(selectedStyle);
				previousButton.setBackground(null); // Reset background
				previousButton.setForeground(Color.BLACK); // Reset text color
			}

			// Update selected style
			selectedStyle = styleName;
			button.setBackground(Color.RED.darker().darker());
			button.setForeground(Color.WHITE); // Change text color for contrast

			// Save to selectedStyle variable
			this.selectedStyle = styleName;
		});

		// If this style is the currently selected one, highlight it
		if (styleName.equals(selectedStyle)) {
			button.setBackground(Color.RED.darker().darker());
			button.setForeground(Color.WHITE); // Change text color for contrast
		} else {
			button.setBackground(null);
			button.setForeground(Color.BLACK);
		}

		// Add the button to the map
		styleButtons.put(styleName, button);

		return button;
	}

	// Helper method to create square buttons with a plus sign
	private JButton createSquareButton(String text) {
		JButton button = new JButton(text);
		Dimension size = new Dimension(60, 60); // Square button size
		button.setPreferredSize(size);
		button.setMinimumSize(size);
		button.setMaximumSize(size);
		return button;
	}
}

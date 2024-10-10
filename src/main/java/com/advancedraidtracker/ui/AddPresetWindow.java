package com.advancedraidtracker.ui;

import static com.advancedraidtracker.ui.PresetManager.DPS_UTILITY_FOLDER;
import static com.advancedraidtracker.ui.PresetManager.EQUIPMENT_PRESETS_FILE;
import static com.advancedraidtracker.ui.PresetManager.loadPresets;
import static com.advancedraidtracker.ui.charts.ChartIO.gson;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.clientThread;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.itemManager;
import static com.advancedraidtracker.ui.charts.chartelements.OutlineBox.spriteManager;
import com.advancedraidtracker.ui.dpsanalysis.EquipmentBonuses;
import com.advancedraidtracker.ui.dpsanalysis.EquipmentData;
import com.advancedraidtracker.ui.dpsanalysis.EquipmentStats;
import com.advancedraidtracker.ui.dpsanalysis.Prayers;
import com.advancedraidtracker.ui.dpsanalysis.Preset;
import com.advancedraidtracker.ui.dpsanalysis.SquareButton;
import com.google.gson.Gson;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.AsyncBufferedImage;

@Slf4j
public class AddPresetWindow
{

	private Map<String, EquipmentData> selectedEquipment = new HashMap<>();
	private Map<Prayers, Boolean> selectedPrayers = new HashMap<>();
	private String selectedStyle;
	private Map<String, Integer> baseLevels = new HashMap<>();
	private Map<String, String> selectedPotions = new HashMap<>();
	private Map<String, JTextField> baseLevelFields = new HashMap<>();
	private Map<String, JComboBox<String>> potionComboBoxes = new HashMap<>();
	private Map<String, JLabel> virtualLevelLabels = new HashMap<>();
	private Map<String, List<EquipmentData>> equipmentMap = new HashMap<>();
	private String selectedAttackType = null;
	private JFrame createFrame;
	JPanel equipmentPanel;
	JPanel prayerPanel;
	JPanel stylePanel;
	JPanel skillPanel;
	Map<String, JButton> slotButtons = new HashMap<>();
	private Map<Prayers, JButton> prayerButtons = new HashMap<>();
	private Map<String, JToggleButton> styleButtons = new HashMap<>();
	private Map<String, JToggleButton> attackTypeButtons = new HashMap<>();
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
	private JLabel prayerRangedDamageBonusLabel;
	private JLabel prayerMagicDamageBonusLabel;

	public AddPresetWindow()
	{
		this("");
	}

	public AddPresetWindow(String fromExisting)
	{
		loadPresets();
		try
		{
			loadEquipmentData();
		}
		catch (Exception e)
		{
			log.info("Failed to load equipment data from github");
			e.printStackTrace();
		}

		if (fromExisting.isEmpty())
		{
			selectedEquipment = new HashMap<>();
			selectedPrayers = new HashMap<>();
			selectedStyle = null;
		}
		else
		{
			Preset preset = PresetManager.getPresets().get(fromExisting);
			if (preset == null)
			{
				selectedEquipment = new HashMap<>();
				selectedPrayers = new HashMap<>();
				selectedStyle = null;
			}
			else
			{
				selectedEquipment = new HashMap<>();
				selectedPrayers = new HashMap<>();
				selectedStyle = null;
			}
		}


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
		for (String presetName : PresetManager.getPresets().keySet())
		{
			baseTemplateComboBox.addItem(presetName);
		}

		topPanel.add(baseTemplateLabel);
		topPanel.add(baseTemplateComboBox);

		// Add action listener to update the create frame when a preset is selected
		baseTemplateComboBox.addActionListener(e -> {
			String selectedPresetName = (String) baseTemplateComboBox.getSelectedItem();
			if (selectedPresetName != null && !selectedPresetName.equals("None"))
			{
				// Load the selected preset as the base template
				loadPresetAsBaseTemplate(selectedPresetName);
			}
			else
			{
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
		skillPanel = createSkillPanel(); // Add this line

		// Combine style and skill panels vertically
		JPanel styleAndSkillPanel = new JPanel();
		styleAndSkillPanel.setLayout(new BoxLayout(styleAndSkillPanel, BoxLayout.Y_AXIS));
		styleAndSkillPanel.add(stylePanel);
		styleAndSkillPanel.add(Box.createVerticalStrut(10));
		styleAndSkillPanel.add(skillPanel);

		// Add panels to contentPanel
		contentPanel.add(equipmentPanel);
		contentPanel.add(Box.createHorizontalStrut(10));
		contentPanel.add(prayerPanel);
		contentPanel.add(Box.createHorizontalStrut(10));
		contentPanel.add(styleAndSkillPanel); // Add the combined panel

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
		createFrame.setSize(1100, 775); // Adjust the size to fit all components
		createFrame.setLocationRelativeTo(null);
		createFrame.setVisible(true);

		// Action listener for savePresetButton
		savePresetButton.addActionListener(e -> {
			String presetName = presetNameField.getText().trim();
			if (presetName.isEmpty())
			{
				JOptionPane.showMessageDialog(createFrame, "Please enter a preset name.");
				return;
			}
			if (PresetManager.getPresets().containsKey(presetName))
			{
				JOptionPane.showMessageDialog(createFrame, "A preset with that name already exists.");
				return;
			}
			if (savePreset(presetName))
			{
				createFrame.dispose();
			}
		});
	}

	private void loadEquipmentData() throws IOException
	{
		String url = "https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/main/cdn/json/equipment.json";
		Gson reader = gson.newBuilder().create();
		EquipmentData[] equipmentDataArray = reader.fromJson(new InputStreamReader(new URL(url).openStream()), EquipmentData[].class);

		for (EquipmentData data : equipmentDataArray)
		{
			String slot = data.getSlot();
			if (slot == null || slot.isEmpty())
			{
				continue; // Skip equipment without a slot or handle accordingly
			}
			equipmentMap.computeIfAbsent(slot, k -> new ArrayList<>()).add(data);
		}
	}

	private void loadPresetAsBaseTemplate(String presetName)
	{
		Preset basePreset = PresetManager.getPresets().get(presetName);
		if (basePreset == null)
		{
			JOptionPane.showMessageDialog(createFrame, "Preset not found: " + presetName);
			return;
		}

		// Copy the equipment, prayers, skill levels, potions, and style from the base preset
		selectedEquipment = new HashMap<>(basePreset.getEquipment());
		selectedPrayers = new HashMap<>(basePreset.getPrayers());
		selectedStyle = basePreset.getSelectedStyle();
		baseLevels = new HashMap<>(basePreset.getBaseLevels());
		selectedPotions = new HashMap<>(basePreset.getSelectedPotions());
		selectedAttackType = basePreset.getSelectedAttackType();

		// Update the UI components to reflect the selected base template
		updateEquipmentPanel();
		updatePrayerPanel();
		updateStylePanel();
		updateAttackTypeSelection();
		updateSkillPanel();
	}

	private void updateEquipmentPanel()
	{
		// For each slot, update the button icon/text
		for (String slotName : slotButtons.keySet())
		{
			JButton slotButton = slotButtons.get(slotName);
			EquipmentData equipmentData = selectedEquipment.get(slotName);
			if (equipmentData != null)
			{
				// Update the button with the equipment's image
				updateButtonWithEquipment(slotButton, equipmentData);
			}
			else
			{
				// Clear the button or set it to default
				slotButton.setIcon(null);
				slotButton.setText("+");
				slotButton.setToolTipText(slotName);
			}
		}

		// Update the summary panel
		updateSummary();
	}

	private void updatePrayerPanel()
	{
		// For each prayer, update the button icon
		for (Prayers prayer : prayerButtons.keySet())
		{
			JButton button = prayerButtons.get(prayer);
			boolean isEnabled = selectedPrayers.getOrDefault(prayer, false);
			updatePrayerButtonIcon(button, prayer, isEnabled);
		}

		// Update the prayer summary
		updatePrayerSummary();
	}

	private void updateStylePanel()
	{
		// Update the style buttons
		for (String styleName : styleButtons.keySet())
		{
			JToggleButton button = styleButtons.get(styleName);
			if (styleName.equals(selectedStyle))
			{
				button.setSelected(true);
			}
			else
			{
				button.setSelected(false);
			}
		}

		updateStyleSelection(); // This will enable/disable attack type buttons

		// Update attack type buttons
		updateAttackTypeSelection();
	}

	private void updateSkillPanel()
	{
		String[] skills = {"Attack", "Strength", "Ranged", "Magic"};
		for (String skill : skills)
		{
			baseLevelFields.get(skill).setText(String.valueOf(baseLevels.get(skill)));
			potionComboBoxes.get(skill).setSelectedItem(selectedPotions.get(skill));
			updateVirtualLevel(skill);
		}
	}

	private JPanel createEquipmentPanel(Map<String, EquipmentData> presetEquipment)
	{
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
		for (int i = 0; i < 3; i++)
		{
			gbc.gridx = i;
			JButton button = createSquareButton("+");
			slot = equipmentSlots.get(slotIndex++);
			addEquipmentButtonListener(button, slot);
			slotButtons.put(slot, button);
			equipmentGrid.add(button, gbc);
		}

		// Third row, 3 buttons
		gbc.gridy = 2;
		for (int i = 0; i < 3; i++)
		{
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
		for (int i = 0; i < 3; i++)
		{
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
		if (presetEquipment != null)
		{
			for (Map.Entry<String, EquipmentData> entry : presetEquipment.entrySet())
			{
				String slotName = entry.getKey();
				EquipmentData equipmentData = entry.getValue();
				JButton slotButton = slotButtons.get(slotName);
				if (slotButton != null)
				{
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

	private JPanel createPrayerPanel()
	{
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

		for (Prayers prayer : Prayers.values())
		{
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

				// Update virtual levels
				updateAllVirtualLevels();
			});

			gbc.gridx = col;
			gbc.gridy = row;
			prayerGrid.add(button, gbc);

			col++;
			if (col >= prayersPerRow)
			{
				col = 0;
				row++;
			}
		}

		// After all buttons are added, update their icons
		for (Prayers prayer : Prayers.values())
		{
			boolean isEnabled = selectedPrayers.getOrDefault(prayer, false);
			JButton button = prayerButtons.get(prayer);
			updatePrayerButtonIcon(button, prayer, isEnabled);
		}

		// Create and add the summary panel at the bottom
		JPanel summaryPanel = createPrayerSummaryPanel();
		prayerPanel.add(summaryPanel, BorderLayout.SOUTH);

		return prayerPanel;
	}

	private JPanel createStylePanel()
	{
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
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		stylePanel.add(mainPanel, BorderLayout.CENTER);

		// Define the style groups
		String[][] styleGroups = {
			{"Accurate", "Aggressive", "Defensive", "Controlled"},
			{"Accurate (Ranged)", "Rapid", "Longrange"},
			{"Standard", "Defensive (Magic)"}
		};

		String[] groupLabels = {
			"Melee Styles",
			"Ranged Styles",
			"Magic Styles"
		};

		ButtonGroup styleButtonGroup = new ButtonGroup(); // Ensure only one style is selected

		// Loop through the groups to create sections
		for (int i = 0; i < styleGroups.length; i++)
		{
			String[] styles = styleGroups[i];
			String groupLabel = groupLabels[i];

			// Section label
			JLabel sectionLabel = new JLabel(groupLabel, SwingConstants.CENTER);
			sectionLabel.setFont(new Font("Arial", Font.BOLD, 14));
			sectionLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0)); // Top and bottom padding
			sectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center within BoxLayout
			mainPanel.add(sectionLabel);

			// Panel for the styles in this group
			JPanel stylesGroupPanel = new JPanel(new GridLayout(0, 2, 5, 5)); // 2 columns
			for (String styleName : styles)
			{
				JToggleButton button = createStyleToggleButton(styleName);
				styleButtonGroup.add(button);
				stylesGroupPanel.add(button);
			}

			mainPanel.add(stylesGroupPanel);

			// If this is the melee group, add attack type options
			if (groupLabel.equals("Melee Styles"))
			{
				mainPanel.add(Box.createVerticalStrut(10)); // Spacing

				JLabel attackTypeLabel = new JLabel("Attack Type", SwingConstants.CENTER);
				attackTypeLabel.setFont(new Font("Arial", Font.BOLD, 14));
				attackTypeLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
				attackTypeLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center within BoxLayout
				mainPanel.add(attackTypeLabel);

				JPanel attackTypePanel = new JPanel(new GridLayout(0, 3, 5, 5)); // 3 columns

				ButtonGroup attackTypeButtonGroup = new ButtonGroup();
				String[] attackTypes = {"Stab", "Slash", "Crush"};
				for (String attackType : attackTypes)
				{
					JToggleButton attackTypeButton = createAttackTypeToggleButton(attackType);
					attackTypeButtonGroup.add(attackTypeButton);
					attackTypePanel.add(attackTypeButton);
					attackTypeButtons.put(attackType, attackTypeButton);

					// Initially disable attack type buttons
					attackTypeButton.setEnabled(false);
				}

				mainPanel.add(attackTypePanel);
			}

			mainPanel.add(Box.createVerticalStrut(10)); // Spacing between groups
		}

		// Add glue to push content to the top
		mainPanel.add(Box.createVerticalGlue());

		return stylePanel;
	}

	private JPanel createSkillPanel()
	{
		skillPanel = new JPanel();
		skillPanel.setLayout(new BorderLayout());

		// Set border with padding
		skillPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));

		JLabel skillLabel = new JLabel("Skills", SwingConstants.CENTER);
		skillLabel.setFont(new Font("Arial", Font.BOLD, 16));
		skillPanel.add(skillLabel, BorderLayout.NORTH);

		// Create a table-like structure using GridBagLayout
		JPanel skillsGridPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.weightx = 1.0;

		// Define headers
		String[] headers = {"Skill", "Level", "Potion", "Virtual Level"};
		for (int i = 0; i < headers.length; i++)
		{
			gbc.gridx = i;
			gbc.gridy = 0;
			JLabel headerLabel = new JLabel(headers[i]);
			headerLabel.setFont(new Font("Arial", Font.BOLD, 12));
			skillsGridPanel.add(headerLabel, gbc);
		}

		// For each skill, create the row
		String[] skills = {"Attack", "Strength", "Ranged", "Magic"};
		int row = 1; // Start from row 1 since row 0 is the header

		for (String skill : skills)
		{
			gbc.gridy = row;

			// Skill name label
			gbc.gridx = 0;
			JLabel skillNameLabel = new JLabel(skill);
			skillsGridPanel.add(skillNameLabel, gbc);

			// Base level field
			gbc.gridx = 1;
			JTextField baseLevelField = new JTextField(3);
			baseLevelField.setText(String.valueOf(baseLevels.get(skill)));
			skillsGridPanel.add(baseLevelField, gbc);

			// Potion dropdown
			gbc.gridx = 2;
			JComboBox<String> potionComboBox = new JComboBox<>();
			for (String potion : getPotionsForSkill(skill))
			{
				potionComboBox.addItem(potion);
			}
			potionComboBox.setSelectedItem(selectedPotions.get(skill));
			skillsGridPanel.add(potionComboBox, gbc);

			// Virtual level label
			gbc.gridx = 3;
			JLabel virtualLevelLabel = new JLabel(String.valueOf(calculateVirtualLevel(skill)));
			skillsGridPanel.add(virtualLevelLabel, gbc);

			// Store references for later updates
			baseLevelFields.put(skill, baseLevelField);
			potionComboBoxes.put(skill, potionComboBox);
			virtualLevelLabels.put(skill, virtualLevelLabel);

			// Add listeners to update virtual levels when inputs change
			baseLevelField.getDocument().addDocumentListener(new DocumentListener()
			{
				public void changedUpdate(DocumentEvent e)
				{
					updateVirtualLevel(skill);
				}

				public void removeUpdate(DocumentEvent e)
				{
					updateVirtualLevel(skill);
				}

				public void insertUpdate(DocumentEvent e)
				{
					updateVirtualLevel(skill);
				}
			});

			potionComboBox.addActionListener(e -> {
				// Update selectedPotions here
				String potion = (String) potionComboBox.getSelectedItem();
				selectedPotions.put(skill, potion);

				// Now update the virtual level
				updateVirtualLevel(skill);
			});

			row++;
		}

		skillPanel.add(skillsGridPanel, BorderLayout.CENTER);

		return skillPanel;
	}

	private void updateAttackTypeSelection()
	{
		for (String attackType : attackTypeButtons.keySet())
		{
			JToggleButton button = attackTypeButtons.get(attackType);
			if (attackType.equals(selectedAttackType))
			{
				button.setSelected(true);
			}
			else
			{
				button.setSelected(false);
			}
		}
	}

	private void updateButtonWithEquipment(JButton button, EquipmentData equipmentData)
	{
		// Use itemManager to get the image
		int itemId = equipmentData.getId();
		AsyncBufferedImage itemImage = itemManager.getImage(itemId);

		// Clear the button's text since we'll use an image
		button.setText("");
		// Set the tooltip to the equipment name and stats
		String tooltipText = getEquipmentTooltipText(equipmentData);
		button.setToolTipText(tooltipText);

		// Set a placeholder icon if desired
		button.setIcon(new ImageIcon()); // Empty icon for now

		// When the image is loaded, update the button's icon
		itemImage.onLoaded(() -> {
			SwingUtilities.invokeLater(() -> {
				ImageIcon icon = new ImageIcon(itemImage);
				// Resize the icon to fit the button
				int width = button.getWidth();
				int height = button.getHeight();
				if (width == 0 || height == 0)
				{
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

	private void updateSummary()
	{
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
		for (EquipmentData eqData : selectedEquipment.values())
		{
			if (eqData == null)
			{
				continue;
			}

			// Offensive stats
			EquipmentStats offensive = eqData.getOffensive();
			if (offensive != null)
			{
				totalAttackStab += offensive.getStab();
				totalAttackSlash += offensive.getSlash();
				totalAttackCrush += offensive.getCrush();
				totalAttackMagic += offensive.getMagic();
				totalAttackRange += offensive.getRanged();
			}

			// Defensive stats
			EquipmentStats defensive = eqData.getDefensive();
			if (defensive != null)
			{
				totalDefenseStab += defensive.getStab();
				totalDefenseSlash += defensive.getSlash();
				totalDefenseCrush += defensive.getCrush();
				totalDefenseMagic += defensive.getMagic();
				totalDefenseRange += defensive.getRanged();
			}

			// Other bonuses
			EquipmentBonuses bonuses = eqData.getBonuses(); // Assuming EquipmentBonuses has the necessary fields
			if (bonuses != null)
			{
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

	private void updatePrayerSummary()
	{
		int totalAttackBonus = 0;
		int totalStrengthBonus = 0;
		int totalDefenseBonus = 0;
		int totalMagicAttackBonus = 0;
		int totalMagicDamageBonus = 0;
		int totalRangedAttackBonus = 0;
		int totalRangedDamageBonus = 0;

		for (Map.Entry<Prayers, Boolean> entry : selectedPrayers.entrySet())
		{
			if (entry.getValue())
			{
				Prayers prayer = entry.getKey();
				totalAttackBonus += prayer.att;
				totalStrengthBonus += prayer.str;
				totalDefenseBonus += prayer.def;
				totalMagicAttackBonus += prayer.mag;
				totalMagicDamageBonus += prayer.magD;
				totalRangedAttackBonus += prayer.ranA;
				totalRangedDamageBonus += prayer.ranD;
			}
		}

		prayerAttackBonusLabel.setText("Melee Attack Bonus: " + totalAttackBonus + "%");
		prayerStrengthBonusLabel.setText("Melee Strength Bonus: " + totalStrengthBonus + "%");
		prayerDefenseBonusLabel.setText("Defense Bonus: " + totalDefenseBonus + "%");
		prayerMagicBonusLabel.setText("Magic Attack Bonus: " + totalMagicAttackBonus + "%");
		prayerMagicDamageBonusLabel.setText("Magic Damage Bonus: " + totalMagicDamageBonus + "%");
		prayerRangeBonusLabel.setText("Ranged Attack Bonus: " + totalRangedAttackBonus + "%");
		// Add a new label for Ranged Damage Bonus
		prayerRangedDamageBonusLabel.setText("Ranged Damage Bonus: " + totalRangedDamageBonus + "%");
	}

	private void updateStyleSelection()
	{
		if (isMeleeStyle(selectedStyle))
		{
			// Enable attack type buttons
			for (JToggleButton button : attackTypeButtons.values())
			{
				button.setEnabled(true);
			}
		}
		else
		{
			// Disable attack type buttons and reset selection
			selectedAttackType = null;
			for (JToggleButton button : attackTypeButtons.values())
			{
				button.setEnabled(false);
				button.setSelected(false);
			}
		}
	}

	private void updateVirtualLevel(String skill)
	{
		// Update base level
		try
		{
			int level = Integer.parseInt(baseLevelFields.get(skill).getText());
			baseLevels.put(skill, Math.min(Math.max(level, 1), 99));
		}
		catch (NumberFormatException ex)
		{
			baseLevels.put(skill, 1); // Default to 1 if invalid input
		}

		// Update selected potion
		String potion = (String) potionComboBoxes.get(skill).getSelectedItem();

		// Calculate virtual level
		int virtualLevel = calculateVirtualLevel(skill);

		// Update the virtual level label
		JLabel virtualLevelLabel = virtualLevelLabels.get(skill);
		virtualLevelLabel.setText(String.valueOf(virtualLevel));
	}

	private JButton createSquareButton(String text)
	{
		JButton button = new JButton(text);
		Dimension size = new Dimension(60, 60); // Square button size
		button.setPreferredSize(size);
		button.setMinimumSize(size);
		button.setMaximumSize(size);
		return button;
	}

	private void addEquipmentButtonListener(JButton button, String slot)
	{
		// Existing ActionListener for left-click events
		button.addActionListener(e -> {
			showEquipmentSelectionDialog(button, slot);
		});

		// Add a MouseListener to detect right-click events
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isRightMouseButton(e))
				{
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

	private JPanel createSummaryPanel()
	{
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

	private void updateAllVirtualLevels()
	{
		String[] skills = {"Attack", "Strength", "Ranged", "Magic"};
		for (String skill : skills)
		{
			int virtualLevel = calculateVirtualLevel(skill);
			virtualLevelLabels.get(skill).setText(String.valueOf(virtualLevel));
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
		prayerRangedDamageBonusLabel = new JLabel("Ranged Damage Bonus: 0%");

		summaryPanel.add(prayerAttackBonusLabel);
		summaryPanel.add(prayerStrengthBonusLabel);
		summaryPanel.add(prayerDefenseBonusLabel);
		summaryPanel.add(prayerMagicBonusLabel);
		summaryPanel.add(prayerRangeBonusLabel);
		summaryPanel.add(prayerMagicDamageBonusLabel);
		summaryPanel.add(prayerRangedDamageBonusLabel);

		return summaryPanel;
	}

	private JToggleButton createStyleToggleButton(String styleName)
	{
		JToggleButton button = new JToggleButton(styleName);
		button.setFocusPainted(false);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);

		// Set button dimensions
		Dimension buttonSize = new Dimension(100, 30);
		button.setPreferredSize(buttonSize);
		button.setMaximumSize(buttonSize);

		// Add action listener
		button.addActionListener(e -> {
			selectedStyle = styleName;
			updateStyleSelection();
		});

		// Highlight if selected
		if (styleName.equals(selectedStyle))
		{
			button.setSelected(true);
		}

		styleButtons.put(styleName, button);

		return button;
	}

	private JToggleButton createAttackTypeToggleButton(String attackType)
	{
		JToggleButton button = new JToggleButton(attackType);
		button.setFocusPainted(false);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);

		// Set button dimensions
		Dimension buttonSize = new Dimension(80, 30);
		button.setPreferredSize(buttonSize);
		button.setMaximumSize(buttonSize);

		// Add action listener
		button.addActionListener(e -> {
			selectedAttackType = attackType;
			updateAttackTypeSelection();
		});

		// Highlight if selected
		if (attackType.equals(selectedAttackType))
		{
			button.setSelected(true);
		}

		return button;
	}

	private List<String> getPotionsForSkill(String skill)
	{
		switch (skill)
		{
			case "Attack":
				return Arrays.asList("None", "Attack", "Super Attack", "Zamorak Brew", "Overload (Raids)", "Smelling Salts");
			case "Strength":
				return Arrays.asList("None", "Strength", "Super Strength", "Overload (Raids)", "Smelling Salts");
			case "Ranged":
				return Arrays.asList("None", "Ranging", "Super Ranging", "Overload (Raids)", "Smelling Salts");
			case "Magic":
				return Arrays.asList("None", "Magic", "Super Magic", "Ancient Brew", "Magister's Brew", "Imbued Heart", "Saturated Heart", "Overload (Raids)", "Smelling Salts");
			default:
				return Collections.singletonList("None");
		}
	}

	private int calculateVirtualLevel(String skill)
	{
		int baseLevel = baseLevels.getOrDefault(skill, 1);
		String potion = selectedPotions.getOrDefault(skill, "None");

		int potionBoost = getPotionBoost(skill, baseLevel, potion);
		int prayerBoost = getPrayerBoost(skill, baseLevel + potionBoost);

		int virtualLevel = baseLevel + potionBoost;
		virtualLevel += prayerBoost;

		return virtualLevel;
	}

	private String getEquipmentTooltipText(EquipmentData equipmentData)
	{
		StringBuilder tooltip = new StringBuilder("<html>");
		tooltip.append("<b>").append(equipmentData.getName()).append("</b><br/>");

		EquipmentStats offensive = equipmentData.getOffensive();
		EquipmentStats defensive = equipmentData.getDefensive();
		EquipmentBonuses bonuses = equipmentData.getBonuses();

		boolean hasOffensive = false;
		boolean hasDefensive = false;
		boolean hasBonuses = false;

		if (offensive != null)
		{
			StringBuilder offensiveStats = new StringBuilder();
			if (offensive.getStab() != 0)
			{
				offensiveStats.append("&nbsp;&nbsp;Stab Attack: ").append(offensive.getStab()).append("<br/>");
			}
			if (offensive.getSlash() != 0)
			{
				offensiveStats.append("&nbsp;&nbsp;Slash Attack: ").append(offensive.getSlash()).append("<br/>");
			}
			if (offensive.getCrush() != 0)
			{
				offensiveStats.append("&nbsp;&nbsp;Crush Attack: ").append(offensive.getCrush()).append("<br/>");
			}
			if (offensive.getMagic() != 0)
			{
				offensiveStats.append("&nbsp;&nbsp;Magic Attack: ").append(offensive.getMagic()).append("<br/>");
			}
			if (offensive.getRanged() != 0)
			{
				offensiveStats.append("&nbsp;&nbsp;Ranged Attack: ").append(offensive.getRanged()).append("<br/>");
			}
			if (offensiveStats.length() > 0)
			{
				tooltip.append("Offensive Bonuses:<br/>").append(offensiveStats.toString());
				hasOffensive = true;
			}
		}

		if (defensive != null)
		{
			StringBuilder defensiveStats = new StringBuilder();
			if (defensive.getStab() != 0)
			{
				defensiveStats.append("&nbsp;&nbsp;Stab Defence: ").append(defensive.getStab()).append("<br/>");
			}
			if (defensive.getSlash() != 0)
			{
				defensiveStats.append("&nbsp;&nbsp;Slash Defence: ").append(defensive.getSlash()).append("<br/>");
			}
			if (defensive.getCrush() != 0)
			{
				defensiveStats.append("&nbsp;&nbsp;Crush Defence: ").append(defensive.getCrush()).append("<br/>");
			}
			if (defensive.getMagic() != 0)
			{
				defensiveStats.append("&nbsp;&nbsp;Magic Defence: ").append(defensive.getMagic()).append("<br/>");
			}
			if (defensive.getRanged() != 0)
			{
				defensiveStats.append("&nbsp;&nbsp;Ranged Defence: ").append(defensive.getRanged()).append("<br/>");
			}
			if (defensiveStats.length() > 0)
			{
				tooltip.append("Defensive Bonuses:<br/>").append(defensiveStats.toString());
				hasDefensive = true;
			}
		}

		if (bonuses != null)
		{
			StringBuilder otherBonuses = new StringBuilder();
			if (bonuses.getStr() != 0)
			{
				otherBonuses.append("&nbsp;&nbsp;Melee Strength: ").append(bonuses.getStr()).append("<br/>");
			}
			if (bonuses.getRanged_str() != 0)
			{
				otherBonuses.append("&nbsp;&nbsp;Ranged Strength: ").append(bonuses.getRanged_str()).append("<br/>");
			}
			if (bonuses.getMagic_str() != 0)
			{
				otherBonuses.append("&nbsp;&nbsp;Magic Damage: ").append(bonuses.getMagic_str()).append("<br/>");
			}
			if (bonuses.getPrayer() != 0)
			{
				otherBonuses.append("&nbsp;&nbsp;Prayer Bonus: ").append(bonuses.getPrayer()).append("<br/>");
			}
			if (otherBonuses.length() > 0)
			{
				tooltip.append("Other Bonuses:<br/>").append(otherBonuses.toString());
				hasBonuses = true;
			}
		}

		tooltip.append("</html>");
		return tooltip.toString();
	}

	private boolean isMeleeStyle(String selectedStyle)
	{
		return "Accurate".equals(selectedStyle) ||
			"Aggressive".equals(selectedStyle) ||
			"Defensive".equals(selectedStyle) ||
			"Controlled".equals(selectedStyle);
	}

	private boolean isRangedStyle(String selectedStyle)
	{
		return "Accurate (Ranged)".equals(selectedStyle) ||
			"Rapid".equals(selectedStyle) ||
			"Longrange".equals(selectedStyle);
	}

	private boolean isMagicStyle(String selectedStyle)
	{
		return "Standard".equals(selectedStyle) ||
			"Defensive (Magic)".equals(selectedStyle);
	}

	private void showEquipmentSelectionDialog(JButton button, String slot)
	{
		List<EquipmentData> equipmentList = equipmentMap.get(slot);
		if (equipmentList == null || equipmentList.isEmpty())
		{
			JOptionPane.showMessageDialog(createFrame, "No equipment available for slot: " + slot);
			return;
		}

		// Create a word bank of equipment names
		List<String> equipmentNames = new ArrayList<>();
		for (EquipmentData eq : equipmentList)
		{
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
		if (x + inputPanelWidth > layeredPane.getWidth())
		{
			x = layeredPane.getWidth() - inputPanelWidth;
		}
		if (x < 0)
		{
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

			for (String name : equipmentNames)
			{
				if (text.isEmpty())
				{
					matchingNames.add(name);
				}
				else if (name.toLowerCase().startsWith(text.toLowerCase()))
				{
					matchingNames.add(name);
				}
				else if (name.toLowerCase().contains(text.toLowerCase()))
				{
					otherNames.add(name);
				}
			}
			// Sort the lists alphabetically
			Collections.sort(matchingNames);
			Collections.sort(otherNames);
			// Add matching names first
			for (String name : matchingNames)
			{
				listModel.addElement(name);
			}
			// Then add other names
			for (String name : otherNames)
			{
				listModel.addElement(name);
			}
			if (!listModel.isEmpty())
			{
				suggestionList.setSelectedIndex(0);
			}
		};

		// Call updateSuggestions initially to populate the list
		updateSuggestions.run();

		textField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				updateSuggestions.run();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				updateSuggestions.run();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				updateSuggestions.run();
			}
		});

		suggestionList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					String selectedName = suggestionList.getSelectedValue();
					selectEquipmentForSlot(button, slot, selectedName);
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
			}
		});

		textField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					String selectedName;
					if (!suggestionList.isSelectionEmpty())
					{
						selectedName = suggestionList.getSelectedValue();
					}
					else
					{
						selectedName = textField.getText();
					}
					selectEquipmentForSlot(button, slot, selectedName);
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
				else if (e.getKeyCode() == KeyEvent.VK_DOWN)
				{
					int index = suggestionList.getSelectedIndex();
					if (index < listModel.getSize() - 1)
					{
						suggestionList.setSelectedIndex(index + 1);
						suggestionList.ensureIndexIsVisible(index + 1);
					}
				}
				else if (e.getKeyCode() == KeyEvent.VK_UP)
				{
					int index = suggestionList.getSelectedIndex();
					if (index > 0)
					{
						suggestionList.setSelectedIndex(index - 1);
						suggestionList.ensureIndexIsVisible(index - 1);
					}
				}
				else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					// Remove the overlayPanel when ESC is pressed
					layeredPane.remove(overlayPanel);
					layeredPane.repaint();
				}
			}
		});

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

	{
		baseLevels.put("Attack", 99);
		baseLevels.put("Strength", 99);
		baseLevels.put("Ranged", 99);
		baseLevels.put("Magic", 99);

		// Default potion selections
		selectedPotions.put("Attack", "None");
		selectedPotions.put("Strength", "None");
		selectedPotions.put("Ranged", "None");
		selectedPotions.put("Magic", "None");
	}

	private int getPotionBoost(String skill, int baseLevel, String potion)
	{
		switch (potion)
		{
			case "None":
				return 0;
			case "Attack":
				return (int) Math.floor(baseLevel * 0.10 + 3);
			case "Super Attack":
				return (int) Math.floor(baseLevel * 0.15 + 5);
			case "Zamorak Brew":
				return (int) Math.floor(baseLevel * 0.20 + 2);
			case "Strength":
				return (int) Math.floor(baseLevel * 0.10 + 3);
			case "Super Strength":
				return (int) Math.floor(baseLevel * 0.15 + 5);
			case "Ranging":
				return (int) Math.floor(baseLevel * 0.10 + 4);
			case "Super Ranging":
				return (int) Math.floor(baseLevel * 0.15 + 5);
			case "Magic":
				return 4;
			case "Super Magic":
				return (int) Math.floor(baseLevel * 0.15 + 5);
			case "Overload (Raids)":
				return (int) Math.floor(baseLevel * 0.16 + 6);
			case "Smelling Salts":
				return (int) Math.floor(baseLevel * 0.16 + 11);
			case "Ancient Brew":
				return (int) Math.floor(baseLevel * 0.05 + 2);
			case "Magister's Brew":
				return (int) Math.floor(baseLevel * 0.08 + 3);
			case "Imbued Heart":
				return (int) Math.floor(baseLevel * 0.10 + 1);
			case "Saturated Heart":
				return (int) Math.floor(baseLevel * 0.10 + 4);
			default:
				return 0;
		}
	}

	private int getPrayerBoost(String skill, int baseLevel)
	{
		double boostPercentage = 0.0;
		for (Map.Entry<Prayers, Boolean> entry : selectedPrayers.entrySet())
		{
			if (entry.getValue())
			{
				Prayers prayer = entry.getKey();
				switch (skill)
				{
					case "Attack":
						boostPercentage += prayer.att;
						break;
					case "Strength":
						boostPercentage += prayer.str;
						break;
					case "Ranged":
						boostPercentage += prayer.ranA;
						break;
					case "Magic":
						boostPercentage += prayer.mag;
						break;
				}
			}
		}
		return (int) Math.floor(baseLevel * boostPercentage / 100.0);
	}

	private void selectEquipmentForSlot(JButton button, String slot, String equipmentName)
	{
		List<EquipmentData> equipmentList = equipmentMap.get(slot);
		if (equipmentList == null)
		{
			return;
		}
		EquipmentData selectedEquipmentData = null;
		for (EquipmentData eq : equipmentList)
		{
			if (eq.getName().equalsIgnoreCase(equipmentName))
			{
				selectedEquipmentData = eq;
				break;
			}
		}
		if (selectedEquipmentData == null)
		{
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
				if (width == 0 || height == 0)
				{
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

	private boolean savePreset(String presetName)
	{
		// Validate that a style is selected
		if (selectedStyle == null)
		{
			JOptionPane.showMessageDialog(createFrame, "Please select a combat style.");
			return false;
		}

		// If melee style, ensure attack type is selected
		if (isMeleeStyle(selectedStyle) && selectedAttackType == null)
		{
			JOptionPane.showMessageDialog(createFrame, "Please select an attack type for melee style.");
			return false;
		}
		// Create copies of the selected equipment and prayers
		Map<String, EquipmentData> presetEquipment = new HashMap<>(selectedEquipment);
		Map<Prayers, Boolean> presetPrayers = new HashMap<>(selectedPrayers);
		String presetSelectedStyle = selectedStyle;
		String presetAttackType = selectedAttackType;

		// Copy base levels and selected potions
		Map<String, Integer> presetBaseLevels = new HashMap<>(baseLevels);
		Map<String, String> presetSelectedPotions = new HashMap<>(selectedPotions);

		// Create a new preset object
		Preset preset = new Preset(
			presetName,
			presetEquipment,
			presetPrayers,
			presetSelectedStyle,
			presetBaseLevels,
			presetSelectedPotions,
			presetAttackType
		);

		preset.setVisible(true);

		// Save the preset
		PresetManager.add(presetName, preset);

		// Automatically add the preset to the table
		/*if (!visiblePresets.contains(presetName))
		{
			visiblePresets.add(presetName);
			addPresetRowToTable(presetName, tableModel);
		}*/ //TODO help

		// Save equipment presets to file
		saveEquipmentPresets();
		JOptionPane.showMessageDialog(createFrame, "Preset '" + presetName + "' created successfully.");
		return true;
	}

	private void saveEquipmentPresets()
	{
		try
		{
			// Ensure the dpsutility folder exists
			if (!Files.exists(DPS_UTILITY_FOLDER))
			{
				Files.createDirectories(DPS_UTILITY_FOLDER);
			}

			// Convert the presets map to a list
			List<Preset> presetList = new ArrayList<>(PresetManager.getPresets().values());

			// Serialize to JSON
			String equipmentPresetsJson = gson.toJson(presetList);

			// Write to file
			Files.write(EQUIPMENT_PRESETS_FILE, equipmentPresetsJson.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}

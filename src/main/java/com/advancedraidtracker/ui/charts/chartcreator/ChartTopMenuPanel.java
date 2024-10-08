package com.advancedraidtracker.ui.charts.chartcreator;

import com.advancedraidtracker.AdvancedRaidTrackerConfig;
import com.advancedraidtracker.ui.dpsanalysis.NPCData;
import java.awt.Dimension;
import java.awt.Point;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.*;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import static com.advancedraidtracker.utility.UISwingUtility.*;

@Slf4j
public class ChartTopMenuPanel extends JPanel
{
	JCheckBox weaponCD = getThemedCheckBox("Enforce Weapon CD?");

	JTextField lineText = getThemedTextField();
	JTextField targetNPCField = getThemedTextField(); // New Target NPC text field

	// Auto-complete components
	private DefaultListModel<String> npcListModel = new DefaultListModel<>();
	private JList<String> suggestionList = new JList<>(npcListModel);
	private JScrollPane suggestionScrollPane = new JScrollPane(suggestionList);

	// Data for auto-complete
	private List<String> allNPCs;
	private Map<String, Set<String>> trigramIndex;

	private JLayeredPane layeredPane; // For overlay
	private JPanel overlayPanel; // Panel to hold suggestions
	private ChartCreatorFrame parentFrame; // Reference to the parent frame

	public ChartTopMenuPanel(ChartCreatorFrame parent, AdvancedRaidTrackerConfig config)
	{
		this.parentFrame = parent; // Save reference to parent frame
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBackground(config.primaryDark());
		setOpaque(true);

		JComboBox<Integer> options = getThemedComboBox(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8});
		options.addActionListener(al ->
		{
			parent.setPlayerCount(options.getSelectedIndex() + 1);
		});

		JSpinner startTick = getThemedSpinner(new SpinnerNumberModel(1, 0, 500, 1));

		JSpinner endTick = getThemedSpinner(new SpinnerNumberModel(50, 1, 500, 1));

		startTick.addChangeListener(cl ->
		{
			endTick.setModel(new SpinnerNumberModel((int) endTick.getValue(), (int) startTick.getValue() + 1, 500, 1));
			parent.setStartTick((int) startTick.getValue());
		});

		endTick.addChangeListener(cl ->
		{
			startTick.setModel(new SpinnerNumberModel((int) startTick.getValue(), 0, (int) endTick.getValue() - 1, 1));
			parent.setEndTick((int) endTick.getValue());
		});

		add(getThemedLabel("Players: "));
		add(options);
		add(Box.createRigidArea(new Dimension(20, 0)));

		add(getThemedLabel("Start Tick: "));
		add(startTick);
		add(Box.createRigidArea(new Dimension(20, 0)));

		add(getThemedLabel("End Tick: "));
		add(endTick);
		add(Box.createRigidArea(new Dimension(30, 0)));

		lineText.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				changed();
			}

			public void changed()
			{
				parent.changeLineText(lineText.getText());
			}
		});
		add(getThemedLabel("Line Text: "));
		add(lineText);
		add(Box.createRigidArea(new Dimension(20, 0)));

		// **Add the new "Target NPC: " label and text field here**
		add(getThemedLabel("Target NPC: "));
		add(targetNPCField);
		add(Box.createRigidArea(new Dimension(20, 0)));

		// Initialize auto-complete functionality
		initAutoComplete();

		weaponCD.setSelected(true);
		parent.setEnforceCD(true);
		add(weaponCD);
		add(Box.createRigidArea(new Dimension(250, 0)));
	}

	private void initAutoComplete()
	{
		// Load NPC data for auto-complete
		loadNPCData();

		// Build the trigram index for efficient searching
		buildTrigramIndex();

		// Configure suggestionList
		suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Handle selection events
		suggestionList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				selectSuggestion();
			}
		});

		suggestionList.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					selectSuggestion();
				}
				else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					removeSuggestionPanel();
				}
			}
		});

		targetNPCField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				updateSuggestions();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				updateSuggestions();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				updateSuggestions();
			}
		});

		targetNPCField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_DOWN)
				{
					if (overlayPanel != null)
					{
						suggestionList.requestFocusInWindow();
						suggestionList.setSelectedIndex(0);
					}
				}
				else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					removeSuggestionPanel();
				}
			}
		});
	}

	private void updateSuggestions()
	{
		String text = targetNPCField.getText().toLowerCase().replaceAll("\\s+", "");

		if (text.length() < 3)
		{
			removeSuggestionPanel();
			return;
		}

		// Use SwingWorker to perform search in background
		SwingWorker<Void, List<String>> worker = new SwingWorker<Void, List<String>>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				// Generate trigrams from input text
				Set<String> inputTrigrams = getTrigrams(text);

				// Collect candidate NPC names based on trigrams
				Map<String, Integer> candidateScores = new HashMap<>();

				for (String trigram : inputTrigrams)
				{
					Set<String> matches = trigramIndex.get(trigram);
					if (matches != null)
					{
						for (String match : matches)
						{
							candidateScores.put(match, candidateScores.getOrDefault(match, 0) + 1);
						}
					}
				}

				// Convert candidateScores to a list and sort by score
				List<Map.Entry<String, Integer>> sortedCandidates = new ArrayList<>(candidateScores.entrySet());
				sortedCandidates.sort((a, b) -> b.getValue().compareTo(a.getValue()));

				// Collect the top suggestions
				List<String> suggestions = new ArrayList<>();
				for (Map.Entry<String, Integer> entry : sortedCandidates)
				{
					suggestions.add(entry.getKey());
					if (suggestions.size() >= 10)
					{
						break;
					}
				}

				publish(suggestions);

				return null;
			}

			@Override
			protected void process(List<List<String>> chunks)
			{
				List<String> suggestions = chunks.get(chunks.size() - 1);
				npcListModel.clear();
				for (String suggestion : suggestions)
				{
					npcListModel.addElement(suggestion);
				}
				if (suggestions.isEmpty())
				{
					removeSuggestionPanel();
				}
				else
				{
					showSuggestionPanel();
					suggestionList.setSelectedIndex(0);
				}
			}
		};
		worker.execute();
	}

	private void selectSuggestion()
	{
		String selectedValue = suggestionList.getSelectedValue();
		if (selectedValue != null)
		{
			targetNPCField.setText(selectedValue);
		}
		removeSuggestionPanel();
	}

	private void showSuggestionPanel()
	{
		if (overlayPanel != null)
		{
			return;
		}

		// Get the layered pane of the parent frame
		layeredPane = parentFrame.getLayeredPane();

		// Create overlay panel
		overlayPanel = new JPanel(null);
		overlayPanel.setOpaque(false);
		overlayPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());

		// Position suggestionScrollPane relative to targetNPCField
		Point fieldLocation = targetNPCField.getLocationOnScreen();
		Point panelLocation = parentFrame.getLocationOnScreen();
		int x = fieldLocation.x - panelLocation.x;
		int y = fieldLocation.y - panelLocation.y + targetNPCField.getHeight();

		suggestionScrollPane.setFocusable(false);
		suggestionScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		suggestionScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		suggestionScrollPane.setBounds(x, y, targetNPCField.getWidth(), 150);

		// Add suggestionScrollPane to overlayPanel
		overlayPanel.add(suggestionScrollPane);

		// Add overlayPanel to layeredPane
		layeredPane.add(overlayPanel, JLayeredPane.POPUP_LAYER);
		layeredPane.revalidate();
		layeredPane.repaint();

		// Add mouse listener to detect clicks outside of suggestion list
		overlayPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (!suggestionScrollPane.getBounds().contains(e.getPoint()))
				{
					removeSuggestionPanel();
				}
			}
		});
	}

	private void removeSuggestionPanel()
	{
		if (overlayPanel != null)
		{
			layeredPane.remove(overlayPanel);
			layeredPane.repaint();
			overlayPanel = null;
		}
	}

	private void loadNPCData()
	{
		try
		{
			allNPCs = new ArrayList<>();
			// Load NPC data from the same source as DPSWindow
			String url = "https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/main/cdn/json/monsters.json";
			Gson gson = new Gson();
			NPCData[] npcDataArray = gson.fromJson(new InputStreamReader(new URL(url).openStream()), NPCData[].class);
			for (NPCData data : npcDataArray)
			{
				String npcName = data.getName() + ", " + data.getVersion() + " (" + data.getLevel() + ")";
				allNPCs.add(npcName);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void buildTrigramIndex()
	{
		trigramIndex = new HashMap<>();
		for (String npcName : allNPCs)
		{
			String normalizedName = npcName.toLowerCase().replaceAll("\\s+", "");
			Set<String> trigrams = getTrigrams(normalizedName);
			for (String trigram : trigrams)
			{
				trigramIndex.computeIfAbsent(trigram, k -> new HashSet<>()).add(npcName);
			}
		}
	}

	private Set<String> getTrigrams(String text)
	{
		Set<String> trigrams = new HashSet<>();
		int length = text.length();
		for (int i = 0; i < length - 2; i++)
		{
			String trigram = text.substring(i, i + 3);
			trigrams.add(trigram);
		}
		return trigrams;
	}
}

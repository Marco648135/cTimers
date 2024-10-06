package com.advancedraidtracker.ui;

import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.advancedraidtracker.constants.LogID; // Ensure that this import points to the correct package containing LogID

public class LogViewerFrame extends JFrame {

	private List<LogEntry> logEntries = new ArrayList<>();
	private List<LogEntry> filteredEntries = new ArrayList<>();
	private Set<Integer> hiddenIDs = new HashSet<>();
	private JTable table;
	private DefaultTableModel tableModel;

	private Map<Integer, JCheckBox> idCheckboxes = new HashMap<>();
	private JPanel checkboxPanel;
	private JLabel filteredIdsLabel;

	// Maximum number of arguments found
	private int maxArguments = 0;

	public LogViewerFrame(Path filePath) {
		super("Log Viewer");
		initUI(filePath);
	}

	private void initUI(Path filePath) {
		readCSV(filePath);
		createTable();
		createSidePanel();
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void readCSV(Path filePath) {
		try (BufferedReader br = Files.newBufferedReader(filePath)) {
			String line;
			int lineNumber = 0;
			while ((line = br.readLine()) != null) {
				lineNumber++;
				String[] tokens = line.split(",", -1);
				if (tokens.length < 4) continue; // skip invalid lines

				String timestampStr = tokens[1];
				String idStr = tokens[3];

				try {
					LocalDateTime timestamp = Instant.ofEpochMilli(Long.parseLong(timestampStr))
						.atZone(ZoneId.systemDefault())
						.toLocalDateTime();

					int id = Integer.parseInt(idStr);

					LogID logId = LogID.valueOf(id);

					String logName = logId.getCommonName();
					List<String> stringArgs = logId.getStringArgs();

					List<String> arguments = new ArrayList<>();
					int argStartIndex = 4; // index after the first four tokens
					for (int i = argStartIndex; i < tokens.length; i++) {
						arguments.add(tokens[i]);
					}

					// Update maximum arguments
					if (arguments.size() > maxArguments) {
						maxArguments = arguments.size();
					}

					LogEntry entry = new LogEntry(
						lineNumber,
						timestamp,
						id,
						logId,
						logName,
						arguments,
						line
					);

					logEntries.add(entry);
				} catch (NumberFormatException ex) {
					// Skip invalid lines
					System.err.println("Invalid number on line " + lineNumber);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createTable() {
		// Columns: Line Number, Time, Log Name, Argument1, Argument2, ..., ArgumentN
		List<String> columnNames = new ArrayList<>();
		columnNames.add("Line");
		columnNames.add("Time");
		columnNames.add("Name");
		for (int i = 1; i <= maxArguments; i++) {
			columnNames.add("Argument" + i);
		}

		tableModel = new DefaultTableModel(columnNames.toArray(), 0);
		table = new JTable(tableModel);

		// Format timestamp column
		table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
			@Override
			protected void setValue(Object value) {
				if (value instanceof LocalDateTime) {
					value = ((LocalDateTime) value).format(formatter);
				}
				super.setValue(value);
			}
		});

		// Populate tableModel from logEntries
		updateFilteredEntries();

		// Add table to a scroll pane
		JScrollPane scrollPane = new JScrollPane(table);

		// Add Table to the JFrame
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		// Add context menu to table
		addTableContextMenu();
	}

	private void updateFilteredEntries() {
		// Apply filtering
		filteredEntries = new ArrayList<>();
		for (LogEntry entry : logEntries) {
			if (!hiddenIDs.contains(entry.id)) {
				filteredEntries.add(entry);
			}
		}
		refreshTable();
	}

	private void refreshTable() {
		// Clear table model
		tableModel.setRowCount(0);
		for (LogEntry entry : filteredEntries) {
			List<Object> rowData = new ArrayList<>();
			rowData.add(entry.lineNumber);
			rowData.add(entry.timestamp);
			rowData.add(entry.logName);

			List<String> argNames = entry.logId.getStringArgs();
			List<String> arguments = entry.arguments;
			for (int i = 0; i < maxArguments; i++) {
				if (i < arguments.size()) {
					String argValue = arguments.get(i);
					String cellValue;
					if (i < argNames.size()) {
						String argName = argNames.get(i);
						cellValue = argName + ": " + argValue;
					} else {
						cellValue = argValue;
					}
					rowData.add(cellValue);
				} else {
					rowData.add("");
				}
			}

			tableModel.addRow(rowData.toArray());
		}
	}

	private void createSidePanel() {
		JPanel sidePanel = new JPanel(new BorderLayout());
		sidePanel.setPreferredSize(new Dimension(250, getHeight()));

		// Checkboxes for IDs
		checkboxPanel = new JPanel(new GridLayout(0, 1));

		// IDs: 403, 404, 576, 801
		int[] idsToHide = {403, 404, 576, 801};
		for (int id : idsToHide) {
			LogID logId = LogID.valueOf(id);
			String name = logId.getCommonName();
			JCheckBox checkBox = new JCheckBox("Hide " + name);
			checkBox.addActionListener(e -> toggleHiddenID(id, checkBox.isSelected()));
			checkboxPanel.add(checkBox);
			idCheckboxes.put(id, checkBox);
		}

		// Text field and button to filter IDs
		JPanel filterPanel = new JPanel(new GridLayout(0,1));
		JPanel filterInputPanel = new JPanel();
		JTextField idTextField = new JTextField(5);
		JButton addFilterButton = new JButton("Add Filter");
		filteredIdsLabel = new JLabel("Filtered IDs: ");

		addFilterButton.addActionListener(e -> {
			String text = idTextField.getText().trim();
			try {
				int filterId = Integer.parseInt(text);
				hiddenIDs.add(filterId);
				updateFilteredEntries();
				updateFilteredIdsLabel();
				updateCheckboxes();
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(this, "Invalid ID");
			}
		});

		filterInputPanel.add(new JLabel("ID: "));
		filterInputPanel.add(idTextField);
		filterInputPanel.add(addFilterButton);

		// Unhide all button
		JButton unhideAllButton = new JButton("Unhide All");
		unhideAllButton.addActionListener(e -> {
			hiddenIDs.clear();
			updateFilteredEntries();
			updateFilteredIdsLabel();
			updateCheckboxes();
		});

		filterPanel.add(filterInputPanel);
		filterPanel.add(unhideAllButton);
		filterPanel.add(filteredIdsLabel);

		sidePanel.add(checkboxPanel, BorderLayout.NORTH);
		sidePanel.add(filterPanel, BorderLayout.CENTER);

		add(sidePanel, BorderLayout.EAST);
	}

	private void toggleHiddenID(int id, boolean hide) {
		if (hide) {
			hiddenIDs.add(id);
		} else {
			hiddenIDs.remove(id);
		}
		updateFilteredEntries();
		updateFilteredIdsLabel();
	}

	private void updateFilteredIdsLabel() {
		// Get the names of the hidden IDs
		List<String> hiddenIdNames = new ArrayList<>();
		for (int id : hiddenIDs) {
			LogID logId = LogID.valueOf(id);
			String name = logId.getCommonName();
			hiddenIdNames.add(name + " (" + id + ")");
		}
		filteredIdsLabel.setText("<html>Filtered IDs:<br>" + String.join(", ", hiddenIdNames) + "</html>");
	}

	private void updateCheckboxes() {
		for (Map.Entry<Integer, JCheckBox> entry : idCheckboxes.entrySet()) {
			int id = entry.getKey();
			JCheckBox checkBox = entry.getValue();
			checkBox.setSelected(hiddenIDs.contains(id));
		}
	}

	private void addTableContextMenu() {
		final JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem hideIdItem = new JMenuItem("Hide this ID");
		JMenuItem viewRawItem = new JMenuItem("View raw CSV line");
		popupMenu.add(hideIdItem);
		popupMenu.add(viewRawItem);

		table.setComponentPopupMenu(popupMenu);

		hideIdItem.addActionListener(e -> {
			int row = table.getSelectedRow();
			if (row >= 0) {
				LogEntry selectedEntry = filteredEntries.get(row);
				hiddenIDs.add(selectedEntry.id);
				updateFilteredEntries();
				updateFilteredIdsLabel();
				updateCheckboxes();
			}
		});

		viewRawItem.addActionListener(e -> {
			int row = table.getSelectedRow();
			if (row >= 0) {
				LogEntry selectedEntry = filteredEntries.get(row);
				JOptionPane.showMessageDialog(LogViewerFrame.this, selectedEntry.rawLine, "Raw CSV Line", JOptionPane.INFORMATION_MESSAGE);
			}
		});
	}

	private class LogEntry {
		int lineNumber;
		LocalDateTime timestamp;
		int id;
		LogID logId;
		String logName;
		List<String> arguments;
		String rawLine;

		public LogEntry(int lineNumber, LocalDateTime timestamp, int id, LogID logId, String logName, List<String> arguments, String rawLine) {
			this.lineNumber = lineNumber;
			this.timestamp = timestamp;
			this.id = id;
			this.logId = logId;
			this.logName = logName;
			this.arguments = arguments;
			this.rawLine = rawLine;
		}
	}
}
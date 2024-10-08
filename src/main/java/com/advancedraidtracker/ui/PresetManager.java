package com.advancedraidtracker.ui;

import com.advancedraidtracker.ui.dpsanalysis.Preset;
import static com.advancedraidtracker.ui.charts.ChartIO.gson;
import static com.advancedraidtracker.utility.datautility.DataWriter.PLUGIN_DIRECTORY;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PresetManager {
	public static final Path DPS_UTILITY_FOLDER = Paths.get(PLUGIN_DIRECTORY, "dpsutility");
	public static final Path EQUIPMENT_PRESETS_FILE = DPS_UTILITY_FOLDER.resolve("equipment.presets");
	private static final String initialEquipmentData = "[]";

	@Getter
	private static final Map<String, Preset> presets = new HashMap<>();

	public static void add(String name, Preset preset)
	{
		presets.put(name, preset);
	}

	public static void remove(String name)
	{
		presets.remove(name);
	}

	public static void openAddPresetWindow()
	{
		new AddPresetWindow();
	}

	public static void loadPresets() {
		presets.clear();
		try {
			// Ensure the dpsutility folder exists
			if (!Files.exists(DPS_UTILITY_FOLDER)) {
				Files.createDirectories(DPS_UTILITY_FOLDER);
			}

			// Load equipment presets
			if (!Files.exists(EQUIPMENT_PRESETS_FILE)) {
				// Create the file and write initialEquipmentData
				Files.write(EQUIPMENT_PRESETS_FILE, initialEquipmentData.getBytes(StandardCharsets.UTF_8));
			}
			String equipmentPresetsJson = new String(Files.readAllBytes(EQUIPMENT_PRESETS_FILE), StandardCharsets.UTF_8);
			java.lang.reflect.Type presetListType = new TypeToken<ArrayList<Preset>>(){}.getType();
			List<Preset> loadedPresets = gson.fromJson(equipmentPresetsJson, presetListType);

			for (Preset preset : loadedPresets) {
				presets.put(preset.getName(), preset);
			}
		} catch (IOException e) {
			e.printStackTrace();
			log.error("Error loading presets", e);
		}
	}
}

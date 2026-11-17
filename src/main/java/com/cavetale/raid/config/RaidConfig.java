package com.cavetale.raid.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import lombok.Data;

@Data
public final class RaidConfig implements Serializable {
    /**
     * The main world is supposed to be present in any raid:
     * buildWorlds = {"main": "path_of_main_world"}
     * It is likely the only world in most raids.
     */
    public static final String MAIN_WORLD = "main";

    public static final String MAIN_WAVE = "main";

    /** Map reference name to BuildWorld path. */
    private LinkedHashMap<String, String> buildWorlds = new LinkedHashMap<>();

    private LinkedHashMap<String, ArrayList<WaveConfig>> waves = new LinkedHashMap<>();

    private LinkedHashMap<String, RaidAreaConfig> areas = new LinkedHashMap<>();

    public static RaidConfig named(final String raidName) {
        return RaidConfigs.getInstance().getRaidConfig(raidName);
    }

    public WaveConfig findWaveConfig(final String wavesName, final int index) {
        final ArrayList<WaveConfig> wavesList = waves.get(wavesName);
        if (wavesList == null) return null;
        if (index < 0 || index >= wavesList.size()) return null;
        return wavesList.get(index);
    }

    public int findWaveIndex(final String wavesName, final String waveLabel) {
        final ArrayList<WaveConfig> wavesList = waves.get(wavesName);
        if (wavesList == null) return -1;
        for (int i = 0; i < wavesList.size(); i += 1) {
            if (waveLabel.equals(wavesList.get(i).getLabel())) {
                return i;
            }
        }
        return -1;
    }
}

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
}

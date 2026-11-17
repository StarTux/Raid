package com.cavetale.raid.config;

import com.cavetale.raid.config.validate.ConfigIssue;
import com.cavetale.raid.config.validate.ConfigValidation;
import com.cavetale.raid.config.validate.ValidationContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public final class RaidConfig implements Serializable, ConfigValidation {
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

    /** Collect all issues without modifying the raid. */
    public List<ConfigIssue> validate() {
        final ValidationContext context = new ValidationContext(this);
        validateDeep(context);
        return context.getIssues();
    }

    @Override
    public void validate(final ValidationContext context) {
        if (buildWorlds == null) {
            context.child("buildWorlds").error("World mappings are required");
        } else {
            if (!buildWorlds.containsKey(MAIN_WORLD)) context.child("buildWorlds").error("Main world is required");
            buildWorlds.forEach((name, path) -> {
                    if (name == null || name.isBlank()) context.child("buildWorlds").error("World name is required");
                    if (path == null || path.isBlank()) context.child("buildWorlds").child(String.valueOf(name)).error("World path is required");
                });
        }
        if (waves == null) {
            context.child("waves").error("Wave lists are required");
        } else {
            if (!waves.containsKey(MAIN_WAVE)) context.child("waves").error("Main wave list is required");
            waves.forEach((name, list) -> {
                    if (name == null || name.isBlank()) context.child("waves").error("Wave list name is required");
                    if (list == null) context.child("waves").child(String.valueOf(name)).error("Wave list is required");
                });
        }
        if (areas == null) {
            context.child("areas").error("Area mappings are required");
        } else {
            areas.forEach((name, area) -> {
                    if (name == null || name.isBlank()) context.child("areas").error("Area name is required");
                    if (area == null) context.child("areas").child(String.valueOf(name)).error("Area is required");
                });
        }
    }

    @Override
    public void validateDeep(final ValidationContext context) {
        validate(context);
        if (waves != null) {
            waves.forEach((name, list) -> {
                    if (list == null) return;
                    final ValidationContext listContext = context.child("waves").child(String.valueOf(name));
                    final Set<String> labels = new HashSet<>();
                    for (int i = 0; i < list.size(); i += 1) {
                        final WaveConfig wave = list.get(i);
                        final ValidationContext waveContext = listContext.index(i);
                        if (wave == null) {
                            waveContext.error("Wave is required");
                        } else {
                            wave.validateDeep(waveContext);
                            if (wave.getLabel() != null && !labels.add(wave.getLabel())) {
                                waveContext.child("label").error("Duplicate label '" + wave.getLabel() + "'");
                            }
                        }
                    }
                });
        }
        if (areas != null) {
            areas.forEach((name, area) -> {
                    if (area != null) area.validateDeep(context.child("areas").child(String.valueOf(name)));
                });
        }
    }
}

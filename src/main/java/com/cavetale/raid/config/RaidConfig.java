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
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

@Data
public final class RaidConfig implements Serializable, ConfigValidation {
    /**
     * The main world is supposed to be present in any raid:
     * buildWorlds = {"main": "path_of_main_world"}
     * It is likely the only world in most raids.
     */
    public static final String MAIN_WORLD = "main";

    public static final String MAIN_WAVE = "main";

    private String name;

    private String displayName;

    /** Map reference name to BuildWorld path. */
    private LinkedHashMap<String, String> buildWorlds = new LinkedHashMap<>();

    private LinkedHashMap<String, ArrayList<WaveConfig>> waves = new LinkedHashMap<>();

    private LinkedHashMap<String, RaidAreaConfig> areas = new LinkedHashMap<>();

    public static RaidConfig named(final String raidName) {
        return RaidConfigs.getInstance().getRaidConfig(raidName);
    }

    public Component getDisplayNameComponent() {
        if (displayName == null || displayName.isEmpty()) return text(name);
        return miniMessage().deserialize(displayName);
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
            buildWorlds.forEach((worldName, path) -> {
                    if (worldName == null || worldName.isBlank()) context.child("buildWorlds").error("World name is required");
                    if (path == null || path.isBlank()) context.child("buildWorlds").child(String.valueOf(worldName)).error("World path is required");
                });
        }
        if (waves == null) {
            context.child("waves").error("Wave lists are required");
        } else {
            if (!waves.containsKey(MAIN_WAVE)) context.child("waves").error("Main wave list is required");
            final List<WaveConfig> main = waves.get(MAIN_WAVE);
            if (main != null) {
                final WaveConfig first = findWaveConfig(MAIN_WAVE, 0);
                if (first == null || first.getType() != WaveType.WARMUP) {
                    context.child("waves").child(MAIN_WAVE).index(0).error("Main wave list must start with WARMUP");
                }
            }
            waves.forEach((waveName, list) -> {
                    if (waveName == null || waveName.isBlank()) context.child("waves").error("Wave list name is required");
                    if (list == null) context.child("waves").child(String.valueOf(waveName)).error("Wave list is required");
                });
        }
        if (areas == null) {
            context.child("areas").error("Area mappings are required");
        } else {
            areas.forEach((areaName, area) -> {
                    if (areaName == null || areaName.isBlank()) context.child("areas").error("Area name is required");
                    if (area == null) context.child("areas").child(String.valueOf(areaName)).error("Area is required");
                });
        }
    }

    @Override
    public void validateDeep(final ValidationContext context) {
        validate(context);
        if (waves != null) {
            waves.forEach((waveName, list) -> {
                    if (list == null) return;
                    final ValidationContext listContext = context.child("waves").child(String.valueOf(waveName));
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
            areas.forEach((areaName, area) -> {
                    if (area != null) area.validateDeep(context.child("areas").child(String.valueOf(areaName)));
                });
        }
    }
}

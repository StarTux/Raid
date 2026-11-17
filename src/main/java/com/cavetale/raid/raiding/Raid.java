package com.cavetale.raid.raiding;

import com.cavetale.raid.RaidPlugin;
import com.cavetale.raid.config.RaidConfig;
import com.cavetale.raid.config.WaveConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

/**
 * One run of an active Raid.
 */
@Getter
@RequiredArgsConstructor
public final class Raid {
    /** The name of this unique Raid instance for logging. */
    private final String name;
    private final RaidConfig config;

    private final RaidWorlds worlds = new RaidWorlds(this);
    private final List<WaveProcessor> waveProcessors = new ArrayList<>();

    private boolean enabled;
    private boolean disabled;

    public static Raid named(String raidName) {
        return Raiding.getInstance().getRaids().getRaid(raidName);
    }

    public void enable() {
        if (config.getBuildWorlds().isEmpty()) {
            error("Build worlds are empty!");
            disable();
            return;
        }
        if (!worlds.load(config.getBuildWorlds())) {
            disable();
        }
    }

    /**
     * This is called by RaidWorlds.load() once all worlds are loaded.
     */
    public void worldsDidLoad() {
        for (Map.Entry<String, ArrayList<WaveConfig>> entry : config.getWaves().entrySet()) {
            waveProcessors.add(new WaveProcessor(this, entry.getKey(), entry.getValue()));
        }
        waveProcessors.forEach(WaveProcessor::enable);
        enabled = true;
    }

    public void disable() {
        if (disabled) return;
        disabled = true;
        enabled = false;
        worlds.clear();
    }

    public List<Raider> getRaiders() {
        return Raiders.getInstance().getRaiders(this);
    }

    public void log(String message) {
        RaidPlugin.getInstance().getLogger().info("[" + name + "] " + message);
    }

    public void warn(String message) {
        RaidPlugin.getInstance().getLogger().warning("[" + name + "] " + message);
    }

    public void error(String message) {
        RaidPlugin.getInstance().getLogger().severe("[" + name + "] " + message);
    }

    public void tick() {
        if (!enabled) return;
        waveProcessors.forEach(WaveProcessor::tick);
    }

    public void bring(Player player) {
    }
}

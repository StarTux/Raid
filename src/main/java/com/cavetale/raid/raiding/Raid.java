package com.cavetale.raid.raiding;

import com.cavetale.raid.RaidPlugin;
import com.cavetale.raid.config.RaidConfig;
import com.cavetale.raid.config.WaveConfig;
import com.cavetale.raid.config.validate.ConfigIssue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    private WaveProcessor victoryProcessor;
    private boolean hadPlayersInside;
    private boolean enabled;
    private boolean disabled;

    public static Raid named(String raidName) {
        return Raiding.getInstance().getRaids().getRaid(raidName);
    }

    public void enable() {
        final List<ConfigIssue> issues = config.validate();
        if (!issues.isEmpty()) {
            for (final ConfigIssue issue : issues) error(issue.getPath() + ": " + issue.getMessage());
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
        waveProcessors.forEach(WaveProcessor::disable);
        getRaiders().forEach(this::kickPlayer);
        worlds.clear();
    }

    public List<Raider> getRaiders() {
        return Raiders.getInstance().getRaiders(this);
    }

    public void addRaider(UUID uuid) {
        Raider.of(uuid).setCurrentRaidName(name);
    }

    public void addRaider(Player player) {
        Raider.of(player).setCurrentRaidName(name);
    }

    public void kickPlayer(final Raider raider) {
        if (!name.equals(raider.getCurrentRaidName())) return;
        raider.setCurrentRaidName(null);
        final Player player = raider.getPlayer();
        if (player != null && player.isOnline() && worlds.containsWorld(player.getWorld())) {
            RaidPlugin.getInstance().getLobby().bringPlayer(player);
        }
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

    public void markVictory(final WaveProcessor processor) {
        victoryProcessor = processor;
    }

    public void tick() {
        if (!enabled) return;
        if (victoryProcessor != null) {
            victoryProcessor.tick();
            return;
        }
        final boolean occupied = getRaiders().stream()
            .filter(raider -> !raider.isFallen())
            .map(Raider::getPlayer)
            .anyMatch(player -> player != null && player.isOnline() && !player.isDead() && worlds.containsWorld(player.getWorld()));
        if (occupied) {
            hadPlayersInside = true;
        } else if (hadPlayersInside || waveProcessors.stream().noneMatch(WaveProcessor::isAdmittingPlayers)) {
            disable();
            return;
        }
        for (final WaveProcessor processor : waveProcessors) {
            processor.tick();
            if (victoryProcessor != null || disabled) return;
        }
    }
}

package com.cavetale.raid.raiding;

import com.cavetale.raid.RaidPlugin;
import com.cavetale.raid.config.RaidConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

@RequiredArgsConstructor
public final class Raids {
    private final Map<String, Raid> nameRaidMap = new HashMap<>();
    private int nextRaidSuffix = 1;

    public void enable() {
        Bukkit.getScheduler().runTaskTimer(RaidPlugin.getInstance(), this::tick, 1L, 1L);
    }

    public void disable() {
        nameRaidMap.values().forEach(Raid::disable);
        nameRaidMap.clear();
    }

    public Raid getRaid(String raidName) {
        return nameRaidMap.get(raidName);
    }

    public List<String> getActiveRaidNames() {
        return nameRaidMap.values().stream()
            .filter(raid -> raid.isEnabled() && !raid.isDisabled())
            .map(Raid::getName)
            .sorted()
            .toList();
    }

    public Raid startRaid(RaidConfig raidConfig) {
        final String name = raidConfig.getName() + "_" + String.format("%03d", nextRaidSuffix++);
        final Raid raid = new Raid(name, raidConfig);
        raid.enable();
        if (raid.isDisabled()) {
            return null;
        }
        nameRaidMap.put(name, raid);
        return raid;
    }

    private void tick() {
        nameRaidMap.values().forEach(Raid::tick);
        nameRaidMap.values().removeIf(Raid::isDisabled);
    }
}

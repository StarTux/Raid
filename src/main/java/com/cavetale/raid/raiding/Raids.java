package com.cavetale.raid.raiding;

import com.cavetale.raid.RaidPlugin;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

@RequiredArgsConstructor
public final class Raids {
    private final Map<String, Raid> nameRaidMap = new HashMap<>();

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

    private void tick() {
        nameRaidMap.values().forEach(Raid::tick);
        nameRaidMap.values().removeIf(Raid::isDisabled);
    }
}

package com.cavetale.raid.raiding;

import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Getter
@RequiredArgsConstructor
public final class Raider {
    private final UUID uuid;

    private String currentRaidName;

    private boolean fallen;

    public void setCurrentRaidName(final String raidName) {
        if (!Objects.equals(currentRaidName, raidName)) fallen = false;
        currentRaidName = raidName;
    }

    public void markFallen() {
        fallen = true;
    }

    private boolean enabled;
    private boolean disabled;

    public void enable() {
        enabled = true;
    }

    public void disable() {
        disabled = true;
        enabled = false;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean isOnline() {
        return getPlayer() != null;
    }

    public static Raider of(UUID uuid) {
        return Raiding.getInstance().getRaiders().getRaider(uuid);
    }

    public static Raider of(Player player) {
        return of(player.getUniqueId());
    }


    public Raid getCurrentRaid() {
        if (currentRaidName == null) return null;
        return Raiding.getInstance().getRaids().getRaid(currentRaidName);
    }

    public boolean hasCurrentRaid() {
        return getCurrentRaid() != null;
    }

    public void tick() { }
}

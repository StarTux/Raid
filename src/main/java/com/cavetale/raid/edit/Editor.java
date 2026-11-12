package com.cavetale.raid.edit;

import com.cavetale.raid.config.RaidConfig;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Getter
@RequiredArgsConstructor
public final class Editor {
    private final UUID uuid;

    private String currentRaidName;
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

    public static Editor of(UUID uuid) {
        return Editors.getInstance().getEditor(uuid);
    }

    public static Editor of(Player player) {
        return of(player.getUniqueId());
    }


    public RaidConfig getCurrentRaid() {
        if (currentRaidName == null) return null;
        return RaidConfig.named(currentRaidName);
    }

    public boolean hasCurrentRaid() {
        return getCurrentRaid() != null;
    }

    public void tick() { }
}

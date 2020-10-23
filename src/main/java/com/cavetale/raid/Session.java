package com.cavetale.raid;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

public final class Session {
    private final RaidPlugin plugin;
    private final UUID uuid;
    private String name;
    private String raidName = null;
    // Editing
    @Getter @Setter private int editWave = -1;
    @Getter @Setter private boolean placingRoadblocks = false;

    public Session(final RaidPlugin plugin, final Player player) {
        this.plugin = plugin;
        this.uuid = player.getUniqueId();
        this.name = player.getName();
    }

    public void update(Player player) {
        this.name = player.getName();
    }

    public void setupRun(Raid raid) {
        this.raidName = raid.getWorldName();
    }

    public void tick() {
    }

    public void disable() {
    }
}

package com.cavetale.raid;

import com.cavetale.core.connect.NetworkServer;
import com.cavetale.raid.config.RaidConfigs;
import com.cavetale.raid.edit.Editing;
import com.cavetale.raid.lobby.Lobby;
import com.cavetale.raid.raiding.Raiding;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class RaidPlugin extends JavaPlugin {
    @Getter private static RaidPlugin instance;
    private Raiding raiding;
    private RaidConfigs configs;
    private Lobby lobby;
    private Editing editing;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        final boolean raid = NetworkServer.RAID.isThisServer();
        final boolean creative = NetworkServer.CREATIVE.isThisServer();
        final boolean beta = NetworkServer.BETA.isThisServer();
        if (raid || creative || beta) {
            configs = new RaidConfigs(this);
            configs.enable();
        }
        if (raid || beta) {
            raiding = new Raiding(this);
            raiding.enable();
            lobby = new Lobby(this);
            lobby.enable();
        }
        if (creative || beta) {
            editing = new Editing(this);
            editing.enable();
        }
    }

    @Override
    public void onDisable() {
        if (raiding != null) raiding.disable();
        if (lobby != null) lobby.disable();
        if (editing != null) editing.disable();
        if (configs != null) configs.disable();
    }
}

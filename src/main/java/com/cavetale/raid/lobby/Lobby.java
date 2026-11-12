package com.cavetale.raid.lobby;

import com.cavetale.raid.RaidPlugin;
import com.cavetale.raid.raiding.Raider;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public final class Lobby implements Listener {
    private final RaidPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void disable() {
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> joined(event.getPlayer()));
    }

    public void joined(Player player) {
        if (!player.isOnline()) return;
        if (!player.getWorld().getName().equals(plugin.getConfig().getString("LobbyWorld"))) return;
        final Raider raider = Raider.of(player);
        if (!raider.isEnabled()) return;
        if (raider.getCurrentRaid() == null || !raider.getCurrentRaid().isEnabled()) return;
        raider.getCurrentRaid().bring(player);
    }
}

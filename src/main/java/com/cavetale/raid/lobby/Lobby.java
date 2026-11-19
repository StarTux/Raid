package com.cavetale.raid.lobby;

import com.cavetale.raid.RaidPlugin;
import com.cavetale.raid.raiding.Raid;
import com.cavetale.raid.raiding.Raider;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

@RequiredArgsConstructor
public final class Lobby implements Listener {
    private final RaidPlugin plugin;
    private final Set<UUID> pendingLobbyRespawn = new HashSet<>();

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void disable() {
    }

    public World getLobbyWorld() {
        final String name = plugin.getConfig().getString("LobbyWorld");
        final World world = name != null ? Bukkit.getWorld(name) : null;
        return world != null ? world : Bukkit.getWorlds().get(0);
    }

    public void bringPlayer(final Player player) {
        player.teleport(getLobbyWorld().getSpawnLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final Raider raider = Raider.of(player);
        final Raid raid = raider.getCurrentRaid();
        if (raid == null || !raid.getWorlds().containsWorld(player.getWorld())) return;
        raider.markFallen();
        pendingLobbyRespawn.add(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPlayerRespawn(final PlayerRespawnEvent event) {
        if (pendingLobbyRespawn.remove(event.getPlayer().getUniqueId())) {
            event.setRespawnLocation(getLobbyWorld().getSpawnLocation());
        }
    }
}

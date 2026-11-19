package com.cavetale.raid.raiding;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class RaidListener implements Listener {
    @EventHandler
    private void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final Raider raider = Raider.of(player);
        final Raid raid = raider.getCurrentRaid();
        if (raid != null && raid.getWorlds().containsWorld(player.getWorld())) {
            raid.kickPlayer(raider);
        }
    }
}

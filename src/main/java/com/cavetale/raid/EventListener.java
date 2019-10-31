package com.cavetale.raid;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

@RequiredArgsConstructor
final class EventListener implements Listener {
    final RaidPlugin plugin;

    @EventHandler(priority = EventPriority.MONITOR)
    void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Mob) {
            Mob mob = (Mob) event.getEntity();
            Instance inst = plugin.raidInstance(mob.getWorld());
            if (inst == null) return;
            inst.onDeath(mob);
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        Instance inst = plugin.raidInstance(entity.getWorld());
        if (inst != null
            && entity.getType() == EntityType.VEX
            && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.DEFAULT) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerItemDamage(PlayerItemDamageEvent event) {
        Instance inst = plugin.raidInstance(event.getPlayer().getWorld());
        if (inst != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Instance inst = plugin.raidInstance(event.getPlayer().getWorld());
        if (inst != null && event.isFlying()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerTeleport(PlayerTeleportEvent event) {
        Instance inst = plugin.raidInstance(event.getPlayer().getWorld());
        if (inst != null
            && event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            event.setCancelled(true);
        }
    }
}

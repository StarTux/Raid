package com.cavetale.raid;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

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
}

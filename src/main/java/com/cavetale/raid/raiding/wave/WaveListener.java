package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.raiding.Raid;
import com.cavetale.raid.raiding.Raider;
import com.cavetale.raid.raiding.WaveProcessor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class WaveListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityDeath(final EntityDeathEvent event) {
        final Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        final Raid raid = Raider.of(killer).getCurrentRaid();
        if (raid == null || !raid.isEnabled()) return;
        for (final WaveProcessor processor : raid.getWaveProcessors()) {
            if (processor.getActiveWave() instanceof KillWave wave) wave.onPlayerKill(event.getEntity());
        }
    }
}

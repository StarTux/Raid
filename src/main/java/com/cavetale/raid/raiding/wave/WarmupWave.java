package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveType;
import com.cavetale.raid.config.wave.WarmupWaveConfig;
import com.cavetale.raid.raiding.Raid;
import com.cavetale.raid.raiding.Raider;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class WarmupWave extends TimedWave {
    private final Raid raid;
    private final WarmupWaveConfig config;
    private Location spawn;

    public WarmupWave(final Raid raid, final WarmupWaveConfig config) {
        super(WaveType.WARMUP);
        this.raid = raid;
        this.config = config;
    }

    @Override
    public void enable() {
        spawn = raid.getWorlds().getLocation(config.getSpawn());
    }

    @Override
    public void activate() {
        startTimer(config.getDuration());
    }

    @Override
    public void tick() {
        if (isFinished()) return;
        if (getRemainingDuration().isZero()) {
            for (final Raider raider : raid.getRaiders()) {
                if (raider.isFallen()) continue;
                final Player player = raider.getPlayer();
                if (player == null || !player.isOnline() || !raid.getWorlds().containsWorld(player.getWorld())) {
                    raid.kickPlayer(raider);
                }
            }
            setFinished(true);
            return;
        }
        for (final Raider raider : raid.getRaiders()) {
            if (raider.isFallen()) continue;
            final Player player = raider.getPlayer();
            if (player == null || !player.isOnline()) continue;
            if (raid.getWorlds().containsWorld(player.getWorld())) continue;
            player.teleport(spawn);
        }
        showCountdown(raid);
    }
}

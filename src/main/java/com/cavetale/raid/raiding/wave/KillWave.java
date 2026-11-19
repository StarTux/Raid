package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.MobSpawnConfig;
import com.cavetale.raid.config.WaveType;
import com.cavetale.raid.config.wave.KillWaveConfig;
import com.cavetale.raid.raiding.Raid;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public final class KillWave extends AbstractWave {
    private final Raid raid;
    private final KillWaveConfig config;
    private final List<Spawn> pending = new ArrayList<>();

    @RequiredArgsConstructor
    private static final class Spawn {
        private final Location location;
        private final EntityType type;
        private Entity entity;
    }

    public KillWave(final Raid raid, final KillWaveConfig config) {
        super(WaveType.KILL);
        this.raid = raid;
        this.config = config;
    }

    @Override
    public void enable() { }

    @Override
    public void activate() {
        setFinished(false);
        for (final MobSpawnConfig mob : config.getSpawns()) {
            pending.add(new Spawn(raid.getWorlds().getLocation(mob.getLocation()), mob.resolveType()));
        }
    }

    @Override
    public void tick() {
        if (pending.isEmpty()) setFinished(true);
        for (final Spawn spawn : pending) {
            if (spawn.entity != null && spawn.entity.isValid()) continue;
            final Location location = spawn.location;
            if (!location.isChunkLoaded()) continue;
            final Chunk chunk = location.getChunk();
            if (chunk.getLoadLevel() != Chunk.LoadLevel.ENTITY_TICKING || !chunk.isEntitiesLoaded()) continue;
            spawn.entity = location.getWorld().spawnEntity(location, spawn.type);
            spawn.entity.setPersistent(false);
        }
    }

    public void onPlayerKill(final Entity entity) {
        pending.removeIf(spawn -> entity.equals(spawn.entity));
    }

    @Override
    public void deactivate() {
        for (final Spawn spawn : pending) {
            if (spawn.entity != null) spawn.entity.remove();
        }
        pending.clear();
    }
}

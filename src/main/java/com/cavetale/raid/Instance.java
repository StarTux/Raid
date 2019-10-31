package com.cavetale.raid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

final class Instance {
    final RaidPlugin plugin;
    final Raid raid;
    final World world;
    // Editing
    int editWave = -1;
    // Run
    int waveIndex = -1;
    int aliveMobCount = 0;
    long ticks = 0;
    long waveTicks = 0;
    boolean waveComplete = false;
    final List<SpawnSlot> spawns = new ArrayList<>();
    BossFight bossFight;

    Instance(@NonNull final RaidPlugin plugin,
             @NonNull final Raid raid,
             @NonNull final World world) {
        this.plugin = plugin;
        this.raid = raid;
        this.world = world;
    }

    @RequiredArgsConstructor
    static class SpawnSlot {
        final Spawn spawn;
        Mob mob = null;
        boolean killed = false;

        boolean isPresent() {
            return mob != null
                && mob.isValid();
        }
    }

    Wave getWave(int index) {
        try {
            return raid.waves.get(index);
        } catch (IndexOutOfBoundsException ioobe) {
            return null;
        }
    }

    List<Player> getPlayers() {
        return world.getPlayers().stream()
            .filter(p -> !p.isDead())
            .filter(p -> p.getGameMode() == GameMode.SURVIVAL
                    || p.getGameMode() == GameMode.ADVENTURE)
            .collect(Collectors.toList());
    }

    void onTick() {
        ticks += 1;
        List<Player> players = getPlayers();
        if (players.isEmpty()) {
            if (waveIndex < 0) return;
            clearWave();
            waveIndex = -1;
            waveTicks = 0;
        }
        Wave wave = getWave(waveIndex);
        if (wave == null) {
            waveIndex = 0;
            waveTicks = 0;
            return;
        }
        try {
            tickWave(wave, players);
        } catch (Exception e) {
            e.printStackTrace();
            waveIndex += 1;
            waveTicks = 0;
        }
    }

    void clearWave() {
        for (SpawnSlot slot : spawns) {
            if (slot.isPresent()) slot.mob.remove();
        }
        spawns.clear();
        if (bossFight != null) {
            bossFight.cleanup();
        }
        bossFight = null;
    }

    void setupWave(@NonNull Wave wave, List<Player> players) {
        for (Spawn spawn : wave.spawns) {
            for (int i = 0; i < spawn.amount; i += 1) {
                spawns.add(new SpawnSlot(spawn));
            }
        }
        if (wave.boss != null) {
            bossFight = new BossFight(this, wave.boss);
        }
    }

    void onDeath(@NonNull Mob mob) {
        for (SpawnSlot slot : spawns) {
            if (mob.equals(slot.mob)) {
                slot.killed = true;
                slot.mob = null;
                return;
            }
        }
        if (bossFight != null && mob.equals(bossFight.mob)) {
            bossFight.killed = true;
            bossFight.mob = null;
        }
    }

    void tickWave(Wave wave, List<Player> players) {
        // Wave ticks
        if (waveTicks == 0) setupWave(wave, players);
        waveTicks += 1;
        // Spawnable chunks
        Set<Vec2i> spawnChunks = new HashSet<>();
        for (Player player : players) {
            if (player.isGliding()) player.setGliding(false);
            Location loc = player.getLocation();
            Chunk chunk = loc.getChunk();
            final int cx = chunk.getX();
            final int cz = chunk.getZ();
            final int r = 4;
            for (int z = cz - r; z <= cz + r; z += 1) {
                for (int x = cx - r; x <= cx + r; x += 1) {
                    spawnChunks.add(new Vec2i(x, z));
                }
            }
        }
        aliveMobCount = 0;
        for (SpawnSlot slot : spawns) {
            if (slot.mob != null && slot.mob.isDead()) {
                slot.mob = null;
                slot.killed = true;
            }
            if (!slot.killed) aliveMobCount += 1;
            if (!slot.killed && !slot.isPresent()
                && spawnChunks.contains(slot.spawn.place.getChunk())) {
                Mob mob = slot.spawn.spawn(world);
                if (mob != null) {
                    slot.mob = mob;
                }
            }
            if (slot.isPresent()) {
                findTarget(slot.mob, players);
            }
        }
        if (bossFight != null) {
            if (!bossFight.killed && !bossFight.isPresent()
                && spawnChunks.contains(wave.place.getChunk())) {
                bossFight.mob = bossFight.boss.spawn(wave.place.toLocation(world));
            }
            if (bossFight.isPresent()) {
                bossFight.onTick(wave, players);
                findTarget(bossFight.mob, players);
            }
        }
        // Complete condition
        switch (wave.type) {
        case MOBS: {
            if (aliveMobCount == 0) waveComplete = true;
            if (!waveComplete && (waveTicks % 20) == 0) {
                String msg = "" + ChatColor.YELLOW + "Kill Mobs: " + aliveMobCount;
                for (Player player : players) {
                    player.sendActionBar(msg);
                }
            }
            break;
        }
        case GOAL: {
            for (Player player : players) {
                Location loc = player.getLocation();
                double dx = Math.abs(loc.getX() - wave.place.x);
                double dz = Math.abs(loc.getZ() - wave.place.z);
                double d = Math.max(dx, dz);
                if ((int) d <= wave.radius) {
                    waveComplete = true;
                    break;
                }
            }
            if (!waveComplete
                && (waveTicks % 10) == 0
                && spawnChunks.contains(wave.place.getChunk())) {
                world.spawnParticle(Particle.END_ROD,
                                    wave.place.toLocation(world).add(0, 1, 0),
                                    2, 1, 1, 1, 0.0);
            }
            break;
        }
        case BOSS: {
            if (bossFight == null || bossFight.killed) {
                waveComplete = true;
            } else if (bossFight != null && bossFight.isPresent()) {
                for (Player player : players) {
                    player.sendActionBar("" + ChatColor.RED
                                         + ((int) bossFight.mob.getHealth())
                                         + "/" + bossFight.maxHealth);
                }
            }
            break;
        }
        case WIN: {
            if (waveTicks == 1) {
                String playerNames = players.stream().map(Player::getName)
                    .collect(Collectors.joining(", "));
                plugin.getLogger().info("Raid " + raid.worldName + " defeated: " + playerNames);
                String msg = ChatColor.GOLD + "Dungeon " + raid.displayName
                    + " defeated by " + playerNames + "!";
                for (Player other : plugin.getServer().getOnlinePlayers()) {
                    other.sendMessage(msg);
                }
                for (Player player : players) {
                    player.playSound(player.getEyeLocation(),
                                     Sound.UI_TOAST_CHALLENGE_COMPLETE,
                                     SoundCategory.MASTER,
                                     1.0f, 1.0f);
                    for (String cmd : raid.winCommands) {
                        cmd = cmd.replace("%player%", player.getName());
                        plugin.getLogger().info("Running command: " + cmd);
                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                                                           cmd);
                    }
                }
            }
            if (waveTicks >= 200) {
                World w = plugin.getServer().getWorld("spawn");
                if (w != null) {
                    for (Player player : players) {
                        player.teleport(w.getSpawnLocation());
                    }
                }
            }
            break;
        }
        default: break;
        }
        if (waveComplete) {
            waveIndex += 1;
            waveComplete = false;
            waveTicks = 0;
        } else if ((waveTicks % 20) == 0) {
            Location target = wave.place.toLocation(world);
            for (Player player : players) {
                player.setCompassTarget(target);
            }
        }
    }

    void findTarget(@NonNull Mob mob, @NonNull List<Player> players) {
        if (mob.getTarget() instanceof Player) return;
        Location eye = mob.getLocation();
        double min = Double.MAX_VALUE;
        Player target = null;
        for (Player player : players) {
            if (!mob.hasLineOfSight(player)) continue;
            double dist = player.getLocation().distanceSquared(eye);
            if (dist < min) {
                min = dist;
                target = player;
            }
        }
        if (target != null) {
            mob.setTarget(target);
        }
    }
}

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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ExperienceOrb;
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
    boolean debug;
    final List<WaveInst> waveInsts = new ArrayList<>();

    Instance(@NonNull final RaidPlugin plugin,
             @NonNull final Raid raid,
             @NonNull final World world) {
        this.plugin = plugin;
        this.raid = raid;
        this.world = world;
        for (Wave wave : raid.waves) {
            waveInsts.add(new WaveInst());
        }
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

    static class WaveInst {
        ArmorStand debugEntity;
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
        if (debug) {
            for (int i = 0; i < raid.waves.size(); i += 1) {
                Wave wave = raid.waves.get(i);
                highlightPlace(wave);
                WaveInst waveInst = waveInsts.get(i);
                if (wave.place != null) {
                    if (waveInst.debugEntity == null) {
                        Vec2i c = wave.place.getChunk();
                        if (world.isChunkLoaded(c.x, c.z)) {
                            waveInst.debugEntity = world
                                .spawn(wave.place.toLocation(world).add(0, 1, 0),
                                       ArmorStand.class,
                                       e -> {
                                           e.setGravity(false);
                                           e.setCanTick(false);
                                           e.setCanMove(false);
                                           e.setMarker(true);
                                           e.setCustomNameVisible(true);
                                           e.setVisible(false);
                                           e.setPersistent(false);
                                       });
                        }
                    } else if (!waveInst.debugEntity.isValid()) {
                        waveInst.debugEntity = null;
                    } else {
                        Location loc = wave.place.toLocation(world).add(0, 1, 0);
                        waveInst.debugEntity.teleport(loc);
                        waveInst.debugEntity
                            .setCustomName(ChatColor.GRAY + "#"
                                           + ChatColor.GREEN + ChatColor.BOLD + i
                                           + ChatColor.GRAY + " "
                                           + wave.type.name().toLowerCase());
                    }
                }
            }
        }
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
            bossFight = null;
        }
        if (debug) {
            debug = false;
            updateDebugMode();
        }
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
            bossFight.mob.remove();
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
            if (waveComplete) {
                int totalExp = wave.spawns.size() * 5;
                if (totalExp > 0) {
                    for (Player player : players) {
                        world.spawn(player.getLocation(),
                                    ExperienceOrb.class,
                                    e -> {
                                        e.setExperience(totalExp);
                                    });
                    }
                }
            }
            break;
        }
        case GOAL: {
            int goalCount = 0;
            for (Player player : players) {
                Location loc = player.getLocation();
                double dx = Math.abs(loc.getX() - wave.place.x);
                double dz = Math.abs(loc.getZ() - wave.place.z);
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < wave.radius) {
                    goalCount += 1;
                }
            }
            if (goalCount >= players.size()) {
                waveComplete = true;
            }
            if (waveTicks % 20 == 0) {
                String msg = "" + ChatColor.YELLOW + "Players reached waypoint: "
                    + goalCount + "/" + players.size();
                for (Player player : players) {
                    player.sendActionBar(msg);
                }
            }
            if (!waveComplete && spawnChunks.contains(wave.place.getChunk())) {
                highlightPlace(wave);
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
            clearWave();
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

    void highlightPlace(@NonNull Wave wave) {
        if (wave.place == null) return;
        double radius = wave.radius;
        if (radius == 0) {
            if (ticks % 40 != 0) return;
            Location loc = wave.place.toLocation(world).add(0, 0.25, 0);
            world.spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
        } else {
            double inp = 0.1 * (double) ticks;
            double x = Math.cos(inp) * radius;
            double z = Math.sin(inp) * radius;
            Location loc = wave.place.toLocation(world);
            if (ticks % 2 == 0) {
                Location p1 = loc.clone().add(x, 0.25, z);
                world.spawnParticle(Particle.FLAME, p1, 1, 0, 0, 0, 0);
            } else {
                Location p2 = loc.clone().add(-x, 0.25, -z);
                world.spawnParticle(Particle.FLAME, p2, 1, 0, 0, 0, 0);
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

    void updateDebugMode() {
        if (!debug) {
            for (WaveInst waveInst : waveInsts) {
                if (waveInst.debugEntity != null) {
                    waveInst.debugEntity.remove();
                    waveInst.debugEntity = null;
                }
            }
        }
    }
}

package com.cavetale.raid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

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
    final List<Mob> adds = new ArrayList<>();
    final List<AbstractArrow> arrows = new ArrayList<>();
    final Set<UUID> bossDamagers = new HashSet<>(); // last boss battle
    int secondsLeft;
    int prevGoalCount = 0;
    ArmorStand goalEntity;
    int goalIndex = 0;
    Set<Vec2i> spawnChunks = new HashSet<>();
    static List<ItemBuilder> goalItems = Arrays
        .asList(new ItemBuilder(Material.RED_BANNER),
                new ItemBuilder(Material.YELLOW_BANNER),
                new ItemBuilder(Material.BLACK_BANNER),
                new ItemBuilder(Material.WHITE_BANNER),
                new ItemBuilder(Material.ORANGE_BANNER),
                new ItemBuilder(Material.LIGHT_BLUE_BANNER));

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

    boolean isChunkLoaded(Vec2i chunk) {
        return world.isChunkLoaded(chunk.x, chunk.z);
    }

    void onTick() {
        if (debug) {
            for (int i = 0; i < raid.waves.size(); i += 1) {
                Wave wave = raid.waves.get(i);
                highlightPlace(wave);
                while (waveInsts.size() <= i) waveInsts.add(new WaveInst());
                WaveInst waveInst = waveInsts.get(i);
                if (wave.place != null) {
                    if (waveInst.debugEntity == null) {
                        if (isChunkLoaded(wave.place.getChunk())) {
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
        for (Mob add : adds) {
            if (add.isValid()) add.remove();
        }
        adds.clear();
        if (bossFight != null) {
            bossFight.cleanup();
            bossFight = null;
        }
        if (debug) {
            debug = false;
            updateDebugMode();
        }
        if (goalEntity != null) {
            if (goalEntity.isValid()) goalEntity.remove();
            goalEntity = null;
        }
        for (AbstractArrow arrow : arrows) {
            arrow.remove();
        }
        arrows.clear();
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
            bossFight.onBossDeath();
            bossDamagers.clear();
            bossDamagers.addAll(bossFight.damagers);
            bossFight.killed = true;
            bossFight.mob.remove();
            bossFight.mob = null;
        }
    }

    boolean isAcceptableMobTarget(Entity target) {
        if (target == null) return false;
        switch (target.getType()) {
        case PLAYER:
        case WOLF:
        case CAT:
            return true;
        default:
            return false;
        }
    }

    void tickWave(Wave wave, List<Player> players) {
        // Wave ticks
        if (waveTicks == 0) setupWave(wave, players);
        waveTicks += 1;
        // Spawnable chunks
        spawnChunks.clear();
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
        // Count alive mobs
        aliveMobCount = 0;
        for (SpawnSlot slot : spawns) {
            if (slot.mob != null && !slot.mob.isValid()) {
                slot.mob = null;
            }
            if (!slot.killed) aliveMobCount += 1;
            // Spawn Mob
            if (!slot.killed && !slot.isPresent()
                && spawnChunks.contains(slot.spawn.place.getChunk())) {
                Mob mob = slot.spawn.spawn(world);
                if (mob != null) {
                    slot.mob = mob;
                    if (players.size() > 1) {
                        double mul = 1.0 + 0.25 * (double) (players.size() - 1);
                        double maxHealth = mob
                            .getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                        double health = maxHealth * mul;
                        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
                        mob.setHealth(health);
                    }
                    mob.getWorld().playSound(mob.getEyeLocation(),
                                             Sound.ENTITY_ENDERMAN_TELEPORT, 0.1f, 2.0f);
                }
            }
            if (slot.isPresent() && !isAcceptableMobTarget(slot.mob.getTarget())) {
                Player target = findTarget(slot.mob, players);
                if (target != null) slot.mob.setTarget(target);
            }
        }
        adds.removeIf(add -> !add.isValid());
        aliveMobCount += adds.size();
        // Arrows
        for (AbstractArrow arrow : arrows) {
            if (!arrow.isValid()) continue;
            if (arrow.isInBlock() && arrow.getTicksLived() > 200) {
                arrow.remove();
            }
        }
        arrows.removeIf(arrow -> !arrow.isValid());
        // Boss Fight
        if (bossFight != null) {
            if (!bossFight.killed && !bossFight.isPresent()
                && spawnChunks.contains(wave.place.getChunk())) {
                bossFight.mob = bossFight.spawn(wave.place.toLocation(world));
            }
            if (bossFight.isPresent()) bossFight.onTick(wave, players);
        }
        // Complete condition
        switch (wave.type) {
        case MOBS:
            tickMobs(wave, players);
            break;
        case GOAL:
            tickGoal(wave, players);
            break;
        case BOSS: {
            if (bossFight == null || bossFight.killed) {
                waveComplete = true;
            } else if (bossFight != null && bossFight.isPresent()) {
                if (waveTicks % 20 == 0) {
                    for (Player player : players) {
                        player.sendActionBar("" + ChatColor.RED
                                             + ((int) bossFight.mob.getHealth())
                                             + "/" + bossFight.maxHealth);
                    }
                }
            }
            break;
        }
        case WIN: {
            if (waveTicks == 1) {
                String playerNames = players.stream()
                    .filter(p -> bossDamagers.contains(p.getUniqueId()))
                    .map(Player::getName)
                    .collect(Collectors.joining(", "));
                plugin.getLogger().info("Raid " + raid.worldName + " defeated: " + playerNames);
                String msg = ChatColor.GOLD + "Dungeon " + raid.displayName
                    + " defeated by " + playerNames + "!";
                for (Player other : plugin.getServer().getOnlinePlayers()) {
                    other.sendMessage(msg);
                }
                for (Player player : players) {
                    if (!bossDamagers.contains(player.getUniqueId())) continue;
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
            if (waveTicks >= 1200) {
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

    void tickGoal(Wave wave, List<Player> players) {
        int goalCount = 0;
        List<Player> outList = new ArrayList<>();
        Location waveLoc = wave.place.toLocation(world);
        double radius = wave.radius * wave.radius;
        for (Player player : players) {
            Location loc = player.getLocation();
            if (loc.distanceSquared(waveLoc) <= radius) {
                goalCount += 1;
            } else {
                outList.add(player);
            }
        }
        if (goalCount >= players.size()) {
            waveComplete = true;
            secondsLeft = 60;
            for (Player player : players) {
                player.playSound(player.getEyeLocation(),
                                 Sound.ENTITY_ARROW_HIT_PLAYER,
                                 SoundCategory.MASTER,
                                 0.1f, 0.7f);
            }
        } else if (goalCount > 0) {
            if (secondsLeft > 0 && waveTicks % 20 == 0) {
                secondsLeft -= 1;
            }
        } else {
            secondsLeft = 60;
        }
        if (secondsLeft == 0) {
            for (Player out : outList) {
                removePlayer(out);
            }
        }
        if (goalCount != prevGoalCount || waveTicks % 20 == 0) {
            prevGoalCount = goalCount;
            String timeString = secondsLeft >= 60
                ? " 01:00"
                : String.format(" 00:%02d", secondsLeft);
            String msg = ""
                + ChatColor.GRAY + "Reach the Goal: "
                + ChatColor.WHITE + goalCount
                + ChatColor.GRAY + "/" + players.size()
                + ChatColor.DARK_GRAY + timeString;
            for (Player player : players) {
                player.sendActionBar(msg);
            }
        }
        if (!waveComplete && spawnChunks.contains(wave.place.getChunk())) {
            highlightPlace(wave);
        }
        if (goalEntity != null && !goalEntity.isValid()) goalEntity = null;
        if (!waveComplete && goalEntity == null && isChunkLoaded(wave.place.getChunk())) {
            goalEntity = world.spawn(wave.place.toLocation(world), ArmorStand.class, e -> {
                    e.setSmall(true);
                    e.setCanTick(false);
                    e.setCanMove(false);
                    e.setGravity(false);
                    e.setDisabledSlots(EquipmentSlot.values());
                    e.setHelmet(new ItemBuilder(Material.RED_BANNER).create());
                    e.setGlowing(true);
                    e.setVisible(false);
                    e.setMarker(true);
                    e.setPersistent(false);
                });
        }
        if (goalEntity != null) {
            Location loc = goalEntity.getLocation();
            float yaw = loc.getYaw();
            yaw += 3.6f;
            if (yaw > 360f) yaw -= 360f;
            loc.setYaw(yaw);
            goalEntity.teleport(loc);
            if (ticks % 5 == 0) {
                goalEntity.setHelmet(goalItems.get(goalIndex++).create());
                if (goalIndex >= goalItems.size()) goalIndex = 0;
            }
        }
     }

    void tickMobs(Wave wave, List<Player> players) {
        if (aliveMobCount == 0) {
            waveComplete = true;
            for (Player player : players) {
                player.playSound(player.getEyeLocation(),
                                 Sound.ENTITY_PLAYER_LEVELUP,
                                 SoundCategory.MASTER,
                                 0.1f, 2.0f);
            }
        }
        if (!waveComplete && (waveTicks % 20) == 0) {
            String msg = "" + ChatColor.YELLOW + "Kill all Mobs: " + aliveMobCount;
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
        if (waveTicks > 1200 && waveTicks % 100 == 0) {
            for (SpawnSlot slot : spawns) {
                if (slot.isPresent()) slot.mob.setGlowing(true);
            }
            for (Mob mob : adds) {
                if (mob.isValid()) mob.setGlowing(true);
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

    /**
     * Return the closest visible player within 32 blocks distance.
     * If none exists, pick the closest non-visible player within 16 blocks.
     */
    Player findTarget(@NonNull Mob mob, @NonNull List<Player> players) {
        Location eye = mob.getEyeLocation();
        double minVisible = Double.MAX_VALUE;
        double minBlind = Double.MAX_VALUE;
        Player visible = null;
        Player blind = null;
        final double maxVisible = 32 * 32;
        final double maxBlind = 16 * 16;
        for (Player player : players) {
            double dist = player.getEyeLocation().distanceSquared(eye);
            if (mob.hasLineOfSight(player)) {
                if (dist < minVisible && dist < maxVisible) {
                    visible = player;
                    minVisible = dist;
                }
            } else {
                if (dist < minBlind && dist < maxBlind) {
                    blind = player;
                    minBlind = dist;
                }
            }
        }
        return visible != null ? visible : blind;
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

    void removePlayer(Player player) {
        World w = plugin.getServer().getWorld("spawn");
        if (w == null) w = plugin.getServer().getWorlds().get(0);
        player.teleport(w.getSpawnLocation());
    }
}

package com.cavetale.raid;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.DecayedBoss;
import com.cavetale.raid.enemy.Enemy;
import com.cavetale.raid.enemy.ForgottenBoss;
import com.cavetale.raid.enemy.VengefulBoss;
import com.cavetale.worldmarker.EntityMarker;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;

final class Instance implements Context {
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
    boolean debug;
    final List<WaveInst> waveInsts = new ArrayList<>();
    final List<Mob> adds = new ArrayList<>();
    final List<Enemy> bosses = new ArrayList<>();
    final List<AbstractArrow> arrows = new ArrayList<>();
    final Set<UUID> bossDamagers = new HashSet<>(); // last boss battle
    int secondsLeft;
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
    BossBar bossBar;
    // Skull stuff
    boolean doSkulls;
    Set<Long> scannedChunks = new TreeSet<>();
    SkullLocations skullLocations;
    Set<UUID> skullIds = new HashSet<>(); // found in world
    Map<UUID, String> skulls = new HashMap<>(); // placed, texture
    Map<UUID, String> skullNames = new HashMap<>(); // placed, name
    Map<Vec3i, UUID> placeSkulls = new HashMap<>();

    Instance(@NonNull final RaidPlugin plugin,
             @NonNull final Raid raid,
             @NonNull final World world) {
        this.plugin = plugin;
        this.raid = raid;
        this.world = world;
        for (Wave wave : raid.waves) {
            waveInsts.add(new WaveInst());
        }
        loadSkulls();
    }

    void loadSkulls() {
        doSkulls = false;
        File dir = world.getWorldFolder();
        File file = new File(dir, "skulls.json");
        skullLocations = plugin.json.load(file, SkullLocations.class, SkullLocations::new);
        if (skullLocations == null || skullLocations.skulls.isEmpty()) return;
        file = new File(dir, "skulls.yml");
        ConfigurationSection config = plugin.yaml.load(file);
        skullIds.clear();
        for (String str : config.getStringList("ids")) {
            skullIds.add(UUID.fromString(str));
        }
        skulls.clear();
        skullNames.clear();
        for (Map<?, ?> map : config.getMapList("heads")) {
            String id = (String) map.get("Id");
            String texture = (String) map.get("Texture");
            String name = (String) map.get("Name");
            UUID uuid = UUID.fromString(id);
            skulls.put(uuid, texture);
            skullNames.put(uuid, name);
        }
        if (skulls.isEmpty()) return;
        doSkulls = true;
    }

    @RequiredArgsConstructor
    static class SpawnSlot {
        final Spawn spawn;
        Mob mob = null;
        boolean killed = false;

        boolean isPresent() {
            return mob != null && mob.isValid();
        }
    }

    static class WaveInst {
        ArmorStand debugEntity;
    }

    static class SkullLocations {
        Set<Vec3i> skulls = new HashSet<>();
    }

    Wave getWave(int index) {
        try {
            return raid.waves.get(index);
        } catch (IndexOutOfBoundsException ioobe) {
            return null;
        }
    }

    @Override // Context
    public List<Player> getPlayers() {
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
            if (bossBar != null) {
                bossBar.removeAll();
            }
            if (waveIndex >= 0) {
                clearWave();
                waveIndex = -1;
                waveTicks = 0;
            }
            return;
        }
        if (doSkulls) {
            for (Chunk chunk : world.getLoadedChunks()) {
                long key = chunk.getChunkKey();
                if (scannedChunks.contains(key)) continue;
                scannedChunks.add(key);
                for (BlockState state : chunk.getTileEntities()) {
                    if (state instanceof Skull) {
                        Skull skull = (Skull) state;
                        UUID id = skull.getPlayerProfile().getId();
                        if (id == null || skullIds.contains(id)) {
                            state.getBlock().setType(Material.AIR);
                        }
                    }
                }
                for (Vec3i vec : new ArrayList<>(placeSkulls.keySet())) {
                    if (chunk.getX() != vec.x >> 4) continue;
                    if (chunk.getZ() != vec.z >> 4) continue;
                    Block block = world.getBlockAt(vec.x, vec.y, vec.z);
                    block.setType(Material.AIR);
                    block.setType(Material.PLAYER_HEAD);
                    Skull skull = (Skull) block.getState();
                    UUID id = placeSkulls.get(vec);
                    String texture = skulls.get(id);
                    String name = skullNames.get(id);
                    PlayerProfile profile = plugin.getServer().createProfile(id, name);
                    profile.setProperty(new ProfileProperty("textures", texture));
                    skull.setPlayerProfile(profile);
                    skull.update();
                    placeSkulls.remove(vec);
                }
            }
        }
        List<Player> bossBarPlayers = getBossBar().getPlayers();
        for (Player player : bossBarPlayers) {
            if (!players.contains(player)) {
                bossBar.removePlayer(player);
            }
        }
        for (Player player : players) {
            if (!bossBarPlayers.contains(player)) {
                getBossBar().addPlayer(player);
            }
        }
        if (ticks % 100 == 0) {
            for (Player player : players) {
                if (player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
                    player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
                    player.sendActionBar(ChatColor.YELLOW + "Fire Resistance Removed");
                }
            }
        }
        Wave wave = getWave(waveIndex);
        if (wave == null) {
            waveIndex = 0;
            setupWave();
            if (doSkulls) setupSkulls();
            return;
        }
        try {
            tickWave(wave, players);
            if (waveComplete) {
                clearWave();
                waveIndex += 1;
                setupWave();
            } else if ((waveTicks % 20) == 0) {
                Location target = wave.place.toLocation(world);
                for (Player player : players) {
                    player.setCompassTarget(target);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            waveIndex = -1;
            waveTicks = 0;
        }
    }

    public Wave getCurrentWave() {
        if (waveIndex > raid.waves.size()) return null;
        if (waveIndex < 1) return raid.waves.get(0);
        return getWave(waveIndex - 1);
    }

    void setupSkulls() {
        scannedChunks.clear();
        placeSkulls.clear();
        List<Vec3i> list = new ArrayList<>(skullLocations.skulls);
        Collections.shuffle(list, plugin.random);
        while (list.size() > 100) list.remove(list.size() - 1);
        List<UUID> idList = new ArrayList<>(skulls.keySet());
        for (Vec3i vec : list) {
            UUID id = idList.get(plugin.random.nextInt(idList.size()));
            placeSkulls.put(vec, id);
        }
    }

    void setupWave() {
        waveTicks = 0;
        waveComplete = false;
        Wave wave = getWave(waveIndex);
        if (wave == null) return;
        switch (wave.type) {
        case GOAL:
            secondsLeft = wave.time > 0 ? wave.time : 60;
            break;
        case TIME:
            secondsLeft = wave.time > 0 ? wave.time : 10;
            break;
        case BOSS:
            getBossBar().addFlag(BarFlag.CREATE_FOG);
            getBossBar().addFlag(BarFlag.DARKEN_SKY);
            getBossBar().addFlag(BarFlag.PLAY_BOSS_MUSIC);
            if (wave.boss != null) {
                getBossBar().setTitle(ChatColor.DARK_RED
                                      + wave.boss.type.displayName);
                getBossBar().setProgress(1.0);
            }
            break;
        default: break;
        }
    }

    void clearWave() {
        // Spawns
        for (SpawnSlot slot : spawns) {
            if (slot.isPresent()) slot.mob.remove();
        }
        spawns.clear();
        // Adds
        for (Mob add : adds) {
            if (add.isValid()) add.remove();
        }
        adds.clear();
        // Bosses
        for (Enemy boss : bosses) {
            boss.onRemove();
            boss.remove();
        }
        bosses.clear();
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
        Wave wave = getWave(waveIndex);
        if (wave == null) return;
        if (bossBar != null && wave.type == Wave.Type.BOSS) {
            bossBar.removeFlag(BarFlag.CREATE_FOG);
            bossBar.removeFlag(BarFlag.DARKEN_SKY);
            bossBar.removeFlag(BarFlag.PLAY_BOSS_MUSIC);
        }
    }

    void clear() {
        clearWave();
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    void setupWave(@NonNull Wave wave, List<Player> players) {
        for (Spawn spawn : wave.spawns) {
            for (int i = 0; i < spawn.amount; i += 1) {
                spawns.add(new SpawnSlot(spawn));
            }
        }
        if (wave.boss != null) {
            switch (wave.boss.type) {
            case VENGEFUL:
                bosses.add(new VengefulBoss(plugin, this));
                break;
            case FORGOTTEN:
                bosses.add(new ForgottenBoss(plugin, this));
                break;
            case DECAYED:
            default:
                bosses.add(new DecayedBoss(plugin, this));
            }
        }
    }

    void onDeath(@NonNull Mob mob) {
        for (SpawnSlot slot : spawns) {
            if (mob.equals(slot.mob)) {
                Wave wave = getWave(waveIndex);
                if (wave != null && wave.type != Wave.Type.TIME) {
                    slot.killed = true;
                }
                slot.mob = null;
                return;
            }
        }
    }

    boolean isAcceptableMobTarget(Entity target) {
        if (target == null) return false;
        if (EntityMarker.hasId(target, Enemy.WORLD_MARKER_ID)) return false;
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
                    double multiplier = 1.0 + 0.25 * (double) players.size();
                    // Health
                    AttributeInstance inst = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    double maxHealth = inst.getBaseValue();
                    if (mob.getType() == EntityType.RABBIT) {
                        maxHealth = 20.0;
                    }
                    double health = maxHealth * multiplier;
                    inst.setBaseValue(health);
                    mob.setHealth(health);
                    // Damage
                    if (inst != null) {
                        double damage = inst.getBaseValue();
                        if (mob.getType() == EntityType.BEE) {
                            inst = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                            inst.setBaseValue(5.0);
                        }
                    } else {
                        plugin.getLogger().info("No attack damage: " + mob.getType());
                    }
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
        // Bosses
        bosses.removeIf(Enemy::isDead);
        for (Enemy boss : bosses) {
            if (!boss.isValid()) {
                Location loc = getSpawnLocation();
                if (loc.isChunkLoaded()) {
                    boss.spawn(loc);
                }
            }
        }
        // Complete condition
        switch (wave.type) {
        case MOBS:
            tickMobs(wave, players);
            break;
        case GOAL:
            tickGoal(wave, players);
            break;
        case TIME:
            tickTime(wave, players);
            break;
        case BOSS: {
            double health = 0;
            double maxHealth = 0;
            for (Enemy boss : bosses) {
                health += boss.getHealth();
                maxHealth += boss.getMaxHealth();
            }
            getBossBar().setProgress(health / maxHealth);
            waveComplete = bosses.isEmpty();
            if (!bosses.isEmpty()) {
                Enemy boss = bosses.get(0);
                getBossBar().setTitle(ChatColor.DARK_RED + boss.getDisplayName() + ChatColor.RED + " " + ((int) health));
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
    }


    void tickTime(Wave wave, List<Player> players) {
        if (secondsLeft > 0 && waveTicks % 20 == 0) {
            secondsLeft -= 1;
        }
        if (secondsLeft > 0) {
            String timeString = String.format("%02d:%02d",
                                              secondsLeft / 60, secondsLeft % 60);
            getBossBar().setTitle(ChatColor.RED + timeString);
        } else {
            waveComplete = true;
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
        double perc = (double) secondsLeft / (double) 60;
        getBossBar().setProgress(perc);
        String timeString = String.format(" %02d:%02d",
                                          secondsLeft / 60, secondsLeft % 60);
        String msg = ""
            + ChatColor.BLUE + "Reach the Goal "
            + ChatColor.WHITE + goalCount
            + ChatColor.GRAY + "/" + players.size()
            + ChatColor.GRAY + timeString;
        getBossBar().setTitle(msg);
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
                    e.setCustomName(ChatColor.BLUE + "Goal");
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
        double perc = !spawns.isEmpty()
            ? Math.min(1.0, (double) aliveMobCount
                       / (double) spawns.size())
            : 0.0;
        getBossBar().setProgress(perc);
        getBossBar().setTitle(ChatColor.RED + "Kill all Mobs "
                              + ChatColor.WHITE + aliveMobCount);
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
        return visible != null ? visible : null;
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
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    BossBar getBossBar() {
        if (bossBar != null) return bossBar;
        bossBar = plugin.getServer().createBossBar("raid",
                                                   BarColor.WHITE,
                                                   BarStyle.SOLID);
        bossBar.setVisible(true);
        return bossBar;
    }

    void giveSkulls(Player player) {
        for (UUID uuid : new ArrayList<>(skulls.keySet())) {
            giveSkull(player, uuid);
        }
    }

    void giveSkull(Player player, UUID id) {
        ItemStack item = makeSkull(id);
        player.getInventory().addItem(item);
    }

    ItemStack makeSkull(UUID id) {
        String texture = skulls.get(id);
        String name = skullNames.get(id);
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        PlayerProfile profile = plugin.getServer().createProfile(id, name);
        profile.setProperty(new ProfileProperty("textures", texture));
        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);
        return item;
    }

    void interact(Player player, Block block) {
        if (block.getType() == Material.PLAYER_HEAD) {
            Skull skull = (Skull) block.getState();
            UUID id = skull.getPlayerProfile().getId();
            String texture = skulls.get(id);
            if (texture == null) return;
            block.setType(Material.AIR);
            world.dropItem(block.getLocation().add(0.5, 0.5, 0.5),
                           makeSkull(id));
        }
    }

    @Override // Context
    public Location getSpawnLocation() {
        return getCurrentWave().getPlace().toLocation(world);
    }

    @Override // Context
    public void registerNewEnemy(Enemy enemy) {
        // TODO
    }

    @Override // Context
    public void registerTemporaryEntity(Entity entity) {
        if (entity instanceof Mob) {
            adds.add((Mob) entity);
        } else if (entity instanceof AbstractArrow) {
            arrows.add((AbstractArrow) entity);
        }
    }

    @Override // Context
    public int countTemporaryEntities(Class<? extends Entity> type) {
        int count = 0;
        for (Mob mob : adds) {
            if (type.isInstance(mob)) count += 1;
        }
        return count;
    }

    @Override // Context
    public void onDeath(Enemy enemy) {
        if (bosses.contains(enemy)) {
            for (Player player : enemy.getPlayerDamagers()) {
                for (ItemStack item : enemy.getDrops()) {
                    for (ItemStack drop : player.getInventory().addItem(item).values()) {
                        player.getWorld().dropItem(player.getEyeLocation(), drop).setPickupDelay(0);
                    }
                }
            }
            enemy.onRemove();
            bosses.remove(enemy);
            if (bosses.isEmpty()) {
                bossDamagers.clear();
                bossDamagers.addAll(enemy.getDamagers());
                waveComplete = true;
            }
        }
    }
}

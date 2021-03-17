package com.cavetale.raid;

import com.cavetale.enemy.Context;
import com.cavetale.enemy.Enemy;
import com.cavetale.enemy.EnemyType;
import com.cavetale.enemy.boss.SadisticVampireBoss;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.MytemsPlugin;
import com.cavetale.mytems.item.AculaItemSet;
import com.cavetale.raid.struct.Cuboid;
import com.cavetale.raid.struct.Vec3i;
import com.cavetale.raid.util.Fireworks;
import com.cavetale.raid.util.Gui;
import com.cavetale.raid.util.Text;
import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import com.destroystokyo.paper.Title;
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
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
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
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;

final class Instance implements Context {
    @Getter final RaidPlugin plugin;
    final String name;
    @Getter private Raid raid;
    @Getter private World world;
    @Getter private Phase phase = Phase.PRE_WORLD;
    @Getter int waveIndex = -1;
    int aliveMobCount = 0;
    int ticks = 0;
    int waveTicks = 0;
    int warmupTicks = 0;
    int roadblockIndex = 0; // timed roadblock removal
    boolean waveComplete = false;
    final List<Enemy> spawns = new ArrayList<>();
    boolean debug;
    List<WaveInst> waveInsts = null; // debug
    final List<Mob> adds = new ArrayList<>();
    final List<Enemy> bosses = new ArrayList<>();
    final List<AbstractArrow> arrows = new ArrayList<>();
    final Set<UUID> bossFighters = new HashSet<>(); // last boss battle
    final Set<UUID> alreadyJoined = new HashSet<>();
    final Map<UUID, WinReward> winRewards = new HashMap<>();
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
    private String sidebarInfo = "";
    Random random = new Random();
    Map<String, EscortMarker> escorts = new HashMap<>();
    private Highscore damageHighscore = new Highscore();
    Map<Block, CustomBlock> customBlocks = new HashMap<>();
    // Skull stuff
    boolean doSkulls;
    Set<Long> scannedChunks = new TreeSet<>();
    SkullLocations skullLocations;
    Set<UUID> skullIds = new HashSet<>(); // found in world
    Map<UUID, String> skulls = new HashMap<>(); // placed, texture
    Map<UUID, String> skullNames = new HashMap<>(); // placed, name
    Map<Vec3i, UUID> placeSkulls = new HashMap<>();

    Instance(final RaidPlugin plugin, final Raid raid) {
        this.plugin = plugin;
        this.name = raid.getWorldName();
        this.raid = raid;
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
            String theName = (String) map.get("Name");
            UUID uuid = UUID.fromString(id);
            skulls.put(uuid, texture);
            skullNames.put(uuid, theName);
        }
        if (skulls.isEmpty()) return;
        doSkulls = true;
    }

    /**
     * Enter the RUN phase.
     * Called when the run is started by any eligible player entering.
     */
    public void setupRun() {
        for (int i = raid.waves.size() - 1; i >= 0; i -= 1) {
            Wave wave = raid.waves.get(i);
            for (Roadblock roadblock: wave.getRoadblocks()) {
                roadblock.block(world);
            }
        }
        phase = Phase.WARMUP;
        warmupTicks = 0;
        getBossBar().setTitle(ChatColor.DARK_RED + "Preparing the Raid");
        getBossBar().setColor(BarColor.RED);
    }

    public void startRun() {
        ticks = 0;
        waveIndex = 0;
        world.setGameRule(GameRule.MOB_GRIEFING, true);
        setupWave();
        damageHighscore.reset();
        alreadyJoined.clear();
        phase = Phase.RUN;
    }

    public void resetRun() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        if (waveIndex >= 0) {
            clearWave();
            waveIndex = -1;
            waveTicks = 0;
        }
        for (EscortMarker escortMarker : escorts.values()) {
            escortMarker.remove();
        }
        escorts.clear();
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        damageHighscore.reset();
        alreadyJoined.clear();
        winRewards.clear();
    }

    /**
     * Skip to a specific waves and ensure that previous waves are
     * unblocked while later ones are blocked.
     *
     * BUGS: This might break non-linear raids; intended mostly for
     * debug!
     */
    public void skipWave(int newWave) {
        if (waveIndex >= 0) {
            clearWave();
        }
        waveIndex = newWave;
        waveComplete = false;
        waveTicks = 0;
        for (int i = raid.waves.size() - 1; i >= 0; i -= 1) {
            Wave wave = raid.waves.get(i);
            for (Roadblock rb : wave.getRoadblocks()) {
                if (i < newWave) {
                    rb.unblock(world);
                } else {
                    rb.block(world);
                }
            }
        }
    }

    public void onWorldLoaded(World theWorld) {
        world = theWorld;
        if (phase != Phase.PRE_WORLD) return;
        phase = Phase.STANDBY;
    }

    public void onWorldUnload() {
        if (phase != Phase.STANDBY) {
            resetRun();
        }
        world = null;
        phase = Phase.PRE_WORLD;
    }

    public void joinPlayer(Player player) {
        if (phase == Phase.STANDBY) {
            setupRun();
        }
        if ("CastleRaid".equals(raid.worldName)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + player.getName());
        }
        Wave wave = getCurrentWave();
        player.teleport(wave.place.toLocation(getWorld()));
        UUID uuid = player.getUniqueId();
        if (!alreadyJoined.contains(uuid)) {
            alreadyJoined.add(uuid);
        }
        Wave wave2 = getWave(waveIndex);
        if (wave2 != null && wave2.type == Wave.Type.BOSS) {
            bossFighters.add(uuid);
        }
    }

    public void onDeath(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setGameMode(GameMode.ADVENTURE);
                for (PotionEffectType pet : PotionEffectType.values()) {
                    player.removePotionEffect(pet);
                }
                player.setFallDistance(0);
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            });
    }

    void clear() {
        if (phase == Phase.RUN || phase == Phase.WARMUP) {
            resetRun();
        }
        clearDebug();
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

    public void tick() {
        if (world == null) return;
        if (debug) tickDebug();
        switch (phase) {
        case STANDBY: tickStandby(); break;
        case WARMUP: tickWarmup(); break;
        case RUN: tickRun(); break;
        default: throw new IllegalStateException("phase=" + phase);
        }
        ticks += 1;
    }

    void tickStandby() {
        List<Player> players = getPlayers();
        if (!players.isEmpty()) {
            setupRun();
        }
    }

    void tickWarmup() {
        List<Player> players = getPlayers();
        if (players.isEmpty()) {
            resetRun();
            phase = Phase.STANDBY;
            return;
        }
        warmupTicks += 1;
        final int warmupDuration = 20 * 60;
        double progress = Math.max(0, Math.min(1, (double) warmupTicks / (double) warmupDuration));
        getBossBar().setProgress(progress);
        tickBossBar(players);
        if (warmupTicks >= warmupDuration) {
            startRun();
        }
    }

    void tickRun() {
        List<Player> players = getPlayers();
        if (players.isEmpty()) {
            resetRun();
            phase = Phase.STANDBY;
            return;
        }
        if (doSkulls) tickSkulls();
        tickBossBar(players);
        Wave wave = getWave(waveIndex);
        if (wave == null) {
            plugin.getLogger().info("Wave not found: " + waveIndex + ". Resetting.");
            resetRun();
            phase = Phase.STANDBY;
            return;
        }
        try {
            tickWave(wave, players);
            if (waveComplete) {
                clearWave();
                onWaveComplete(wave);
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

    void tickDebug() {
        if (waveInsts == null) waveInsts = new ArrayList<>();
        for (int i = 0; i < raid.waves.size(); i += 1) {
            Wave wave = raid.waves.get(i);
            highlightPlace(wave);
            if (waveInsts.size() <= i) waveInsts.add(new WaveInst());
            WaveInst waveInst = waveInsts.get(i);
            if (wave.place != null) {
                if (waveInst.debugEntity == null || !waveInst.debugEntity.isValid()) {
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

    void clearDebug() {
        if (waveInsts == null) return;
        for (WaveInst waveInst : waveInsts) {
            if (waveInst.debugEntity != null) {
                waveInst.debugEntity.remove();
                waveInst.debugEntity = null;
            }
        }
        waveInsts.clear();
    }

    void tickSkulls() {
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
                String theName = skullNames.get(id);
                PlayerProfile profile = plugin.getServer().createProfile(id, theName);
                profile.setProperty(new ProfileProperty("textures", texture));
                skull.setPlayerProfile(profile);
                skull.update();
                placeSkulls.remove(vec);
            }
        }
    }

    void tickBossBar(List<Player> players) {
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
        Collections.shuffle(list, random);
        while (list.size() > 100) list.remove(list.size() - 1);
        List<UUID> idList = new ArrayList<>(skulls.keySet());
        for (Vec3i vec : list) {
            UUID id = idList.get(random.nextInt(idList.size()));
            placeSkulls.put(vec, id);
        }
    }

    void setupWave() {
        waveTicks = 0;
        waveComplete = false;
        roadblockIndex = 0;
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
            for (Player player : getPlayers()) {
                bossFighters.add(player.getUniqueId());
            }
            if (!bosses.isEmpty()) {
                Title title = new Title(bosses.get(0).getDisplayName(), ChatColor.DARK_RED + "Boss Fight", 10, 20, 10);
                for (Player player : getPlayers()) {
                    player.sendTitle(title);
                }
            }
            break;
        case TITLE:
            break;
        default: break;
        }
        for (Escort escort : wave.getEscorts()) {
            EscortMarker escortMarker = escorts.get(escort.getName());
            boolean placeIsSpecified = false;
            Location destination = null;
            if (escort.getPlace() != null) {
                destination = escort.getPlace().toLocation(world);
                placeIsSpecified = true;
            } else if (wave.getPlace() != null) {
                destination = wave.getPlace().toLocation(world);
            }
            if (destination != null) {
                if (escortMarker == null || !escortMarker.isValid()) {
                    if (escortMarker != null) escortMarker.remove();
                    // (Re)spawn
                    Villager villager = world.spawn(destination,  Villager.class, e -> {
                            e.setProfession(Villager.Profession.FARMER);
                            e.setVillagerType(Villager.Type.PLAINS);
                            e.setVillagerLevel(5);
                            e.setPersistent(false);
                        });
                    escortMarker = new EscortMarker(plugin, villager).enable();
                    escorts.put(escort.getName(), escortMarker);
                } else {
                    // Walk
                    if (placeIsSpecified) {
                        escortMarker.pathTo(destination);
                    }
                }
            }
            if (escortMarker != null && escortMarker.isValid()) {
                escortMarker.setDialogue(escort.getDialogue());
                escortMarker.setDialogueIndex(0);
                escortMarker.setDialogueCooldown(40);
            }
            if (escort.isDisappear()) {
                escortMarker.remove();
                escorts.remove(escort.getName());
            }
        }
    }

    void clearWave() {
        // Spawns
        for (Enemy enemy : spawns) {
            enemy.remove();
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
        // Custom blocks
        for (CustomBlock customBlock : customBlocks.values()) {
            customBlock.remove();
        }
        customBlocks.clear();
    }

    void onWaveComplete(Wave wave) {
        if (wave.type != Wave.Type.ROADBLOCK) {
            for (Roadblock rb : wave.getRoadblocks()) {
                rb.unblock(world, true);
            }
        }
    }

    /**
     * Called on the 0-tick by tickWave().
     */
    void setupWave(@NonNull Wave wave, List<Player> players) {
        for (Spawn spawn : wave.getSpawns()) {
            int amount = spawn.amount;
            if (players.size() > 4) amount += (players.size() - 5) / 2 + 1;
            for (int i = 0; i < amount; i += 1) {
                Enemy enemy = spawn.createEnemy(this);
                if (enemy == null) {
                    plugin.getLogger().warning(name + ": invalid spawn: " + spawn.getShortInfo());
                    continue;
                }
                enemy.setSpawnLocation(spawn.place.toLocation(world));
                spawns.add(enemy);
            }
        }
        if (wave.boss != null) {
            bosses.add(wave.boss.type.create(this));
        }
        switch (wave.type) {
        case TITLE:
            Title title = new Title(Text.colorize(raid.displayName), "", 20, 40, 20);
            for (Player player : players) {
                player.sendTitle(title);
            }
            break;
        case WIN: {
            for (UUID uuid : bossFighters) {
                winRewards.put(uuid, new WinReward());
            }
            String playerNames = Bukkit.getOnlinePlayers().stream()
                .filter(p -> bossFighters.contains(p.getUniqueId()))
                .map(Player::getName)
                .collect(Collectors.joining(", "));
            plugin.getLogger().info("Raid " + raid.worldName + " defeated: " + playerNames);
            String msg = ChatColor.GOLD + "Dungeon "
                + Text.colorize(raid.displayName)
                + ChatColor.GOLD + " defeated by " + playerNames + "!";
            for (Player other : plugin.getServer().getOnlinePlayers()) {
                other.sendMessage(msg);
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!bossFighters.contains(player.getUniqueId())) continue;
                player.playSound(player.getEyeLocation(),
                                 Sound.UI_TOAST_CHALLENGE_COMPLETE,
                                 SoundCategory.MASTER,
                                 1.0f, 1.0f);
            }
            Cuboid bossChestRegion = wave.regions.get("boss_chest");
            if (bossChestRegion != null) {
                Block block = bossChestRegion.getMin().toBlock(getWorld());
                CustomBlock customBlock = new CustomBlock(block, Mytems.BOSS_CHEST);
                customBlocks.put(block, customBlock);
                customBlock.place();
            }
            break;
        }
        default:
            break;
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
        for (Enemy enemy : spawns) {
            if (enemy.isAlive()) {
                aliveMobCount += 1;
                if (!enemy.isValid()) enemy.spawn();
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
        for (Enemy boss : bosses) {
            if (boss.isDead()) {
                Location location = boss.getLocation();
                Firework firework = Fireworks.spawnFirework(location);
                firework.setMetadata("raid:nodamage", new FixedMetadataValue(plugin, true));
                firework.detonate();
            }
        }
        bosses.removeIf(Enemy::isDead);
        for (Enemy boss : bosses) {
            if (!boss.isValid()) {
                Location loc = getSpawnLocation();
                if (loc.isChunkLoaded()) {
                    boss.spawn(loc);
                }
            }
        }
        // Escorts
        for (EscortMarker escortMarker : escorts.values()) {
            escortMarker.tick(players);
        }
        // Adds
        for (Mob mob : adds) {
            if (!(mob.getTarget() instanceof Player)) {
                Player target = SpawnEnemy.findTarget(mob, this);
                if (target != null) mob.setTarget(target);
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
                if (boss instanceof SadisticVampireBoss) {
                    ((SadisticVampireBoss) boss).tickBossFight();
                }
                health += boss.getHealth();
                maxHealth += boss.getMaxHealth();
            }
            if (maxHealth > 0) {
                getBossBar().setProgress(health / maxHealth);
            }
            waveComplete = bosses.isEmpty();
            if (!bosses.isEmpty()) {
                Enemy boss = bosses.get(0);
                getBossBar().setTitle(ChatColor.DARK_RED + boss.getDisplayName());
                sidebarInfo = ChatColor.BLUE + "Boss Health " + ChatColor.RED + ((int) health);
            }
            break;
        }
        case ROADBLOCK: {
            List<Roadblock> roadblocks = wave.getRoadblocks();
            if (roadblockIndex >= roadblocks.size()) {
                waveComplete = true;
            } else {
                Roadblock rb = roadblocks.get(roadblockIndex++);
                rb.unblock(world, true);
            }
            break;
        }
        case WIN: {
            if (waveTicks >= 2 * 20 * 60) {
                World w = Bukkit.getWorlds().get(0);
                if (w != null) {
                    for (Player player : players) {
                        player.teleport(w.getSpawnLocation());
                    }
                }
            } else {
                for (CustomBlock customBlock : customBlocks.values()) {
                    switch (customBlock.mytems) {
                    case BOSS_CHEST:
                        if ((waveTicks % 5) == 0) {
                            Location location = customBlock.block.getLocation().add(0.5, 0.5, 0.5);
                            getWorld().spawnParticle(Particle.END_ROD, location, 2, 0.25, 0.25, 0.25, 0.1);
                        }
                        break;
                    default:
                        break;
                    }
                }
            }
            break;
        }
        case TITLE:
            waveComplete = true;
            break;
        default: break;
        }
    }

    public Gui openRewardGui(Player player) {
        WinReward winReward = winRewards.get(player.getUniqueId());
        if (winReward == null || winReward.complete) return null;
        Gui gui = new Gui(plugin);
        Component component = Component.text("\uE001\uE101\uE001\uE001\uE001\uE001\uE001\uE001\uE001"
                                             + "\uE001\uE001\uE001\uE001\uE001\uE001\uE001\uE001\uE001"
                                             + "\uE001\uE001\uE001\uE001")
            .style(Style.style().color(NamedTextColor.WHITE).font(Key.key("cavetale:default")))
            .append(Component.text(Text.colorize(raid.displayName))
                    .style(Style.style().color(NamedTextColor.WHITE).font(Style.DEFAULT_FONT)));
        gui.title(component);
        gui.size(3 * 9);
        updateRewardGui(gui, winReward, player);
        player.playSound(player.getEyeLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.MASTER, 0.5f, 1.0f);
        gui.onClose(event -> player.playSound(player.getEyeLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.MASTER, 0.5f, 1.0f));
        gui.onTick(() -> updateRewardGui(gui, winReward, player));
        gui.open(player);
        return gui;
    }

    ItemStack getPlaceholder(int index) {
        ItemStack itemStack = new ItemStack(Material.SHULKER_BOX);
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text("Click to open").color(NamedTextColor.LIGHT_PURPLE));
        meta.lore(Arrays.asList(Component.text("You will get a random item.").color(NamedTextColor.GRAY),
                                Component.text("Good luck!").color(NamedTextColor.GRAY)));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    List<LootDrop> getRewardItems(int index) {
        switch (index) {
        case 0:
            return Arrays.asList(new LootDrop(new ItemStack(Material.BLAZE_ROD, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.BONE, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.CLAY_BALL, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.FEATHER, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.FLINT, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.GUNPOWDER, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.INK_SAC, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.LEATHER, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.NETHER_BRICK, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.PAPER, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.RABBIT_HIDE, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.SLIME_BALL, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.STRING, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.HONEYCOMB, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.PRISMARINE_SHARD, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.PUMPKIN, 1), 63, 1),
                                 new LootDrop(new ItemStack(Material.SUGAR_CANE, 1), 63, 1));
        case 1:
            return Arrays.asList(new LootDrop(new ItemStack(Material.DIAMOND, 1), 15, 4),
                                 new LootDrop(new ItemStack(Material.EMERALD, 2), 30, 4),
                                 new LootDrop(new ItemStack(Material.IRON_INGOT, 4), 60, 4),
                                 new LootDrop(new ItemStack(Material.GOLD_INGOT, 4), 60, 4),
                                 new LootDrop(new ItemStack(Material.LAPIS_LAZULI, 2), 30, 4),
                                 new LootDrop(new ItemStack(Material.COAL, 4), 60, 4),
                                 new LootDrop(new ItemStack(Material.QUARTZ, 4), 60, 4),
                                 new LootDrop(new ItemStack(Material.ANCIENT_DEBRIS, 1), 0, 1));
        case 2:
            return Arrays.asList(new LootDrop(new ItemStack(Material.COD, 1), 0, 30),
                                 new LootDrop(new ItemStack(Material.TROPICAL_FISH, 1), 0, 30),
                                 new LootDrop(new ItemStack(Material.SALMON, 1), 0, 30),
                                 new LootDrop(new ItemStack(Material.PUFFERFISH, 1), 0, 30),
                                 new LootDrop(new ItemStack(Material.NETHER_STAR, 1), 0, 1),
                                 new LootDrop(new ItemStack(Material.CONDUIT, 1), 0, 1),
                                 new LootDrop(new ItemStack(Material.DRAGON_EGG, 1), 0, 1),
                                 new LootDrop(new ItemStack(Material.DRAGON_HEAD, 1), 0, 1),
                                 new LootDrop(new ItemStack(Material.BEACON, 1), 0, 1));
        case 3:
            return Arrays.asList(new LootDrop(Mytems.KITTY_COIN.getMytem().getItem(), 2, 1));
        default:
            return null;
        }
    }

    void updateRewardGui(Gui gui, WinReward winReward, Player player) {
        for (int i = 0; i < 4; i += 1) {
            updateRewardSlot(gui, winReward, player, i);
        }
    }

    void updateRewardSlot(Gui gui, WinReward winReward, Player player, int index) {
        final int max = 100;
        int slot = 10 + index * 2;
        int state = winReward.unlocked.get(index);
        if (state == 0) {
            gui.setItem(slot, getPlaceholder(index), click -> {
                    if (winReward.unlocked.get(index) != state) return;
                    winReward.unlocked.set(index, 1);
                });
        } else if (state < max) {
            switch (state) {
            case 0: case 1: case 2: case 3: case 4: case 5:
            case 6: case 8: case 10: case 12: case 14: case 16: case 18: case 20:
            case 23: case 26: case 29: case 32: case 35: case 38: case 41:
            case 45: case 49: case 53: case 57: case 61: case 65: case 69:
            case 74: case 79: case 84: case 89: case 94:
                List<LootDrop> list = getRewardItems(index);
                LootDrop loot = list.get(random.nextInt(list.size()));
                ItemStack itemStack = loot.itemStack.clone();
                itemStack.setAmount(itemStack.getAmount() + random.nextInt(loot.bonusAmount + 1));
                gui.setItem(slot, itemStack);
                player.playSound(player.getEyeLocation(), Sound.BLOCK_LEVER_CLICK, SoundCategory.MASTER, 0.5f, 2.0f);
                break;
            default:
                break;
            }
            winReward.unlocked.set(index, state + 1);
        } else if (state == max) {
            List<LootDrop> list = getRewardItems(index);
            List<LootDrop> list2 = new ArrayList<>();
            for (LootDrop it : list) {
                for (int i = 0; i < it.weight; i += 1) list2.add(it);
            }
            LootDrop lootDrop = list2.get(random.nextInt(list2.size()));
            ItemStack itemStack = lootDrop.itemStack.clone();
            itemStack.setAmount(itemStack.getAmount() + random.nextInt(lootDrop.bonusAmount + 1));
            gui.setItem(slot, itemStack);
            for (ItemStack drop : player.getInventory().addItem(itemStack).values()) {
                player.getWorld().dropItem(player.getEyeLocation(), drop);
            }
            winReward.unlocked.set(index, state + 1);
            player.playSound(player.getEyeLocation(), Sound.ENTITY_ITEM_PICKUP, SoundCategory.MASTER, 0.5f, 2.0f);
        } else {
            return;
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
            sidebarInfo = ChatColor.BLUE + "Time " + ChatColor.WHITE + timeString;
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
        getBossBar().setTitle(ChatColor.BLUE + "Reach the Goal");
        sidebarInfo = ChatColor.BLUE + "Goal "
            + ChatColor.WHITE + goalCount
            + ChatColor.GRAY + "/" + players.size()
            + " " + ChatColor.GRAY + timeString;
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
                    e.setSilent(true);
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
        getBossBar().setTitle(ChatColor.RED + "Kill all Mobs");
        sidebarInfo = ChatColor.BLUE + "Mobs " + ChatColor.WHITE + aliveMobCount;
        if (waveComplete && wave.getSpawns().size() > 0) {
            int totalExp = wave.getSpawns().size() * 5;
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
            for (Enemy enemy : spawns) {
                Mob mob = enemy.getMob();
                if (mob != null) mob.setGlowing(true);
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

    void removePlayer(Player player) {
        World w = Bukkit.getWorlds().get(0);
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
        String theName = skullNames.get(id);
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        PlayerProfile profile = plugin.getServer().createProfile(id, theName);
        profile.setProperty(new ProfileProperty("textures", texture));
        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);
        return item;
    }

    void interact(Player player, Block block) {
        CustomBlock customBlock = customBlocks.get(block);
        if (customBlock != null) {
            switch (customBlock.mytems) {
            case BOSS_CHEST:
                openRewardGui(player);
                return;
            default:
                break;
            }
        }
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
        return getWave(waveIndex).getPlace().toLocation(world);
    }

    @Override // Context
    public void registerNewEnemy(Enemy enemy) {
        spawns.add(enemy);
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
    public boolean isTemporaryEntity(Entity entity) {
        return adds.contains(entity) || arrows.contains(entity);
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
            enemy.onRemove();
            enemy.remove();
            bosses.remove(enemy);
            if (bosses.isEmpty()) {
                bossFighters.addAll(enemy.getDamagers());
                waveComplete = true;
            }
        }
    }

    @Override // Context
    public List<Enemy> getEnemies() {
        List<Enemy> result = new ArrayList<>(spawns);
        result.addAll(bosses);
        return result;
    }

    public boolean isRunning() {
        return phase == Phase.RUN;
    }

    public void onDealDamage(Player player, EntityDamageByEntityEvent event) {
        Enemy enemy = Enemy.of(event.getEntity());
        if (enemy != null) {
            EnemyType enemyType = EnemyType.of(enemy);
            if (enemyType != null && MytemsPlugin.getInstance().getEquipment(player).hasSetBonus(AculaItemSet.getInstance().getVampiricBonusDamage())) {
                switch (enemyType) {
                case VAMPIRE_BAT:
                case SADISTIC_VAMPIRE:
                case WICKED_CRONE:
                case INFERNAL_PHANTASM:
                    double base = event.getFinalDamage();
                    event.setDamage(base * 1.5);
                    player.sendActionBar(ChatColor.DARK_RED + "Vampiric Bonus Damage");
                default: break;
                }
            }
        }
        if (enemy != null || adds.contains(event.getEntity())) {
            double damage = event.getFinalDamage();
            if (event.getEntity() instanceof Damageable) {
                damage = Math.min(damage, ((Damageable) event.getEntity()).getHealth());
            }
            damageHighscore.add(player, damage);
        }
    }

    public void onPlayerSidebar(Player player, PlayerSidebarEvent event) {
        List<String> lines = new ArrayList<>(20);
        lines.add("" + ChatColor.RED + ChatColor.BOLD + ChatColor.translateAlternateColorCodes('&', raid.displayName));
        lines.add(sidebarInfo);
        if (!damageHighscore.isEmpty()) {
            lines.add("" + ChatColor.RED + "Damage Dealt:");
            for (Highscore.Entry entry : damageHighscore.getEntries()) {
                if (entry.getRank() > 9) break;
                lines.add("" + ChatColor.BLUE + (1 + entry.getRank()) + ") "
                          + ChatColor.RED + (int) entry.getScore()
                          + " " + ChatColor.WHITE + entry.getName());
            }
        }
        event.addLines(plugin, Priority.HIGHEST, lines);
    }
}

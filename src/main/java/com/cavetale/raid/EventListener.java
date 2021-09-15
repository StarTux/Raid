package com.cavetale.raid;

import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.worldmarker.entity.EntityMarker;
import com.destroystokyo.paper.event.entity.EndermanEscapeEvent;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import com.destroystokyo.paper.event.entity.ThrownEggHatchEvent;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

@RequiredArgsConstructor
final class EventListener implements Listener {
    final RaidPlugin plugin;
    static final String MOB_PROJECTILE_ID = "raid:mob_projectile";

    @EventHandler(priority = EventPriority.LOW)
    void onEntityExplode(EntityExplodeEvent event) {
        Instance inst = plugin.raidInstance(event.getEntity().getWorld());
        if (inst == null) return;
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.LOW)
    void onBlockExplode(BlockExplodeEvent event) {
        Instance inst = plugin.raidInstance(event.getBlock().getWorld());
        if (inst == null) return;
        event.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true)
    void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Mob)) return;
        Mob mob = (Mob) event.getEntity();
        Instance inst = plugin.raidInstance(mob.getWorld());
        if (inst == null) return;
        switch (event.getSpawnReason()) {
        case CUSTOM:
            // allowed
            return;
        case SPAWNER:
        case SLIME_SPLIT:
            // allowed but modified
            inst.adds.add(mob);
            mob.setPersistent(false);
            return;
        case DEFAULT:
            // ???
            if (mob.getType() == EntityType.VEX) {
                event.setCancelled(true);
            }
            return;
        case NATURAL:
        case JOCKEY:
        case CHUNK_GEN:
        case EGG:
        case SPAWNER_EGG:
        case LIGHTNING:
        case BUILD_SNOWMAN:
        case BUILD_IRONGOLEM:
        case BUILD_WITHER:
        case VILLAGE_DEFENSE:
        case VILLAGE_INVASION:
        case BREEDING:
        case REINFORCEMENTS:
        case NETHER_PORTAL:
        case DISPENSE_EGG:
        case INFECTION:
        case CURED:
        case OCELOT_BABY:
        case SILVERFISH_BLOCK:
        case MOUNT:
        case TRAP:
        case ENDER_PEARL:
        case SHOULDER_ENTITY:
        case DROWNED:
        case SHEARED:
        case EXPLOSION:
        case RAID:
        case PATROL:
        case BEEHIVE:
            // forbidden
            event.setCancelled(true);
            return;
        default:
            plugin.getLogger().warning("Unhandled spawn: " + event.getSpawnReason());
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
    void onPlayerTeleport(PlayerTeleportEvent event) {
        Instance inst = plugin.raidInstance(event.getPlayer().getWorld());
        if (inst != null) {
            switch (event.getCause()) {
            case ENDER_PEARL:
                if (inst.getWave().getType() != Wave.Type.BOSS) {
                    event.setCancelled(true);
                    return;
                }
                break;
            case CHORUS_FRUIT:
                event.setCancelled(true);
            default: break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onEntityDamage(EntityDamageEvent event) {
        Instance inst = plugin.raidInstance(event.getEntity().getWorld());
        if (inst == null) return;
        if (EscortMarker.of(event.getEntity()) != null) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            switch (event.getCause()) {
            case LIGHTNING:
                event.setDamage(14.0);
                break;
            default:
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().hasMetadata("raid:nodamage")) {
            event.setCancelled(true);
            return;
        }
        Instance inst = plugin.raidInstance(event.getEntity().getWorld());
        if (inst == null) return;
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (event.getDamager() instanceof ElderGuardian) {
                double dmg = event.getDamage();
                if (dmg > 1.0) event.setDamage(1.0); // 4 times per second
            }
            if (EntityMarker.hasId(event.getDamager(), MOB_PROJECTILE_ID)) {
                double base = event.getDamage(EntityDamageEvent.DamageModifier.BASE);
                event.setDamage(EntityDamageEvent.DamageModifier.BASE, base * 1.25);
            }
        }
        if (event.getEntity() instanceof ArmorStand) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && event.getDamager() instanceof Player) {
            inst.onDealDamage((Player) event.getDamager(), event);
        } else if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE && event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                inst.onDealDamage((Player) projectile.getShooter(), event);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onProjectileLaunch(ProjectileLaunchEvent event) {
        Instance inst = plugin.raidInstance(event.getEntity().getWorld());
        if (inst == null) return;
        if (event.getEntity() instanceof AbstractArrow) {
            AbstractArrow arrow = (AbstractArrow) event.getEntity();
            if (!(arrow.getShooter() instanceof Mob)) return;
            arrow.setPersistent(false);
            inst.arrows.add(arrow);
        }
        if (event.getEntity().getShooter() instanceof Mob) {
            EntityMarker.setId(event.getEntity(), MOB_PROJECTILE_ID);
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onHangingBreak(HangingBreakEvent event) {
        Entity entity = event.getEntity();
        Instance inst = plugin.raidInstance(entity.getWorld());
        if (inst == null) return;
        if (event instanceof HangingBreakByEntityEvent) {
            Entity remover = ((HangingBreakByEntityEvent) event).getRemover();
            if (remover instanceof Player && ((Player) remover).getGameMode() == GameMode.CREATIVE) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler
    void onPlayerInteract(PlayerInteractEvent event) {
        switch (event.getAction()) {
        case RIGHT_CLICK_BLOCK: case LEFT_CLICK_BLOCK: break;
        default: return;
        }
        Block block = event.getClickedBlock();
        Instance inst = plugin.raidInstance(block.getWorld());
        if (inst == null) return;
        inst.interact(event.getPlayer(), block);
    }

    @EventHandler
    void onThrownEggHatch(ThrownEggHatchEvent event) {
        Instance inst = plugin.raidInstance(event.getEgg().getWorld());
        if (inst == null) return;
        event.setHatching(false);
    }

    @EventHandler
    void onPlayerCanBuild(PlayerBlockAbilityQuery query) {
        if (query.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        Instance inst = plugin.raidInstance(query.getBlock().getWorld());
        if (inst == null) return;
        query.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Instance inst = plugin.raidInstance(event.getBlock().getWorld());
        if (inst == null) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    void onEntityBlockForm(EntityBlockFormEvent event) {
        Instance inst = plugin.raidInstance(event.getBlock().getWorld());
        if (inst == null) return;
        event.setCancelled(true);
    }

    @EventHandler
    void onWorldLoad(WorldLoadEvent event) {
        Instance inst = plugin.raidInstance(event.getWorld());
        if (inst != null) {
            inst.onWorldLoaded(event.getWorld());
        }
    }

    @EventHandler
    void onWorldUnload(WorldUnloadEvent event) {
        Instance inst = plugin.raidInstance(event.getWorld());
        if (inst != null) {
            inst.onWorldUnload();
        }
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        plugin.sessions.enter(event.getPlayer());
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event) {
        Attributes.reset(event.getPlayer());
        plugin.sessions.exit(event.getPlayer());
    }

    @EventHandler
    void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Attributes.reset(event.getPlayer());
    }

    @EventHandler
    void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Instance inst = plugin.raidInstance(player.getWorld());
        if (inst == null) return;
        if (player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
            return;
        }
        Session session = plugin.sessions.of(player);
        if (session.isPlacingRoadblocks()) {
            Wave wave = inst.getWave(session.getEditWave());
            if (wave == null) return;
            Block block = event.getBlock();
            Roadblock rb = new Roadblock(block, block.getBlockData(), event.getBlockReplacedState().getBlockData());
            wave.addRoadblock(rb);
            player.sendMessage(ChatColor.YELLOW + "Roadblock added: " + rb);
        }
    }

    @EventHandler
    void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Instance inst = plugin.raidInstance(player.getWorld());
        if (inst == null) return;
        if (player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
            return;
        }
        Session session = plugin.sessions.of(player);
        if (session.isPlacingRoadblocks()) {
            Wave wave = inst.getWave(session.getEditWave());
            if (wave == null) return;
            Block block = event.getBlock();
            Roadblock rb = new Roadblock(block, Material.AIR.createBlockData(), block.getBlockData());
            wave.addRoadblock(rb);
            player.sendMessage(ChatColor.YELLOW + "Roadblock added: " + rb);
        }
    }

    @EventHandler
    void onEntityPathfind(EntityPathfindEvent event) {
        EscortMarker escortMarker = EscortMarker.of(event.getEntity());
        if (escortMarker == null) return;
        if (escortMarker.isPathing()) return;
        event.setCancelled(true);
        escortMarker.refreshPath();
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        Instance inst = plugin.raidInstance(player.getWorld());
        if (inst == null) return;
        ItemStack stack = event.getItemDrop().getItemStack();
        // Whitelist simple items
        if (stack.isSimilar(new ItemStack(stack.getType()))) return;
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "Can't drop this item in a raid!");
    }

    @EventHandler(ignoreCancelled = true)
    void onBlockIgnite(BlockIgniteEvent event) {
        Instance inst = plugin.raidInstance(event.getBlock().getWorld());
        if (inst == null) return;
        if (event.getIgnitingEntity() instanceof Player) {
            Player player = (Player) event.getIgnitingEntity();
            if (player.getGameMode() == GameMode.CREATIVE) return;
        }
        event.setCancelled(true);
    }

    /**
     * Enemies are already taken care of, but temporary entities also
     * need to be stopped from targetting each other or any entities.
     */
    @EventHandler(ignoreCancelled = true)
    void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity living = (LivingEntity) event.getEntity();
        Instance inst = plugin.raidInstance(living.getWorld());
        if (inst == null) return;
        if (inst.isTemporaryEntity(event.getEntity())) {
            if (!SpawnEnemy.isAcceptableMobTarget(event.getTarget(), inst)) {
                LivingEntity target = SpawnEnemy.findTarget(living, inst);
                if (target != null) {
                    event.setTarget(target);
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    void onPlayerSidebar(PlayerSidebarEvent event) {
        Instance inst = plugin.raidInstance(event.getPlayer().getWorld());
        if (inst == null) return;
        inst.onPlayerSidebar(event.getPlayer(), event);
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Instance inst = plugin.raidInstance(event.getBlock().getWorld());
        if (inst == null) return;
        if (event.getPlayer().isOp()) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Instance inst = plugin.raidInstance(event.getBlock().getWorld());
        if (inst == null) return;
        if (event.getPlayer().isOp()) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Instance inst = plugin.raidInstance(event.getRightClicked().getWorld());
        if (inst == null) return;
        if (event.getPlayer().isOp()) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Instance inst = plugin.raidInstance(player.getWorld());
        if (inst == null) return;
        inst.onPlayerDeath(player);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Instance inst = plugin.raidInstance(player.getWorld());
        if (inst == null) return;
        event.setRespawnLocation(Bukkit.getWorlds().get(0).getSpawnLocation());
    }

    @EventHandler
    void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        Instance inst = plugin.raidInstance(event.getSpawnLocation().getWorld());
        if (inst != null && !inst.alreadyJoined.contains(event.getPlayer().getUniqueId())) {
            event.setSpawnLocation(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
    }

    @EventHandler
    void onProjectileCollide(ProjectileCollideEvent event) {
        Projectile projectile = event.getEntity();
        Instance inst = plugin.raidInstance(projectile.getWorld());
        if (inst == null) return;
        if (projectile.getShooter() instanceof Player && event.getCollidedWith() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        plugin.getIdEscortMap().remove(event.getEntity().getEntityId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onEndermanEscape(EndermanEscapeEvent event) {
        Instance inst = plugin.raidInstance(event.getEntity().getWorld());
        if (inst != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (event.isGliding()) {
            Instance inst = plugin.raidInstance(event.getEntity().getWorld());
            if (inst != null) {
                event.setCancelled(true);
            }
        }
    }
}

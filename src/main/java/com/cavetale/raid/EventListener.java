package com.cavetale.raid;

import lombok.RequiredArgsConstructor;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

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

    @EventHandler(priority = EventPriority.MONITOR)
    void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Mob) {
            Mob mob = (Mob) event.getEntity();
            Instance inst = plugin.raidInstance(mob.getWorld());
            if (inst == null) return;
            inst.onDeath(mob);
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Mob)) return;
        Mob mob = (Mob) event.getEntity();
        Instance inst = plugin.raidInstance(mob.getWorld());
        if (inst != null) {
            if (mob.getType() == EntityType.VEX
                && event.getSpawnReason() == SpawnReason.DEFAULT) {
                event.setCancelled(true);
            } else if (event.getSpawnReason() == SpawnReason.SLIME_SPLIT) {
                if (inst.bossFight != null) {
                    inst.bossFight.adds.add(mob);
                } else {
                    inst.adds.add(mob);
                }
            }
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
        if (inst != null
            && event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Mob) {
            Mob mob = (Mob) event.getEntity();
            Instance inst = plugin.raidInstance(mob.getWorld());
            if (inst == null) return;
            if (inst.bossFight != null && mob.equals(inst.bossFight.mob)) {
                inst.bossFight.onBossDamage(event);
            }
        } else if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Instance inst = plugin.raidInstance(player.getWorld());
            if (inst == null) return;
            switch (event.getCause()) {
            case LIGHTNING:
                event.setDamage(14.0);
                break;
            default:
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Instance inst = plugin.raidInstance(player.getWorld());
            if (inst == null) return;
            if (event.getDamager() instanceof ElderGuardian) {
                double dmg = event.getDamage();
                if (dmg > 1.0) event.setDamage(1.0); // 4 times per second
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow)) return;
        AbstractArrow arrow = (AbstractArrow) event.getEntity();
        Instance inst = plugin.raidInstance(arrow.getWorld());
        if (inst == null) return;
        if (!(arrow.getShooter() instanceof Mob)) return;
        arrow.setPersistent(false);
        inst.arrows.add(arrow);
    }

    @EventHandler(ignoreCancelled = true)
    void onHangingBreak(HangingBreakEvent event) {
        Entity entity = event.getEntity();
        Instance inst = plugin.raidInstance(entity.getWorld());
        if (inst == null) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    void onEntityTarget(EntityTargetEvent event) {
        if (event.getReason() == EntityTargetEvent.TargetReason.CUSTOM) return;
        if (!(event.getEntity() instanceof Mob)) return;
        Mob mob = (Mob) event.getEntity();
        Instance inst = plugin.raidInstance(mob.getWorld());
        if (inst == null) return;
        if (inst.isAcceptableMobTarget(event.getTarget())) return;
        Player newTarget = inst.findTarget(mob, inst.getPlayers());
        // May be null which will set NO target, NOT keep the previous one.
        event.setTarget(newTarget);
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
}

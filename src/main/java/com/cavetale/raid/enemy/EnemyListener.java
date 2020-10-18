package com.cavetale.raid.enemy;

import com.cavetale.raid.ability.EggLauncherAbility;
import com.cavetale.raid.ability.FireworkAbility;
import com.cavetale.worldmarker.EntityMarker;
import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public final class EnemyListener implements Listener {
    private final JavaPlugin plugin;

    public EnemyListener enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        return this;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onEntityDeath(EntityDeathEvent event) {
        EnemyHandle handle = EnemyHandle.of(event.getEntity());
        if (handle == null) return;
        handle.onEntityDeath(event);
    }

    @EventHandler(ignoreCancelled = true)
    void onEntityDamage(EntityDamageEvent event) {
        EnemyHandle handle = EnemyHandle.of(event.getEntity());
        if (handle == null) return;
        handle.onEntityDamage(event);
    }

    @EventHandler(ignoreCancelled = false)
    void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getCause() == DamageCause.ENTITY_EXPLOSION && EntityMarker.hasId(event.getDamager(), EggLauncherAbility.EXPLOSIVE_EGG_ID)) {
            if (!(event.getEntity() instanceof Player)) {
                event.setCancelled(true);
            }
        } else if (EntityMarker.hasId(event.getDamager(), FireworkAbility.FIREWORK_ID)) {
            if (event.getEntity() instanceof Player) {
                event.setDamage(EntityDamageEvent.DamageModifier.BASE, 9.0);
            } else {
                event.setCancelled(true);
            }
        }
        EnemyHandle handle = EnemyHandle.of(event.getEntity());
        if (handle == null) return;
        handle.onEntityDamageByEntity(event);
    }

    @EventHandler(ignoreCancelled = false)
    void onProjectileCollide(ProjectileCollideEvent event) {
        Projectile proj = event.getEntity();
        String id = EntityMarker.getId(proj);
        if (id == null) return;
        switch (id) {
        case EggLauncherAbility.EXPLOSIVE_EGG_ID:
        case FireworkAbility.FIREWORK_ID: // doesn't seem to work with fireworks
            if (!(event.getCollidedWith() instanceof Player)) {
                event.setCancelled(true);
            }
            break;
        default: break;
        }
    }

    @EventHandler
    void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (EntityMarker.hasId(proj, EggLauncherAbility.EXPLOSIVE_EGG_ID)) {
            proj.getWorld().createExplosion(proj, 1.0f);
            proj.remove();
        }
    }
}

package com.cavetale.raid.enemy;

import com.cavetale.worldmarker.EntityMarker;
import com.cavetale.worldmarker.Persistent;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpellCastEvent;
import org.bukkit.event.entity.EntityTargetEvent;

public interface EnemyHandle extends Persistent {
    Enemy getEnemy();

    @Override
    default boolean shouldSave() {
        return false;
    }

    default void onEntityDeath(EntityDeathEvent event) { }

    default void onEntityExplode(EntityExplodeEvent event) { }

    default void onEntityDamage(EntityDamageEvent event) { }

    default void onEntityDamageByEntity(EntityDamageByEntityEvent event) { }

    default void onEntityTarget(EntityTargetEvent event) { }

    default void onEntityPathfind(EntityPathfindEvent event) { }

    default void onEntitySpellCast(EntitySpellCastEvent event) { }

    static EnemyHandle of(Entity entity) {
        return EntityMarker.getEntity(entity).getPersistent(Enemy.WORLD_MARKER_ID, EnemyHandle.class);
    }
}

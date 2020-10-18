package com.cavetale.raid.enemy;

import com.cavetale.worldmarker.EntityMarker;
import com.cavetale.worldmarker.Persistent;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public interface EnemyHandle extends Persistent {
    Enemy getEnemy();

    default void onEntityDeath(EntityDeathEvent event) { }

    default void onEntityDamage(EntityDamageEvent event) { }

    default void onEntityDamageByEntity(EntityDamageByEntityEvent event) { }

    static EnemyHandle of(Entity entity) {
        return EntityMarker.getEntity(entity).getPersistent(Enemy.WORLD_MARKER_ID, EnemyHandle.class);
    }
}

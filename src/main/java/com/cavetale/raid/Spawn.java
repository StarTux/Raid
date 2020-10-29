package com.cavetale.raid;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import com.cavetale.raid.enemy.EnemyType;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

/**
 * Configuration.
 * A mob to be spawned in a location.
 */
final class Spawn implements ShortInfo {
    String entityType;
    Place place;
    int amount = 1;

    Spawn() { }

    Spawn(@NonNull final String entityType, @NonNull final Location location, final int amount) {
        this.entityType = entityType;
        this.place = Place.of(location);
        this.amount = amount;
    }

    public EnemyType getEnemyType() {
        try {
            return EnemyType.valueOf(entityType.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    public EntityType getBukkitType() {
        try {
            return EntityType.valueOf(entityType.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    public Enemy createEnemy(Context context) {
        EnemyType enemyType = getEnemyType();
        if (enemyType != null) return enemyType.create(context);
        EntityType bukkitType = getBukkitType();
        if (bukkitType != null && Mob.class.isAssignableFrom(bukkitType.getEntityClass())) {
            return new SpawnEnemy(context, (Class<? extends Mob>) bukkitType.getEntityClass(), place.toLocation(context.getWorld()));
        }
        return null;
    }

    @Override
    public String getShortInfo() {
        return entityType + " " + place.getShortInfo() + " " + amount;
    }
}

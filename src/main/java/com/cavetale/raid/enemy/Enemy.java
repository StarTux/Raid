package com.cavetale.raid.enemy;

import com.cavetale.worldmarker.EntityMarker;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * An Enemy instance represents a unit which fights the player and
 * vice versa. It could be a wrapper for a literal bukkit Entity, a
 * collection of entities, or entirely virtual.
 */
public interface Enemy {
    String WORLD_MARKER_ID = "raid:enemy";

    Context getContext();

    void spawn(Location location);

    default void spawn() {
        spawn(getContext().getSpawnLocation());
    }

    void remove();

    /**
     * Called when the enemy is purposely removed from the world.
     */
    void onRemove();

    World getWorld();

    Location getLocation();

    Location getEyeLocation();

    boolean hasLineOfSight(Entity other);

    String getDisplayName();

    <T extends Projectile> T launchProjectile(Class<T> projectile, Vector velocity);

    void setInvulnerable(boolean invulnerable);

    void setImmobile(boolean immobile);

    boolean isInvulnerable();

    /**
     * May return null.
     */
    LivingEntity getLivingEntity();

    /**
     * May return null.
     */
    Mob getMob();

    void teleport(Location to);

    BoundingBox getBoundingBox();

    Collection<UUID> getDamagers();

    boolean isValid();

    boolean isAlive();

    default boolean isDead() {
        return !isAlive();
    }

    double getHealth();

    double getMaxHealth();

    default List<Player> getPlayerDamagers() {
        return getDamagers().stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Get the Enemy that belongs to this entity, or null if none is found.
     */
    static Enemy of(Entity entity) {
        EnemyHandle handle = EntityMarker.getEntity(entity).getPersistent(Enemy.WORLD_MARKER_ID, EnemyHandle.class);
        if (handle == null) return null;
        return handle.getEnemy();
    }

    /**
     * Customize drops if desired.
     */
    default List<ItemStack> getDrops() {
        return Collections.emptyList();
    }
}

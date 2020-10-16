package com.cavetale.raid.enemy;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public interface Enemy {
    World getWorld();

    Location getLocation();

    Location getEyeLocation();

    boolean hasLineOfSight(Entity other);

    String getDisplayName();

    <T extends Projectile> T launchProjectile(Class<T> projectile, Vector velocity);

    void setInvulnerable(boolean invulnerable);

    void setImmobile(boolean immobile);

    boolean isInvulnerable();

    LivingEntity getLivingEntity();

    Mob getMob();

    void teleport(Location to);

    BoundingBox getBoundingBox();
}

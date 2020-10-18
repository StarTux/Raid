package com.cavetale.raid;

import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Rabbit;

/**
 * Configuration.
 * A mob to be spawned in a location.
 */
final class Spawn implements ShortInfo {
    EntityType entityType;
    Place place;
    int amount = 1;
    double health = 0;
    double attributeFactor = 1.0;

    Spawn() { }

    Spawn(@NonNull final EntityType entityType,
          @NonNull final Location location,
          final int amount) {
        this.entityType = entityType;
        this.place = Place.of(location);
        this.amount = amount;
    }

    Mob spawn(Location loc) {
        World w = loc.getWorld();
        Entity entity = w.spawn(loc, entityType.getEntityClass(), this::prep);
        return entity instanceof Mob ? (Mob) entity : null;
    }

    Mob spawn(@NonNull World world) {
        Location loc = place.toLocation(world);
        return spawn(loc);
    }

    void prep(Entity entity) {
        entity.setPersistent(false);
        if (entity instanceof Mob) prep((Mob) entity);
    }

    void prep(Mob mob) {
        if (health > 0) {
            mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
            mob.setHealth(health);
        }
        if (mob instanceof Rabbit) {
            Rabbit rabbit = (Rabbit) mob;
            rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
        }
        if (mob instanceof Bee) {
            Bee bee = (Bee) mob;
            bee.setAnger(12000);
        }
    }

    @Override
    public String getShortInfo() {
        return entityType + " " + place.getShortInfo() + " " + amount;
    }
}

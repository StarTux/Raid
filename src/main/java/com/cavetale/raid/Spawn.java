package com.cavetale.raid;

import com.cavetale.enemy.Enemy;
import com.cavetale.enemy.EnemyType;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
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
    EntityType mount;
    Material helmet;
    Material chestplate;
    Material leggings;
    Material boots;
    Material hand;
    boolean baby;
    boolean powered;
    Map<Attribute, Double> attributes;
    double scaling = 0.25;

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

    public Enemy createEnemy(Instance instance) {
        EnemyType enemyType = getEnemyType();
        if (enemyType != null) return enemyType.create(instance);
        EntityType bukkitType = getBukkitType();
        if (bukkitType != null && Mob.class.isAssignableFrom(bukkitType.getEntityClass())) {
            return new SpawnEnemy(instance, (Class<? extends Mob>) bukkitType.getEntityClass(), this);
        }
        return null;
    }

    static String camel(String in) {
        return Stream.of(in.split("_"))
            .map(i -> i.substring(0, 1) + i.substring(1).toLowerCase())
            .collect(Collectors.joining(""));
    }

    @Override
    public String getShortInfo() {
        StringBuilder sb = new StringBuilder(entityType);
        sb.append(" ").append(place.getShortInfo());
        if (amount != 1) sb.append(" x" + amount);
        if (mount != null) sb.append(" mount=" + mount);
        if (helmet != null) sb.append(" helmet=" + helmet);
        if (chestplate != null) sb.append(" chestplate=" + chestplate);
        if (leggings != null) sb.append(" leggings=" + leggings);
        if (boots != null) sb.append(" boots=" + boots);
        if (attributes != null) {
            for (Map.Entry<Attribute, Double> entry : attributes.entrySet()) {
                sb.append(" " + camel(entry.getKey().name().replace("GENERIC_", "").replace("MAX_", ""))
                          + "=" + String.format("%.2f", entry.getValue()));
            }
        }
        if (scaling != 0) sb.append(" scaling=" + String.format("%.2f", scaling));
        return sb.toString();
    }

    public Map<Attribute, Double> getAttributes() {
        if (attributes == null) attributes = new EnumMap<>(Attribute.class);
        return attributes;
    }

    public void onSave() {
        if (attributes != null && attributes.isEmpty()) attributes = null;
    }
}

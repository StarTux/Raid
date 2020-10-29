package com.cavetale.raid.enemy;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;

/**
 * Utility class to prepare an entity for battle, quickly.
 */
public final class Prep {
    private Prep() { }

    public static void disableEquipmentDrop(LivingEntity entity) {
        EntityEquipment eq = entity.getEquipment();
        if (eq != null) {
            eq.setHelmetDropChance(0.0f);
            eq.setChestplateDropChance(0.0f);
            eq.setLeggingsDropChance(0.0f);
            eq.setBootsDropChance(0.0f);
            eq.setItemInMainHandDropChance(0.0f);
            eq.setItemInOffHandDropChance(0.0f);
        }
    }

    public static void attr(Attributable entity, Attribute attribute, double value) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst == null) return;
        inst.setBaseValue(value);
    }

    public static void health(LivingEntity entity, double health, double maxHealth) {
        attr(entity, Attribute.GENERIC_MAX_HEALTH, maxHealth);
        entity.setHealth(Math.min(health, maxHealth));
    }

    public static void health(LivingEntity entity, double health) {
        health(entity, health, health);
    }

    public static void boss(LivingEntity entity) {
        attr(entity, Attribute.GENERIC_ATTACK_DAMAGE, 10.0);
        attr(entity, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
        attr(entity, Attribute.GENERIC_ARMOR, 20.0); // dia=20
        attr(entity, Attribute.GENERIC_ARMOR_TOUGHNESS, 8.0); // dia=8
        entity.setCustomNameVisible(true);
        entity.setPersistent(false);
        disableEquipmentDrop(entity);
    }

    public static void add(LivingEntity entity) {
        entity.setPersistent(false);
        disableEquipmentDrop(entity);
    }

    public static void removeGoals(Mob mob) {
        Bukkit.getMobGoals().removeAllGoals(mob);
    }

    public static void movementSpeed(LivingEntity entity, double speed) {
        entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
    }
}

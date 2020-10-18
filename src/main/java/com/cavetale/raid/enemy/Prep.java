package com.cavetale.raid.enemy;

import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;

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
}

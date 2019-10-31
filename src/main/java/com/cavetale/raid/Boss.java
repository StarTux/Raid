package com.cavetale.raid;

import com.cavetale.worldmarker.EntityMarker;
import java.util.Arrays;
import java.util.List;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.EntityEquipment;

final class Boss implements ShortInfo {
    Type type;

    enum Type {
        DECAYED("The Decayed"),
        FORGOTTEN("The Forgotten"),
        VENGEFUL("The Vengeful");

        public final String displayName;

        Type(@NonNull final String displayName) {
            this.displayName = displayName;
        }
    }

    Boss() { }

    Boss(@NonNull final Type type) {
        this.type = type;
    }

    @Override
    public String getShortInfo() {
        return "" + type;
    }

    List<String> getDialogue() {
        switch (type) {
        case DECAYED:
            return Arrays
                .asList("Your effort is in vain.",
                        "The show's over with my next move.",
                        "Welcome home.");
        case FORGOTTEN:
            return Arrays
                .asList("The journey ends here.",
                        "My powers are beyond your understanding.",
                        "You were foolish to come here.");
        case VENGEFUL:
            return Arrays
                .asList("Like the crops in the field, you too shall wither away.",
                        "Your time has come.",
                        "Death draws nearer with every moment.");
        default:
            return Arrays.asList("You'll never defeat StarTuuuuux!!!");
        }
    }

    Mob spawn(@NonNull Location loc) {
        World w = loc.getWorld();
        switch (type) {
        case DECAYED:
            return w.spawn(loc, WitherSkeleton.class, e -> {
                    EntityEquipment eq = e.getEquipment();
                    eq.setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN).create());
                    eq.setItemInMainHand(new ItemBuilder(Material.DIAMOND_SWORD)
                                         .ench(Enchantment.KNOCKBACK, 2)
                                         .ench(Enchantment.DAMAGE_ALL, 5).create());
                    prep(e);
                });
        case FORGOTTEN:
            return w.spawn(loc, Evoker.class, e -> {
                    prep(e);
                });
        case VENGEFUL:
            return w.spawn(loc, Wither.class, e -> {
                    prep(e);
                });
        default:
            throw new IllegalArgumentException(type.name());
        }
    }

    void prep(@NonNull Mob mob) {
        double health = 1000.0;
        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
        mob.setHealth(1000.0);
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(10.0);
        mob.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25);
        mob.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(20.0);
        mob.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(8.0);
        mob.setPersistent(false);
        EntityMarker.setId(mob, "raid:boss");
        EntityEquipment eq = mob.getEquipment();
        eq.setHelmetDropChance(0.0f);
        eq.setChestplateDropChance(0.0f);
        eq.setLeggingsDropChance(0.0f);
        eq.setBootsDropChance(0.0f);
        eq.setItemInMainHandDropChance(0.0f);
        eq.setItemInOffHandDropChance(0.0f);
        mob.setCustomName("" + ChatColor.YELLOW + ChatColor.BOLD + type.displayName);
        mob.setCustomNameVisible(true);
    }
}

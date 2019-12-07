package com.cavetale.raid;

import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

final class ItemBuilder {
    private final ItemStack item;

    ItemBuilder(@NonNull final Material mat) {
        item = new ItemStack(mat);
    }

    ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    ItemBuilder dmg(int dmg) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            if (damageable.hasDamage()) {
                damageable.setDamage(dmg);
                item.setItemMeta(meta);
            }
        }
        return this;
    }

    ItemBuilder ench(@NonNull Enchantment ench, int level) {
        item.addUnsafeEnchantment(ench, level);
        return this;
    }

    ItemBuilder basePotion(@NonNull PotionType type, boolean extended, boolean upgraded) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof PotionMeta) {
            PotionMeta potion = (PotionMeta) meta;
            potion.setBasePotionData(new PotionData(type, extended, upgraded));
            item.setItemMeta(potion);
        }
        return this;
    }

    ItemBuilder customEffect(@NonNull PotionEffectType type, int duration, int amplifier) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof PotionMeta) {
            PotionMeta potion = (PotionMeta) meta;
            PotionEffect effect = new PotionEffect(type, duration, amplifier,
                                                   true, true, true);
            potion.addCustomEffect(effect, true);
            item.setItemMeta(potion);
        }
        return this;
    }

    ItemStack create() {
        return item.clone();
    }
}

package com.cavetale.raid;

import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

final class Util {
    static MemoryConfiguration tmp;

    private Util() { }

    static ConfigurationSection createSection(Map<?, ?> map) {
        if (tmp == null) {
            tmp = new MemoryConfiguration();
        }
        return tmp.createSection("tmp", map);
    }

    static Player getPlayerDamager(Entity entity) {
        if (entity instanceof Player) {
            return (Player) entity;
        } else if (entity instanceof Projectile) {
            Projectile proj = (Projectile) entity;
            if (proj.getShooter() instanceof Player) {
                return (Player) proj.getShooter();
            }
        }
        return null;
    }
}

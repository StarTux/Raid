package com.cavetale.raid;

import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

final class Util {
    static MemoryConfiguration tmp;

    private Util() { }

    static ConfigurationSection createSection(Map<?, ?> map) {
        if (tmp == null) {
            tmp = new MemoryConfiguration();
        }
        return tmp.createSection("tmp", map);
    }
}

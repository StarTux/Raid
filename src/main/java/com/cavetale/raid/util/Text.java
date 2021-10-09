package com.cavetale.raid.util;

import org.bukkit.ChatColor;

public final class Text {
    private Text() { }

    public static String colorize(String in) {
        return ChatColor.translateAlternateColorCodes('&', in);
    }
}

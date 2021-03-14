package com.cavetale.raid.util;

import net.md_5.bungee.api.ChatColor;

public final class Text {
    private Text() { }

    public static String colorize(String in) {
        return ChatColor.translateAlternateColorCodes('&', in);
    }
}

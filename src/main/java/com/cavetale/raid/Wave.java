package com.cavetale.raid;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;

@Getter
final class Wave implements ShortInfo {
    Type type = Type.MOBS;
    Place place;
    double radius = 0;
    List<Spawn> spawns = new ArrayList<>();
    Boss boss;
    int time = 0;

    enum Type {
        MOBS(ChatColor.RED),
        GOAL(ChatColor.WHITE),
        BOSS(ChatColor.DARK_RED),
        TIME(ChatColor.BLUE),
        WIN(ChatColor.GOLD);

        public final ChatColor color;
        public final String key;

        Type(final ChatColor color) {
            this.color = color;
            this.key = name().toLowerCase();
        }

        Type() {
            this(ChatColor.WHITE);
        }
    }

    @Override
    public String getShortInfo() {
        return type
            + " place=" + ShortInfo.of(place)
            + " radius=" + radius
            + " mobs=" + spawns.size()
            + " boss=" + ShortInfo.of(boss);
    }
}

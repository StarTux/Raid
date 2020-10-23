package com.cavetale.raid;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;

@Getter
final class Wave implements ShortInfo {
    Type type = Type.MOBS;
    Place place;
    double radius = 0;
    Boss boss;
    int time = 0;
    private List<Spawn> spawns;
    private List<Roadblock> roadblocks;
    private List<Escort> escorts;
    private Set<Flag> flags;

    enum Type {
        MOBS(ChatColor.RED),
        GOAL(ChatColor.WHITE),
        BOSS(ChatColor.DARK_RED),
        TIME(ChatColor.BLUE),
        ROADBLOCK(ChatColor.GREEN),
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

    enum Flag {
        NONE;
    }

    @Override
    public String getShortInfo() {
        return type
            + " place=" + ShortInfo.of(place)
            + (radius == 0 ? "" : " radius=" + radius)
            + (getSpawns().isEmpty() ? "" : " mobs=" + getSpawns().size())
            + (getRoadblocks().isEmpty() ? "" : " rblocks=" + getRoadblocks().size())
            + (boss == null ? "" : " boss=" + ShortInfo.of(boss))
            + (getEscorts().isEmpty() ? "" : " escort=" + getEscorts().size());
    }

    public void onSave() {
        if (spawns != null && spawns.isEmpty()) spawns = null;
        if (roadblocks != null && roadblocks.isEmpty()) roadblocks = null;
        if (flags != null && flags.isEmpty()) flags = null;
    }

    public List<Spawn> getSpawns() {
        if (spawns == null) spawns = new ArrayList<>();
        return spawns;
    }

    public List<Roadblock> getRoadblocks() {
        if (roadblocks == null) roadblocks = new ArrayList<>();
        return roadblocks;
    }

    public Set<Flag> getFlags() {
        if (flags == null) flags = EnumSet.noneOf(Flag.class);
        return flags;
    }

    public List<Escort> getEscorts() {
        if (escorts == null) escorts = new ArrayList<>();
        return escorts;
    }

    public void addRoadblock(Roadblock roadblock) {
        Roadblock found = null;
        for (Roadblock old : getRoadblocks()) {
            if (old.isInSamePlace(roadblock)) {
                found = old;
                break;
            }
        }
        if (found != null) {
            getRoadblocks().remove(found);
        }
        getRoadblocks().add(roadblock);
    }
}

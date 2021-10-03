package com.cavetale.raid;

import com.cavetale.raid.struct.Cuboid;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import net.md_5.bungee.api.ChatColor;

@Data
final class Wave implements ShortInfo {
    protected String name;
    protected Type type = Type.MOBS;
    protected Place place;
    private double radius = 0;
    protected Boss boss;
    protected int time = 0;
    private List<Spawn> spawns;
    private List<Roadblock> roadblocks;
    private List<Escort> escorts;
    private Set<Flag> flags;
    protected Map<String, Cuboid> regions = new HashMap<>();
    protected List<String> nextWave;
    protected Map<ClipEvent, List<String>> clips = new EnumMap<>(ClipEvent.class);

    enum Type {
        MOBS(ChatColor.RED), // Kill all mobs
        GOAL(ChatColor.WHITE), // Reach goal
        BOSS(ChatColor.DARK_RED), // Boss fight
        TIME(ChatColor.BLUE), // Wait time
        ROADBLOCK(ChatColor.GREEN), // Roadblocks dictate timing
        WIN(ChatColor.GOLD), // Rewards, boss chest
        TITLE(ChatColor.WHITE), // Show title and finish fast
        ESCORT(ChatColor.LIGHT_PURPLE), // Escorts dictate timing
        CHOICE(ChatColor.GREEN), // Pick nextWave via region. "choice.X" => nextWave[X]
        RANDOM(ChatColor.GREEN), // Random next wave among nextWave[]
        DEFEND(ChatColor.RED); // Defend the escort

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

    protected enum Flag {
        NONE;
    }

    protected enum ClipEvent {
        INIT,
        ENTER,
        COMPLETE;
    }

    @Override
    public String getShortInfo() {
        return type
            + (name != null ? " name=" + name : "")
            + " place=" + ShortInfo.of(place)
            + (radius == 0 ? "" : " radius=" + radius)
            + (getSpawns().isEmpty() ? "" : " mobs=" + getSpawns().size())
            + (getRoadblocks().isEmpty() ? "" : " rblocks=" + getRoadblocks().size())
            + (boss == null ? "" : " boss=" + ShortInfo.of(boss))
            + (getEscorts().isEmpty() ? "" : " escort=" + getEscorts().size())
            + (nextWave != null ? " next=" + nextWave : "");
    }

    public void onSave() {
        if (spawns != null && spawns.isEmpty()) spawns = null;
        if (roadblocks != null && roadblocks.isEmpty()) roadblocks = null;
        if (flags != null && flags.isEmpty()) flags = null;
        if (spawns != null) {
            for (Spawn spawn : spawns) {
                spawn.onSave();
            }
        }
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

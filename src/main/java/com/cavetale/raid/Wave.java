package com.cavetale.raid;

import com.cavetale.core.editor.EditMenuAdapter;
import com.cavetale.mytems.Mytems;
import com.cavetale.raid.struct.Cuboid;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Data
final class Wave implements ShortInfo, EditMenuAdapter {
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
        MOBS(RED, () -> new ItemStack(Material.CREEPER_SPAWN_EGG)), // Kill all mobs
        GOAL(WHITE, () -> Mytems.ARROW_RIGHT.createIcon()), // Reach goal
        BOSS(DARK_RED, () -> new ItemStack(Material.DRAGON_HEAD)), // Boss fight
        TIME(BLUE, () -> new ItemStack(Material.CLOCK)), // Wait time
        ROADBLOCK(GREEN, () -> new ItemStack(Material.BARRIER)), // Roadblocks dictate timing
        WIN(GOLD, () -> new ItemStack(Material.WHITE_BANNER)), // Rewards, boss chest
        TITLE(WHITE, () -> new ItemStack(Material.NAME_TAG)), // Show title and finish fast
        ESCORT(LIGHT_PURPLE, () -> new ItemStack(Material.EMERALD)), // Escorts dictate timing
        CHOICE(GREEN, () -> Mytems.QUESTION_MARK.createIcon()), // Pick nextWave via region. "choice.X" => nextWave[X]
        RANDOM(GREEN, () -> Mytems.DICE.createIcon()), // Random next wave among nextWave[]
        DEFEND(RED, () -> new ItemStack(Material.EMERALD_BLOCK)); // Defend the escort

        public final TextColor textColor;
        public final String key;
        public final String humanName;
        public final Supplier<ItemStack> iconCreator;

        Type(final TextColor textColor, final Supplier<ItemStack> iconCreator) {
            this.textColor = textColor;
            this.key = name().toLowerCase();
            this.humanName = name().substring(0, 1) + name().substring(1).toLowerCase();
            this.iconCreator = iconCreator;
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

    @Override
    public ItemStack getMenuIcon() {
        return type.iconCreator.get();
    }

    @Override
    public List<Component> getTooltip() {
        Component sep = text(": ", DARK_GRAY);
        return List.of(text(type.humanName + " Wave", type.textColor),
                       join(separator(sep), text("Name", GRAY), text(name != null ? name : "-", WHITE)),
                       join(separator(sep), text("Place", GRAY), text(place != null ? place.getShortInfo() : "-", WHITE)),
                       join(separator(sep), text("Radius", GRAY), text(String.format("%.2f", radius), WHITE)));
    }
}

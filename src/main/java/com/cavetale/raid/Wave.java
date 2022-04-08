package com.cavetale.raid;

import com.cavetale.core.editor.EditMenuAdapter;
import com.cavetale.core.editor.EditMenuItem;
import com.cavetale.core.editor.EditMenuNode;
import com.cavetale.mytems.Mytems;
import com.cavetale.raid.struct.Cuboid;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
    @EditMenuItem(description = "Can be referenced by nextWave."
                  + " Never displayed to players")
    protected String name;
    protected Type type = Type.MOBS;
    @EditMenuItem(description = "Where the GOAL flag appears and joining player spawn")
    protected Place place = new Place();
    @EditMenuItem(description = "Flag radius for GOAL waves")
    private double radius = 0;
    @EditMenuItem(deletable = true,
                  description = "For boss waves")
    protected Boss boss;
    @EditMenuItem(description = "Required for TIME waves."
                  + " Some other waves will end the raid if the timer runs out.")
    protected int time = 0;
    @EditMenuItem(description = "Regular mobs enemies")
    private List<Spawn> spawns;
    @EditMenuItem(description = "Legacy, do not use!", deletable = true)
    private List<Roadblock> roadblocks;
    private List<Escort> escorts;
    @EditMenuItem(description = "Unused")
    private Set<Flag> flags;
    @EditMenuItem(description = "Mark boss_chest")
    protected Map<String, Cuboid> regions = new HashMap<>();
    @EditMenuItem(description = "One wave to jump to when this is over."
                  + " multiple entries for CHOICE and RANDOM")
    protected List<String> nextWave;
    @EditMenuItem(description = "Not currently editable")
    protected Map<ClipEvent, List<String>> clips = new EnumMap<>(ClipEvent.class);

    enum Type implements EditMenuAdapter {
        MOBS(RED,
             "Kill all mobs",
             () -> new ItemStack(Material.CREEPER_SPAWN_EGG)),
        GOAL(WHITE,
             "Reach the goal",
             () -> Mytems.ARROW_RIGHT.createIcon()),
        BOSS(DARK_RED,
             "Boss fight",
             () -> new ItemStack(Material.DRAGON_HEAD)),
        TIME(BLUE,
             "Wait or or survive some time",
             () -> new ItemStack(Material.CLOCK)),
        ROADBLOCK(GREEN,
                  "Deprecated, do not use!",
                  () -> new ItemStack(Material.BARRIER)),
        WIN(GOLD,
            "Final wave, victory! Rewards with boss chest.",
            () -> new ItemStack(Material.WHITE_BANNER)),
        TITLE(WHITE,
              "Display the displayName of the raid to every player",
              () -> new ItemStack(Material.NAME_TAG)),
        ESCORT(LIGHT_PURPLE,
               "Wave ends when all escorts have finished their action",
               () -> new ItemStack(Material.EMERALD)),
        CHOICE(GREEN,
               "Choose a path among nextWave. NOT IMPLEMENTED!",
               () -> Mytems.QUESTION_MARK.createIcon()),
        RANDOM(GREEN,
               "Random wave among nextWave. NOT IMPLEMENTED!",
               () -> Mytems.DICE.createIcon()),
        DEFEND(RED,
               "Defend the escort. NOT IMPLEMENTED!",
               () -> new ItemStack(Material.EMERALD_BLOCK));

        public final TextColor textColor;
        public final String key;
        public final String humanName;
        public final String description;
        public final Supplier<ItemStack> iconCreator;

        Type(final TextColor textColor, final String description, final Supplier<ItemStack> iconCreator) {
            this.textColor = textColor;
            this.key = name().toLowerCase();
            this.humanName = name().substring(0, 1) + name().substring(1).toLowerCase();
            this.description = description;
            this.iconCreator = iconCreator;
        }

        @Override
        public ItemStack getMenuIcon(EditMenuNode node) {
            return iconCreator.get();
        }

        @Override
        public List<Component> getTooltip(EditMenuNode node) {
            return List.of(text(name(), WHITE),
                           text(description, GRAY));
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
    public ItemStack getMenuIcon(EditMenuNode node) {
        return type.iconCreator.get();
    }

    @Override
    public List<Component> getTooltip(EditMenuNode node) {
        Component sep = text(": ", DARK_GRAY);
        return List.of(text(type.humanName + " Wave", type.textColor),
                       join(separator(sep), text("Name", GRAY), text(name != null ? name : "-", WHITE)),
                       join(separator(sep), text("Place", GRAY), text(place != null ? place.getShortInfo() : "-", WHITE)),
                       join(separator(sep), text("Radius", GRAY), text(String.format("%.2f", radius), WHITE)));
    }

    @Override
    public List<Object> getPossibleValues(EditMenuNode node, String fieldName, int valueIndex) {
        switch (fieldName) {
        case "nextWave":
            return node.getContext().getRootObject() instanceof Raid raid
                ? raid.waves.stream().map(Wave::getName).filter(Objects::nonNull).collect(Collectors.toList())
                : null;
        default: return null;
        }
    }

    @Override
    public Boolean validateValue(EditMenuNode node, String fieldName, Object newValue, int valueIndex) {
        switch (fieldName) {
        case "radius":
        case "time":
            return newValue instanceof Number number && number.doubleValue() >= 0.0;
        default: return null;
        }
    }


    @Override
    public Object createNewValue(EditMenuNode node, String fieldName, int valueIndex) {
        switch (fieldName) {
        case "spawns": {
            Spawn spawn = new Spawn();
            spawn.place.load(node.getContext().getPlayer().getLocation());
            return spawn;
        }
        default: return null;
        }
    }
}

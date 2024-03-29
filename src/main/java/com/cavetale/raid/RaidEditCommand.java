package com.cavetale.raid;

import com.cavetale.blockclip.BlockClip;
import com.cavetale.core.editor.EditMenuDelegate;
import com.cavetale.core.editor.EditMenuNode;
import com.cavetale.core.editor.Editor;
import com.cavetale.core.font.DefaultFont;
import com.cavetale.enemy.EnemyType;
import com.cavetale.raid.struct.Cuboid;
import com.cavetale.raid.util.Gui;
import com.cavetale.raid.util.Text;
import com.cavetale.raid.util.WorldEdit;
import com.destroystokyo.paper.MaterialTags;
import com.winthier.playercache.PlayerCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class RaidEditCommand implements TabExecutor {
    final RaidPlugin plugin;
    final ChatColor y = ChatColor.YELLOW;
    final ChatColor w = ChatColor.WHITE;
    final ChatColor g = ChatColor.GOLD;

    static class Wrong extends Exception {
        Wrong(@NonNull final String msg) {
            super(msg);
        }
    }

    enum Cmd {
        INFO,
        LIVE,
        NEW,
        TYPE,
        PLACE,
        WAVE,
        MOB,
        BOSS,
        SET,
        SAVE,
        RELOAD,
        TP,
        SKIP,
        DEBUG,
        ROADBLOCK,
        ESCORT,
        RESETJOIN,
        TEST,
        DISPLAYNAME,
        START,
        REGION,
        NEXTWAVE,
        NAME,
        CLIP,
        WAVECLIP,
        EDITOR;

        final String key;

        Cmd() {
            key = name().toLowerCase();
        }
    }

    enum ListCmd {
        ADD,
        REMOVE,
        LIST,
        TP,
        MOVE;

        final String key;

        ListCmd() {
            key = name().toLowerCase();
        }

        static ListCmd of(@NonNull String arg) {
            for (ListCmd cmd : ListCmd.values()) {
                if (arg.equals(cmd.key)) return cmd;
            }
            return null;
        }
    }

    enum EscortCmd {
        INFO,
        CREATE,
        REMOVE,
        PLACE,
        DIALOGUE,
        DISAPPEAR,
        TP;
    }

    @Override
    public boolean onCommand(final CommandSender sender,
                             final Command command,
                             final String alias,
                             final String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        final Cmd cmd;
        try {
            cmd = Cmd.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException iae) {
            help(sender);
            return true;
        }
        try {
            final boolean res;
            res = onCommand(sender, cmd, Arrays.copyOfRange(args, 1, args.length));
            if (!res) help(sender, cmd);
        } catch (Wrong wrong) {
            sender.sendMessage(ChatColor.RED + wrong.getMessage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender,
                                      final Command command,
                                      final String alias,
                                      final String[] args) {
        if (args.length == 0) return null;
        String cmds = args[0].toLowerCase();
        if (args.length == 1) {
            return Stream.of(Cmd.values())
                .map(Enum::name)
                .map(String::toLowerCase)
                .filter(f -> f.startsWith(cmds))
                .collect(Collectors.toList());
        }
        String arg = args[args.length - 1].toLowerCase();
        Cmd cmd;
        try {
            cmd = Cmd.valueOf(cmds.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return Collections.emptyList();
        }
        switch (cmd) {
        case WAVE:
            if (args.length == 2) {
                return Stream.of(ListCmd.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .filter(f -> f.startsWith(arg))
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        case MOB:
            if (args.length == 2) {
                return Stream.of(ListCmd.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .filter(f -> f.startsWith(arg))
                    .collect(Collectors.toList());
            }
            if (args.length == 3) {
                return Stream.concat(Stream.of(EnemyType.values()).map(Enum::name),
                                     Stream.of(EntityType.values()).map(Enum::name))
                    .map(String::toLowerCase)
                    .filter(f -> f.contains(arg))
                    .collect(Collectors.toList());
            }
            if (args.length == 4) {
                if (arg.isEmpty()) return Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9");
                try {
                    int i = Integer.parseInt(arg);
                    if (i < 1) return Collections.emptyList();
                    return Arrays.asList("" + i, "" + i + "0", "" + i + "00");
                } catch (NumberFormatException nfe) { }
                return Collections.emptyList();
            }
            if (args.length > 4) {
                if (!arg.contains("=")) {
                    return Stream.of("mount=", "hand=", "head=", "chest=", "legs=", "feet=",
                                     "baby=", "powered=", "health=", "damage=", "armor=", "toughness=",
                                     "speed=", "knockback=", "knockbackResist=", "scaling=")
                        .filter(f -> f.startsWith(arg))
                        .collect(Collectors.toList());
                }
                List<String> result = new ArrayList<>();
                for (EntityType entityType : EntityType.values()) {
                    Class<? extends Entity> type = entityType.getEntityClass();
                    if (type == null || !Mob.class.isAssignableFrom(type)) continue;
                    result.add("mount=" + entityType.name().toLowerCase());
                }
                for (Material mat : Material.values()) {
                    result.add("hand=" + mat.name().toLowerCase());
                }
                for (Material mat : MaterialTags.HEAD_EQUIPPABLE.getValues()) {
                    result.add("head=" + mat.name().toLowerCase());
                }
                for (Material mat : MaterialTags.CHEST_EQUIPPABLE.getValues()) {
                    result.add("chest=" + mat.name().toLowerCase());
                }
                for (Material mat : MaterialTags.LEGGINGS.getValues()) {
                    result.add("legs=" + mat.name().toLowerCase());
                }
                for (Material mat : MaterialTags.BOOTS.getValues()) {
                    result.add("feet=" + mat.name().toLowerCase());
                }
                for (String val : Arrays.asList("true", "false")) {
                    result.add("baby=" + val);
                    result.add("powered=" + val);
                }
                result.removeIf(it -> !it.startsWith(arg));
                return result;
            }
            return Collections.emptyList();
        case TYPE:
            if (args.length == 2) {
                return Stream.of(Wave.Type.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .filter(f -> f.startsWith(arg))
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        case ROADBLOCK:
            if (args.length == 2) {
                return Arrays.asList("add", "clear");
            }
            return Collections.emptyList();
        case BOSS:
            if (args.length == 2) {
                return Stream.of(EnemyType.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .filter(f -> f.contains(arg))
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        case CLIP:
            if (args.length == 2) {
                return Stream.of("list", "create")
                    .filter(f -> f.contains(arg))
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        case WAVECLIP:
            if (args.length == 2) {
                return Stream.of("list", "set")
                    .filter(f -> f.contains(arg)).collect(Collectors.toList());
            }
            if (args[1].equals("set")) {
                if (args.length == 3) {
                    return Stream.of(Wave.ClipEvent.values())
                        .map(Wave.ClipEvent::name).map(String::toLowerCase)
                        .filter(f -> f.contains(arg)).collect(Collectors.toList());
                }
                if (!(sender instanceof Player)) return Collections.emptyList();
                Player player = (Player) sender;
                Raid raid = plugin.raids.get(player.getWorld().getName());
                if (raid == null) return Collections.emptyList();
                Instance instance = plugin.raidInstance(raid);
                if (instance == null) return Collections.emptyList();
                return instance.clips.keySet().stream()
                    .filter(f -> f.toLowerCase().contains(arg)).collect(Collectors.toList());
            }
            return Collections.emptyList();
        default:
            return Collections.emptyList();
        }
    }

    boolean onCommand(CommandSender sender, Cmd cmd, String[] args) throws Wrong {
        switch (cmd) {
        case INFO: return infoCommand(requirePlayer(sender), args);
        case LIVE: return liveCommand(requirePlayer(sender), args);
        case NEW: return newCommand(requirePlayer(sender), args);
        case TYPE: return typeCommand(requirePlayer(sender), args);
        case PLACE: return placeCommand(requirePlayer(sender), args);
        case WAVE: return waveCommand(requirePlayer(sender), args);
        case MOB: return mobCommand(requirePlayer(sender), args);
        case BOSS: return bossCommand(requirePlayer(sender), args);
        case SET: return setCommand(requirePlayer(sender), args);
        case SAVE: return saveCommand(requirePlayer(sender), args);
        case RELOAD: return reloadCommand(sender, args);
        case TP: return tpCommand(requirePlayer(sender), args);
        case SKIP: return skipCommand(requirePlayer(sender), args);
        case DEBUG: return debugCommand(requirePlayer(sender), args);
        case ROADBLOCK: return roadblockCommand(requirePlayer(sender), args);
        case ESCORT: return escortCommand(requirePlayer(sender), args);
        case RESETJOIN: return resetJoinCommand(requirePlayer(sender), args);
        case TEST: return testCommand(requirePlayer(sender), args);
        case DISPLAYNAME: return displayNameCommand(requirePlayer(sender), args);
        case START: return startCommand(requirePlayer(sender), args);
        case REGION: return regionCommand(requirePlayer(sender), args);
        case NAME: return nameCommand(requirePlayer(sender), args);
        case NEXTWAVE: return nextWaveCommand(requirePlayer(sender), args);
        case CLIP: return clipCommand(requirePlayer(sender), args);
        case WAVECLIP: return waveClipCommand(requirePlayer(sender), args);
        case EDITOR: return editorCommand(requirePlayer(sender), args);
        default:
            throw new IllegalArgumentException(cmd.key);
        }
    }

    void help(CommandSender sender) {
        sender.sendMessage(y + "Usage");
        for (Cmd cmd : Cmd.values()) {
            help(sender, cmd);
        }
    }

    void help(CommandSender sender, Cmd cmd) {
        switch (cmd) {
        case INFO:
            sender.sendMessage(y + "/redit info - Print info on edit wave");
            break;
        case LIVE:
            sender.sendMessage(y + "/redit live - Print info on playing wave");
            break;
        case TYPE:
            sender.sendMessage(y + "/redit type "
                               + Stream.of(Wave.Type.values()).map(Enum::name)
                               .collect(Collectors.joining("|"))
                               + " - Set wave type.");
            break;
        case SET:
            sender.sendMessage(y + "/redit set radius [value] - Set radius");
            sender.sendMessage(y + "/redit set time [value] - Set timeSettings");
            break;
        case WAVE:
            sender.sendMessage(y + "/redit wave <index> - Select wave.");
            sender.sendMessage(y + "/redit wave add [index] - Add wave.");
            sender.sendMessage(y + "/redit wave remove - Remove current wave.");
            sender.sendMessage(y + "/redit wave list - List waves.");
            sender.sendMessage(y + "/redit wave tp - Teleport to wave.");
            sender.sendMessage(y + "/redit wave move <index> <index2> - Move wave.");
            break;
        case MOB:
            sender.sendMessage(y + "/redit mob add <type> [amount] [<key>=<value>] - Add mob.");
            sender.sendMessage(y + "/redit mob remove [index] - Remove mob.");
            sender.sendMessage(y + "/redit mob list - List mobs.");
            sender.sendMessage(y + "/redit mob tp <index> - Teleport to mob.");
            break;
        case BOSS:
            sender.sendMessage(y + "/redit boss - Clear wave boss.");
            sender.sendMessage(y + "/redit boss <type> - Set wave boss.");
            break;
        case TP:
            sender.sendMessage(y + "/redit tp <wave> - Teleport to wave location.");
            break;
        case SKIP:
            sender.sendMessage(y + "/redit skip [wave] - Skip to (next) wave.");
            break;
        case DEBUG:
            sender.sendMessage(y + "/redit debug - Toggle debug mode.");
            break;
        case ROADBLOCK:
            sender.sendMessage(y + "/redit roadblock add|clear - Edit roadblocks");
            break;
        case ESCORT:
            sender.sendMessage(y + "/redit escort <name> info|create|remove|place|dialogue|disappear - Edit escorts");
            break;
        case RESETJOIN:
            sender.sendMessage(y + "/redit resetjoin [name] - Allow player(s) to rejoin this raid");
            break;
        case DISPLAYNAME:
            sender.sendMessage(y + "/redit displayname [name] - Set the display name. With chat colors.");
            break;
        case START:
            sender.sendMessage(y + "/redit start - Skip the warmup phase");
            break;
        case REGION:
            sender.sendMessage(y + "/redit region add <name> - Add a region to this wave");
            sender.sendMessage(y + "/redit region remove <name> - Remove a region from this wave");
            break;
        case CLIP:
            sender.sendMessage(y + "/redit clip list - List all clips");
            sender.sendMessage(y + "/redit clip create <name> - Create a clip");
            break;
        case WAVECLIP:
            sender.sendMessage(y + "/redit waveclip list - List wave clips");
            sender.sendMessage(y + "/redit waveclip set init|enter|complete <clip...> - Set clips for this wave");
            break;
        case EDITOR:
            sender.sendMessage(y + "/redit editor - Open editor");
        default:
            sender.sendMessage(y + "/redit " + cmd.key);
            break;
        }
    }

    Player requirePlayer(@NonNull CommandSender sender) throws Wrong {
        if (!(sender instanceof Player)) {
            throw new Wrong("Player expected!");
        }
        return (Player) sender;
    }

    Raid requireRaid(@NonNull Player player) throws Wrong {
        String worldName = player.getWorld().getName();
        Raid raid = plugin.raids.get(worldName);
        if (raid == null) {
            throw new Wrong("No raid in this world!");
        }
        return raid;
    }

    Instance requireInstance(@NonNull Player player) throws Wrong {
        Instance instance = plugin.raidInstance(requireRaid(player));
        if (instance == null) {
            throw new Wrong("No instance in this world!");
        }
        return instance;
    }

    Wave requireWave(@NonNull Player player) throws Wrong {
        Raid raid = requireRaid(player);
        Instance instance = plugin.raidInstance(raid);
        try {
            return raid.waves.get(plugin.sessions.of(player).getEditWave());
        } catch (IndexOutOfBoundsException ioobe) {
            throw new Wrong("No wave selected!");
        }
    }

    Wave requireWave(@NonNull Player player, final int index) throws Wrong {
        Raid raid = requireRaid(player);
        Instance instance = plugin.raidInstance(raid);
        try {
            return raid.waves.get(index);
        } catch (IndexOutOfBoundsException ioobe) {
            throw new Wrong("No wave selected!");
        }
    }

    int requireInt(@NonNull String arg) throws Wrong {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException nfe) {
            throw new Wrong("Invalid int: " + arg);
        }
    }

    double requireDouble(@NonNull String arg) throws Wrong {
        try {
            return Double.parseDouble(arg);
        } catch (NumberFormatException nfe) {
            throw new Wrong("Invalid double: " + arg);
        }
    }

    boolean infoCommand(@NonNull Player player, String[] args) throws Wrong {
        Raid raid = requireRaid(player);
        Instance inst = plugin.raidInstance(raid);
        player.sendMessage(y + "Raid: " + w + raid.worldName + ": " + Text.colorize(raid.displayName));
        int waveIndex = plugin.sessions.of(player).getEditWave();
        Wave wave = inst.getWave(waveIndex);
        if (wave == null) {
            player.sendMessage(y + "No edit wave");
            return true;
        }
        player.sendMessage(y + "Editing: " + w + waveIndex + "/" + (raid.waves.size() - 1) + " " + y + ShortInfo.of(wave));
        player.sendMessage(y + "Time: " + wave.getTime());
        for (Spawn spawn : wave.getSpawns()) {
            player.sendMessage(y + "Spawn: " + spawn.getShortInfo());
        }
        for (Escort escort : wave.getEscorts()) {
            player.sendMessage(y + "Escort: " + escort.getShortInfo());
        }
        if (wave != null) {
            for (Map.Entry<String, Cuboid> entry : wave.getRegions().entrySet()) {
                String name = entry.getKey();
                Cuboid region = entry.getValue();
                player.sendMessage(y + "Region: " + name + ": " + region);
            }
        }
        if (wave.getNextWave() != null) {
            player.sendMessage(y + "NextWave: " + wave.getNextWave());
        }
        return true;
    }

    boolean liveCommand(@NonNull Player player, String[] args) throws Wrong {
        Raid raid = requireRaid(player);
        Instance inst = plugin.raidInstance(raid);
        player.sendMessage(y + "Raid: " + w + raid.worldName + ": " + Text.colorize(raid.displayName));
        int waveIndex = inst.getWaveIndex();
        player.sendMessage(y + "Playing: " + w + waveIndex + "/" + (raid.waves.size() - 1) + " " + y + ShortInfo.of(inst.getWave()));
        for (Map.Entry<String, EscortMarker> entry : inst.getEscorts().entrySet()) {
            String name = entry.getKey();
            EscortMarker escortMarker = entry.getValue();
            player.sendMessage(y + "Escort: " + name + ": " + escortMarker.debugString());
        }
        return true;
    }

    boolean newCommand(@NonNull Player player, String[] args) throws Wrong {
        String worldName = player.getWorld().getName();
        if (plugin.raids.get(worldName) != null) {
            throw new Wrong("World already has a raid!");
        }
        Raid raid = new Raid(worldName);
        plugin.raids.put(worldName, raid);
        plugin.saveRaid(raid);
        player.sendMessage(y + "Raid `" + worldName + "' created.");
        return true;
    }

    boolean typeCommand(@NonNull Player player, String[] args) throws Wrong {
        if (args.length != 1) return false;
        Wave.Type type;
        try {
            type = Wave.Type.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new Wrong("Invalid wave type: " + args[0] + " ("
                            + Stream.of(Wave.Type.values()).map(Enum::name)
                            .collect(Collectors.joining(", "))
                            + ")");
        }
        Raid raid = requireRaid(player);
        Wave wave = requireWave(player);
        wave.type = type;
        if (type == Wave.Type.GOAL && wave.place == null) {
            wave.place = Place.of(player.getLocation());
        }
        if (type == Wave.Type.GOAL && wave.getRadius() == 0) {
            wave.setRadius(7);
        }
        plugin.saveRaid(raid);
        player.sendMessage(y + "Wave #" + raid.waves.indexOf(wave) + " type=" + type);
        return true;
    }

    boolean placeCommand(@NonNull Player player, String[] args) throws Wrong {
        if (args.length != 0) return false;
        Place place = Place.of(player.getLocation());
        Raid raid = requireRaid(player);
        Wave wave = requireWave(player);
        wave.place = place;
        plugin.saveRaid(raid);
        player.sendMessage(y + "Wave #" + raid.waves.indexOf(wave)
                           + " place=" + ShortInfo.of(place));
        return true;
    }

    boolean waveCommand(Player player, String[] args) throws Wrong {
        Raid raid = requireRaid(player);
        Instance inst = plugin.raidInstance(raid);
        if (args.length == 0) {
            int waveIndex = plugin.sessions.of(player).getEditWave();
            if (waveIndex >= 0 && waveIndex < raid.waves.size()) {
                Wave wave = raid.waves.get(waveIndex);
                player.sendMessage(y + "Wave " + waveIndex
                                   + ": " + ShortInfo.of(wave));
            }
            return false;
        }
        // Select Wave
        if (args.length == 1) {
            try {
                int waveIndex = Integer.parseInt(args[0]);
                Wave wave = requireWave(player, waveIndex);
                plugin.sessions.of(player).setEditWave(waveIndex);
                player.sendMessage(y + "Wave " + waveIndex
                                   + " selected: " + ShortInfo.of(wave));
                return true;
            } catch (NumberFormatException nfe) { }
        }
        ListCmd cmd = ListCmd.of(args[0]);
        if (cmd == null) return false;
        switch (cmd) {
        case ADD: {
            if (args.length > 2) return false;
            Wave wave = new Wave();
            wave.place = Place.of(player.getLocation());
            int index;
            if (args.length >= 2) {
                index = requireInt(args[1]);
                if (index < 0 || index > raid.waves.size()) {
                    throw new Wrong("Invalid index: " + index);
                }
            } else {
                index = raid.waves.size();
            }
            plugin.sessions.of(player).setEditWave(index);
            raid.waves.add(index, wave);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Wave #" + index + " created.");
            return true;
        }
        case REMOVE: {
            if (args.length > 1) return false;
            Wave wave = requireWave(player);
            raid.waves.remove(wave);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Wave #" + plugin.sessions.of(player).getEditWave() + " removed: "
                               + ShortInfo.of(wave));
            return true;
        }
        case LIST: {
            List<Component> components = new ArrayList<>(raid.waves.size());
            for (int i = 0; i < raid.waves.size(); i += 1) {
                Wave wave = raid.waves.get(i);
                int count = wave.getSpawns().size();
                components.add(Component.text(i + (wave.name != null ? "[" + wave.name + "]" : "")
                                              + ":" + wave.type.key
                                              + (count > 0 ? "(" + count + ")" : ""),
                                              wave.type.textColor)
                               .hoverEvent(HoverEvent.showText(Component.text(wave.getShortInfo())))
                               .clickEvent(ClickEvent.runCommand("/raidedit wave " + i)));
            }
            JoinConfiguration joinConfiguration = JoinConfiguration.builder()
                .prefix(Component.text(raid.waves.size() + " waves:", NamedTextColor.YELLOW))
                .separator(Component.space())
                .build();
            player.sendMessage(Component.join(joinConfiguration, components));
            return true;
        }
        case TP: {
            if (args.length > 1) return false;
            Wave wave = requireWave(player);
            player.teleport(wave.place.toLocation(inst.getWorld()));
            player.sendMessage(y + "Teleported to wave #" + plugin.sessions.of(player).getEditWave());
            return true;
        }
        case MOVE: {
            if (args.length != 3) return false;
            int indexA = requireInt(args[1]);
            int indexB = requireInt(args[2]);
            if (indexA < 0 || indexA >= raid.waves.size()) throw new Wrong("Out of bounds: " + indexA);
            if (indexB < 0 || indexB >= raid.waves.size()) throw new Wrong("Out of bounds: " + indexB);
            Wave wave = raid.waves.remove(indexA);
            raid.waves.add(indexB, wave);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Wave #" + indexA + " moved to #" + indexB);
            return true;
        }
        default: throw new IllegalArgumentException(cmd.key);
        }
    }

    boolean mobCommand(@NonNull Player player, String[] args) throws Wrong {
        if (args.length == 0) return false;
        Wave wave = requireWave(player);
        Raid raid = requireRaid(player);
        Instance inst = plugin.raidInstance(raid);
        ListCmd cmd = ListCmd.of(args[0]);
        if (cmd == null) return false;
        switch (cmd) {
        case ADD: {
            if (args.length < 2) return false;
            String entityType = args[1];
            int amount = 1;
            if (args.length >= 3) {
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException nfe) {
                    amount = -1;
                }
                if (amount < 1) {
                    throw new Wrong("Bad amount: " + args[2]);
                }
            }
            Spawn spawn = new Spawn(entityType, player.getLocation(), amount);
            for (int i = 3; i < args.length; i += 1) {
                String arg = args[i];
                String[] toks = arg.split("=", 2);
                if (toks.length != 2) throw new Wrong("Invalid setting: " + arg);
                String key = toks[0];
                String value = toks[1];
                try {
                    switch (key) {
                    case "mount":
                        spawn.mount = EntityType.valueOf(value.toUpperCase());
                        break;
                    case "hand":
                        spawn.hand = Material.valueOf(value.toUpperCase());
                        break;
                    case "head":
                        spawn.helmet = Material.valueOf(value.toUpperCase());
                        break;
                    case "chest":
                        spawn.chestplate = Material.valueOf(value.toUpperCase());
                        break;
                    case "legs":
                        spawn.leggings = Material.valueOf(value.toUpperCase());
                        break;
                    case "feet":
                        spawn.boots = Material.valueOf(value.toUpperCase());
                        break;
                    case "baby":
                        spawn.baby = Boolean.parseBoolean(value);
                        break;
                    case "powered":
                        spawn.powered = Boolean.parseBoolean(value);
                        break;
                    case "health":
                        spawn.getAttributes().put(Attribute.GENERIC_MAX_HEALTH, Double.parseDouble(value));
                        break;
                    case "damage":
                        spawn.getAttributes().put(Attribute.GENERIC_ATTACK_DAMAGE, Double.parseDouble(value));
                        break;
                    case "armor":
                        spawn.getAttributes().put(Attribute.GENERIC_ARMOR, Double.parseDouble(value));
                        break;
                    case "toughness":
                        spawn.getAttributes().put(Attribute.GENERIC_ARMOR_TOUGHNESS, Double.parseDouble(value));
                        break;
                    case "speed":
                        spawn.getAttributes().put(Attribute.GENERIC_MOVEMENT_SPEED, Double.parseDouble(value));
                        break;
                    case "knockback":
                        spawn.getAttributes().put(Attribute.GENERIC_ATTACK_KNOCKBACK, Double.parseDouble(value));
                        break;
                    case "knockbackResist":
                        spawn.getAttributes().put(Attribute.GENERIC_KNOCKBACK_RESISTANCE, Double.parseDouble(value));
                        break;
                    case "scaling":
                        spawn.scaling = Double.parseDouble(value);
                        break;
                    default: throw new Wrong("Illegal key: " + key);
                    }
                } catch (NumberFormatException nfe) {
                    throw new Wrong("Invalid number: " + key + " = " + value);
                } catch (IllegalArgumentException iae) {
                    throw new Wrong("Invalid value: " + key + " = " + value);
                }
            }
            List<Spawn> list = wave.getSpawns();
            int index = list.size();
            list.add(spawn);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Mob #" + index + " added: " + ShortInfo.of(spawn));
            return true;
        }
        case REMOVE: {
            if (args.length > 2) return false;
            List<Spawn> list = wave.getSpawns();
            int index = list.size() - 1;
            if (args.length >= 2) {
                try {
                    index = Integer.parseInt(args[1]);
                } catch (NumberFormatException iae) {
                    index = -1;
                }
            }
            if (index < 0 || index >= list.size()) {
                throw new Wrong("Invalid index: " + args[1]);
            }
            Spawn spawn = list.remove(index);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Mob #" + index + " removed: " + ShortInfo.of(spawn));
            return true;
        }
        case LIST: {
            List<Spawn> list = wave.getSpawns();
            int size = list.size();
            player.sendMessage(y + "Wave #" + plugin.sessions.of(player).getEditWave() + ": " + size + " mobs:");
            for (int i = 0; i < size; i += 1) {
                player.sendMessage(i + ") " + y + ShortInfo.of(list.get(i)));
            }
            return true;
        }
        case TP: {
            if (args.length != 2) return false;
            int index;
            try {
                index = Integer.parseInt(args[1]);
            } catch (NumberFormatException iae) {
                index = -1;
            }
            List<Spawn> list = wave.getSpawns();
            if (index < 0 || index >= list.size()) {
                throw new Wrong("Invalid index: " + args[1]);
            }
            Spawn spawn = list.get(index);
            player.teleport(spawn.place.toLocation(inst.getWorld()));
            player.sendMessage(y + "Teleported to mob #" + index);
            return true;
        }
        default: throw new IllegalArgumentException(cmd.key);
        }
    }

    boolean bossCommand(@NonNull Player player, String[] args) throws Wrong {
        if (args.length > 1) return false;
        Raid raid = requireRaid(player);
        Wave wave = requireWave(player);
        if (args.length == 0) {
            wave.boss = null;
            player.sendMessage(y + "Wave boss=-");
        } else if (args.length == 1) {
            EnemyType type;
            try {
                type = EnemyType.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new Wrong("Invalid boss type: " + args[0] + "("
                                + Stream.of(EnemyType.values()).map(Enum::name)
                                .collect(Collectors.joining("|"))
                                + ")");
            }
            Boss boss = new Boss(type);
            wave.boss = boss;
            player.sendMessage(y + "Wave boss=" + boss.getShortInfo());
        }
        plugin.saveRaid(raid);
        return true;
    }

    boolean setCommand(@NonNull Player player, String[] args) throws Wrong {
        Wave wave = requireWave(player);
        if (args.length < 1) return false;
        final String key = args[0];
        final String value = args.length < 2 ? null
            : Stream.of(Arrays.copyOfRange(args, 1, args.length))
            .collect(Collectors.joining(" "));
        switch (key) {
        case "radius":
            wave.setRadius(value == null ? 0 : requireDouble(value));
            player.sendMessage(y + "Set radius=" + wave.getRadius());
            return true;
        case "time":
            wave.setTime(value == null ? 0 : requireInt(value));
            player.sendMessage(y + "Set time=" + wave.getTime());
            return true;
        default: throw new Wrong("Unknown key: " + key);
        }
    }

    boolean saveCommand(@NonNull Player player, String[] args) throws Wrong {
        if (args.length != 0) return false;
        Raid raid = requireRaid(player);
        boolean res = plugin.saveRaid(raid);
        player.sendMessage(y + "Saving raid: " + raid.worldName + ": " + res);
        return true;
    }

    boolean reloadCommand(@NonNull CommandSender sender, String[] args) throws Wrong {
        if (args.length != 0) return false;
        for (Instance inst : plugin.instances.values()) {
            inst.clear();
        }
        plugin.raids.clear();
        plugin.instances.clear();
        plugin.loadRaids();
        for (World world : plugin.getServer().getWorlds()) {
            plugin.raidInstance(world);
        }
        sender.sendMessage(y + "Raids reloaded.");
        return true;
    }

    boolean tpCommand(@NonNull Player player, String[] args) throws Wrong {
        if (args.length != 1) return false;
        Raid raid = requireRaid(player);
        int index = requireInt(args[0]);
        Wave wave = raid.waves.get(index); // aioobe
        Instance inst = plugin.raidInstance(raid);
        player.teleport(wave.place.toLocation(inst.getWorld()));
        player.sendMessage(y + "Teleported to wave " + index + ".");
        return true;
    }

    boolean skipCommand(@NonNull Player player, String[] args) throws Wrong {
        if (args.length > 1) return false;
        Raid raid = requireRaid(player);
        Instance inst = plugin.raidInstance(raid);
        if (inst.getPhase() == Phase.WARMUP) {
            inst.startRun();
        }
        if (args.length == 0) {
            inst.waveComplete = true;
            player.sendMessage(y + "Skipping wave...");
        } else {
            int newWave = Integer.parseInt(args[0]);
            inst.skipWave(newWave);
            player.sendMessage(y + "Jumping to wave " + newWave + ".");
        }
        return true;
    }

    boolean debugCommand(@NonNull Player player, String[] args) throws Wrong {
        Raid raid = requireRaid(player);
        Instance inst = plugin.raidInstance(raid);
        inst.debug = !inst.debug;
        if (!inst.debug) inst.clearDebug();
        player.sendMessage(y + "Debug mode: " + inst.debug);
        return true;
    }

    boolean roadblockCommand(Player player, String[] args) throws Wrong {
        if (args.length != 1) return false;
        Session session = plugin.sessions.of(player);
        switch (args[0]) {
        case "clear":
            requireWave(player).getRoadblocks().clear();
            player.sendMessage(y + "Roadblocks cleared");
            return true;
        case "add":
            if (session.isPlacingRoadblocks()) {
                session.setPlacingRoadblocks(false);
                Instance inst = plugin.raidInstance(player.getWorld());
                player.sendMessage(y + "Roadblock placement disabled.");
                if (inst != null) {
                    plugin.saveRaid(inst.getRaid());
                    Wave wave = inst.getWave(session.getEditWave());
                    if (wave != null) {
                        for (Roadblock roadblock : wave.getRoadblocks()) {
                            roadblock.unblock(inst.getWorld());
                        }
                    }
                }
            } else {
                session.setPlacingRoadblocks(true);
                player.sendMessage(y + "Roadblock placement enabled. Place roadblocks, break bridges. Re-enter command to disable.");
            }
            return true;
        default: throw new Wrong("Unknown command: " + args[0]);
        }
    }

    boolean escortCommand(Player player, String[] args) throws Wrong {
        Raid raid = requireRaid(player);
        if (args.length == 0) {
            Wave wave = requireWave(player);
            List<Escort> escorts = wave.getEscorts();
            player.sendMessage(y + "Wave has " + escorts.size() + " escorts:");
            for (Escort escort : escorts) {
                player.sendMessage("- " + escort.getShortInfo());
            }
            return true;
        }
        if (args.length < 2) return false;
        Wave wave = requireWave(player);
        String escortArg = args[1];
        EscortCmd escortCmd;
        try {
            escortCmd = EscortCmd.valueOf(escortArg.toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new Wrong("Invalid escort command: " + escortArg);
        }
        Escort escort = null;
        String name = args[0];
        for (Escort it : wave.getEscorts()) {
            if (name.equals(it.getName())) {
                escort = it;
                break;
            }
        }
        if (escortCmd != EscortCmd.CREATE && escort == null) throw new Wrong("Escort not found: " + name);
        switch (escortCmd) {
        case INFO: {
            player.sendMessage(y + "Escort: " + escort.getShortInfo());
            return true;
        }
        case CREATE: {
            if (args.length != 2) return false;
            if (escort != null) {
                wave.getEscorts().remove(escort);
            }
            escort = new Escort();
            escort.setName(name);
            wave.getEscorts().add(escort);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Escort created: " + name);
            return true;
        }
        case REMOVE: {
            wave.getEscorts().remove(escort);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Escort removed: " + escort.getName());
            return true;
        }
        case PLACE: {
            if (args.length != 2) return false;
            Place place = Place.of(player.getLocation());
            escort.setPlace(place);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Updated escort place: " + place.getShortInfo());
            return true;
        }
        case DIALOGUE: {
            List<String> dialogue = escort.getDialogue();
            if (args.length < 3) return false;
            ListCmd listCmd = ListCmd.of(args[2]);
            if (listCmd == null) throw new Wrong("Invalid dialogue command: " + args[2]);
            switch (listCmd) {
            case LIST: {
                if (dialogue == null) {
                    throw new Wrong("No dialogue!");
                }
                player.sendMessage("" + dialogue.size() + " lines:");
                int i = 0;
                for (String line : dialogue) {
                    player.sendMessage("" + i++ + ") " + line);
                }
                return true;
            }
            case ADD: {
                if (args.length < 4) return false;
                if (dialogue == null) {
                    dialogue = new ArrayList<>();
                    escort.setDialogue(dialogue);
                }
                String line = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                dialogue.add(line);
                plugin.saveRaid(raid);
                player.sendMessage(y + "Dialogue added: " + line);
                return true;
            }
            case REMOVE: {
                if (args.length != 4) return false;
                if (dialogue == null) {
                    throw new Wrong("No dialogue!");
                }
                int index = requireInt(args[3]);
                String removed;
                try {
                    removed = dialogue.remove(index);
                } catch (IndexOutOfBoundsException ioobe) {
                    throw new Wrong("Invalid dialogue index: " + index);
                }
                player.sendMessage(y + "Dialogue removed: " + removed);
                return true;
            }
            default:
                throw new Wrong("Invalid dialogue command: " + listCmd);
            }
        }
        case DISAPPEAR: {
            if (args.length != 2) return false;
            boolean newValue = !escort.isDisappear();
            escort.setDisappear(newValue);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Disappear: " + newValue);
            return true;
        }
        case TP: {
            if (args.length != 2) return false;
            if (escort.getPlace() == null) throw new Wrong("Escort " + escort.getName() + " has no place!");
            Location location = escort.getPlace().toLocation(raid.getWorld());
            player.teleport(location);
            player.sendMessage(y + "Teleport to escort location: " + escort.getName());
            return true;
        }
        default: throw new IllegalStateException("escortCmd=" + escortCmd);
        }
    }

    boolean resetJoinCommand(Player player, String[] args) throws Wrong {
        Instance instance = plugin.raidInstance(player.getWorld());
        if (instance == null) {
            throw new Wrong("No raid instance in this world!");
        }
        if (args.length == 1) {
            String name = args[0];
            UUID uuid = PlayerCache.uuidForName(name);
            if (uuid == null) {
                throw new Wrong("Player not found: " + name);
            }
            if (!instance.alreadyJoined.remove(uuid)) {
                throw new Wrong("Player not marked as joined: " + name);
            }
            player.sendMessage(ChatColor.YELLOW + "Player join status reset: " + name);
        } else if (args.length == 0) {
            if (instance.alreadyJoined.isEmpty()) {
                throw new Wrong("No players were marked as already joined!");
            }
            int count = instance.alreadyJoined.size();
            instance.alreadyJoined.clear();
            player.sendMessage(ChatColor.YELLOW + "All player join statuses reset: " + count);
        } else {
            return false;
        }
        return true;
    }

    boolean testCommand(Player player, String[] args) throws Wrong {
        Gui gui = new Gui(plugin);
        Component title = Component.text()
            .append(DefaultFont.guiOverlay(DefaultFont.GUI_RAID_REWARD))
            .append(Component.text("This is a test!", NamedTextColor.WHITE))
            .build();
        gui.title(title);
        gui.size(3 * 9);
        gui.open(player);
        return true;
    }

    boolean displayNameCommand(Player player, String[] args) throws Wrong {
        Raid raid = requireRaid(player);
        raid.displayName = String.join(" ", args);
        plugin.saveRaid(raid);
        player.sendMessage("Display name updated: " + Text.colorize(raid.displayName));
        return true;
    }

    boolean startCommand(Player player, String[] args) throws Wrong {
        Instance instance = plugin.raidInstance(player.getWorld());
        if (instance == null) {
            throw new Wrong("No raid instance in this world!");
        }
        if (instance.getPhase() != Phase.WARMUP) throw new Wrong("Not in warmup phase!");
        instance.startRun();
        player.sendMessage("Run started!");
        return true;
    }

    boolean regionCommand(Player player, String[] args) throws Wrong {
        if (args.length == 0) return false;
        switch (args[0]) {
        case "add": {
            if (args.length != 2) return false;
            Raid raid = requireRaid(player);
            Wave wave = requireWave(player);
            String name = args[1];
            Cuboid selection = WorldEdit.getSelection(player);
            if (selection == null) throw new Wrong("Make a WorldEdit selection first!");
            wave.regions.put(name, selection);
            plugin.saveRaid(raid);
            player.sendMessage(Component.text("Region added: " + name + ": " + selection).color(NamedTextColor.YELLOW));
            return true;
        }
        case "remove": {
            Raid raid = requireRaid(player);
            Wave wave = requireWave(player);
            String name = args[1];
            Cuboid region = wave.regions.remove(name);
            if (region != null) {
                plugin.saveRaid(raid);
                player.sendMessage(Component.text("Region removed: " + name + ": " + region).color(NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("Region not found: " + name).color(NamedTextColor.RED));
            }
            return true;
        }
        default: return false;
        }
    }

    boolean nameCommand(Player player, String[] args) throws Wrong {
        Raid raid = requireRaid(player);
        Wave wave = requireWave(player);
        if (args.length == 0) {
            if (wave.getName() == null) {
                throw new Wrong("Wave has no name set!");
            }
            wave.setName(null);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Wave name reset");
            return true;
        } else if (args.length == 1) {
            if (args[0].equals(wave.getName())) {
                throw new Wrong("Wave already named " + args[0] + "!");
            }
            wave.setName(args[0]);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Wave name set: " + wave.getName());
            return true;
        } else {
            return false;
        }
    }

    boolean nextWaveCommand(Player player, String[] args) throws Wrong {
        Raid raid = requireRaid(player);
        Wave wave = requireWave(player);
        if (args.length == 0) {
            if (wave.getNextWave() == null) {
                throw new Wrong("Wave does not have nextWave set!");
            }
            wave.setNextWave(null);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Wave nextWave reset");
            return true;
        } else {
            wave.setNextWave(Arrays.asList(args));
            plugin.saveRaid(raid);
            player.sendMessage(y + "Wave nextWave set to: " + wave.getNextWave());
            return true;
        }
    }

    boolean clipCommand(Player player, String[] args) throws Wrong {
        if (args.length == 0) return false;
        Instance instance = requireInstance(player);
        switch (args[0]) {
        case "list": {
            if (args.length != 1) return false;
            player.sendMessage("Raid has " + instance.clips.size() + " clips: "
                               + String.join(" ", instance.clips.keySet()));
            return true;
        }
        case "create": {
            if (args.length != 2) return false;
            String name = args[1];
            Cuboid selection = WorldEdit.getSelection(player);
            if (selection == null) throw new Wrong("No selection!");
            Block a = selection.getMin().toBlock(instance.getWorld());
            Block b = selection.getMax().toBlock(instance.getWorld());
            BlockClip clip = BlockClip.copyOf(a, b);
            instance.setClip(name, clip);
            player.sendMessage("Clip created: " + name);
            return true;
        }
        default: return false;
        }
    }

    boolean waveClipCommand(Player player, String[] args) throws Wrong {
        Instance instance = requireInstance(player);
        Wave wave = requireWave(player);
        if (args.length == 0) return false;
        switch (args[0]) {
        case "list":
            if (args.length != 1) return false;
            for (Wave.ClipEvent clipEvent : Wave.ClipEvent.values()) {
                player.sendMessage(clipEvent + ": " + wave.clips.get(clipEvent));
            }
            return true;
        case "set": {
            if (args.length < 2) return false;
            String eventArg = args[1];
            String[] otherArgs = Arrays.copyOfRange(args, 2, args.length);
            Wave.ClipEvent clipEvent;
            try {
                clipEvent = Wave.ClipEvent.valueOf(eventArg.toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new Wrong("Unknown clip event: " + eventArg);
            }
            if (otherArgs.length == 0) {
                wave.clips.remove(clipEvent);
                player.sendMessage("Event " + clipEvent + " reset");
            } else {
                List<String> newValue = List.of(otherArgs);
                wave.clips.put(clipEvent, newValue);
                player.sendMessage("Event " + clipEvent + " set to " + newValue);
            }
            plugin.saveRaid(instance.raid);
            return true;
        }
        default: return false;
        }
    }

    private boolean editorCommand(Player player, String[] args) throws Wrong {
        if (args.length != 0) return false;
        Instance instance = requireInstance(player);
        Editor.get().open(plugin, player, instance.raid, new EditMenuDelegate() {
                @Override
                public Runnable getSaveFunction(EditMenuNode node) {
                    return () -> plugin.saveRaid(instance.raid);
                }
            });
        return true;
    }
}

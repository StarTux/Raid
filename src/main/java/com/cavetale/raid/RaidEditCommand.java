package com.cavetale.raid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class RaidEditCommand implements TabExecutor {
    final RaidPlugin plugin;
    final ChatColor y = ChatColor.YELLOW;

    static class Wrong extends Exception {
        Wrong(@NonNull final String msg) {
            super(msg);
        }
    }

    enum Cmd {
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
        SKULLS,
        ROADBLOCK,
        ESCORT;

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
        DISAPPEAR;
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
        case MOB:
            if (args.length == 2) {
                return Stream.of(ListCmd.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .filter(f -> f.startsWith(arg))
                    .collect(Collectors.toList());
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
        default:
            return Collections.emptyList();
        }
    }

    boolean onCommand(CommandSender sender, Cmd cmd, String[] args) throws Wrong {
        switch (cmd) {
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
        case SKULLS: return skullsCommand(requirePlayer(sender), args);
        case ROADBLOCK: return roadblockCommand(requirePlayer(sender), args);
        case ESCORT: return escortCommand(requirePlayer(sender), args);
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
        case TYPE:
            sender.sendMessage(y + "/raidedit type "
                               + Stream.of(Wave.Type.values()).map(Enum::name)
                               .collect(Collectors.joining("|"))
                               + " - Set wave type.");
            break;
        case SET:
            sender.sendMessage(y + "/raidedit set radius [value] - Set radius");
            sender.sendMessage(y + "/raidedit set time [value] - Set timeSettings");
            break;
        case WAVE:
            sender.sendMessage(y + "/raidedit wave <index> - Select wave.");
            sender.sendMessage(y + "/raidedit wave add [index] - Add wave.");
            sender.sendMessage(y + "/raidedit wave remove - Remove current wave.");
            sender.sendMessage(y + "/raidedit wave list - List waves.");
            sender.sendMessage(y + "/raidedit wave tp - Teleport to wave.");
            sender.sendMessage(y + "/raidedit wave move <index> <index2> - Move wave.");
            break;
        case MOB:
            sender.sendMessage(y + "/raidedit mob add <type> [amount] - Add mob.");
            sender.sendMessage(y + "/raidedit mob remove [index] - Remove mob.");
            sender.sendMessage(y + "/raidedit mob list - List mobs.");
            sender.sendMessage(y + "/raidedit mob tp <index> - Teleport to mob.");
            break;
        case BOSS:
            sender.sendMessage(y + "/raidedit boss - Clear wave boss.");
            sender.sendMessage(y + "/raidedit boss <type> - Set wave boss.");
            break;
        case TP:
            sender.sendMessage(y + "/raidedit tp <wave> - Teleport to wave location.");
            break;
        case SKIP:
            sender.sendMessage(y + "/raidedit skip [wave] - Skip to (next) wave.");
            break;
        case DEBUG:
            sender.sendMessage(y + "/raidedit debug - Toggle debug mode.");
            break;
        case ROADBLOCK:
            sender.sendMessage(y + "/raidedit roadblock add|clear - Edit roadblocks");
            break;
        case ESCORT:
            sender.sendMessage(y + "/raidedit escort <name> info|create|remove|place|dialogue|disappear - Edit escorts");
            break;
        default:
            sender.sendMessage(y + "/raidedit " + cmd.key);
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
        if (type == Wave.Type.GOAL && wave.radius == 0) {
            wave.radius = 2;
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
            ComponentBuilder cb = new ComponentBuilder(y + "" + y + raid.waves.size() + " waves:");
            for (int i = 0; i < raid.waves.size(); i += 1) {
                Wave wave = raid.waves.get(i);
                int count = wave.getSpawns().size();
                cb.append(" " + wave.type.color + i + ":" + wave.type.key + (count > 0 ? "(" + count + ")" : ""));
                String tooltip = wave.getShortInfo();
                cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(tooltip)));
                cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/raidedit wave " + i));
            }
            player.sendMessage(cb.create());
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
            if (args.length < 2 || args.length > 3) return false;
            EntityType et;
            try {
                et = EntityType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new Wrong("No such EntityType: " + args[1]);
            }
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
            Spawn spawn = new Spawn(et, player.getLocation(), amount);
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
            Boss.Type type;
            try {
                type = Boss.Type.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new Wrong("Invalid boss type: " + args[0] + "("
                                + Stream.of(Boss.Type.values()).map(Enum::name)
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
            wave.radius = value == null ? 0 : requireDouble(value);
            player.sendMessage(y + "Set radius=" + wave.radius);
            return true;
        case "time":
            wave.time = value == null ? 0 : requireInt(value);
            player.sendMessage(y + "Set time=" + wave.time);
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

    boolean skullsCommand(@NonNull Player player, String[] args) throws Wrong {
        Raid raid = requireRaid(player);
        Instance inst = plugin.raidInstance(raid);
        inst.giveSkulls(player);
        player.sendMessage("Skulls given.");
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
            plugin.saveRaid(requireRaid(player));
            player.sendMessage(y + "Escort created: " + name);
            return true;
        }
        case PLACE: {
            if (args.length != 2) return false;
            Place place = Place.of(player.getLocation());
            escort.setPlace(place);
            plugin.saveRaid(requireRaid(player));
            player.sendMessage(y + "Updated escort place: " + place.getShortInfo());
            return true;
        }
        case DIALOGUE: {
            List<String> dialogue = escort.getDialogue();
            if (args.length > 2) {
                if (dialogue == null) {
                    dialogue = new ArrayList<>();
                    escort.setDialogue(dialogue);
                }
                String line = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                dialogue.add(line);
                plugin.saveRaid(requireRaid(player));
                player.sendMessage(y + "Dialogue added: " + line);
            } else {
                if (dialogue == null) {
                    throw new Wrong("No dialogue!");
                }
                player.sendMessage("" + dialogue.size() + " lines:");
                for (String line : dialogue) {
                    player.sendMessage("- " + line);
                }
            }
            return true;
        }
        case DISAPPEAR:
            if (args.length != 2) return false;
            boolean newValue = !escort.isDisappear();
            escort.setDisappear(newValue);
            plugin.saveRaid(requireRaid(player));
            player.sendMessage(y + "Disappear: " + newValue);
            return true;
        default: throw new IllegalStateException("escortCmd=" + escortCmd);
        }
    }
}

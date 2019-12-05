package com.cavetale.raid;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class RaidCommand implements CommandExecutor {
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
        DEBUG;

        final String key;

        Cmd() {
            key = name().toLowerCase();
        }
    }

    enum ListCmd {
        ADD,
        REMOVE,
        LIST;

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
        case DEBUG: return debugCommand(requirePlayer(sender), args);
        default:
            throw new IllegalArgumentException(cmd.key);
        }
    }

    void help(CommandSender sender) {
        sender.sendMessage("Usage");
        for (Cmd cmd : Cmd.values()) {
            help(sender, cmd);
        }
    }

    void help(CommandSender sender, Cmd cmd) {
        switch (cmd) {
        case TYPE:
            sender.sendMessage("/raid type "
                               + Stream.of(Wave.Type.values()).map(Enum::name)
                               .collect(Collectors.joining("|"))
                               + " - Set wave type.");
            break;
        case SET:
            sender.sendMessage("/raid set <key> [value] - Settings");
            break;
        case WAVE:
            sender.sendMessage("/raid wave <index> - Select wave.");
            sender.sendMessage("/raid wave add [index] - Add wave.");
            sender.sendMessage("/raid wave remove - Remove current wave.");
            sender.sendMessage("/raid wave list - List waves.");
            break;
        case MOB:
            sender.sendMessage("/raid mob add <type> [amount] - Add mob.");
            sender.sendMessage("/raid mob remove [index] - Remove mob.");
            sender.sendMessage("/raid mob list - List mobs.");
            break;
        case BOSS:
            sender.sendMessage("/raid boss - Clear wave boss.");
            sender.sendMessage("/raid boss <type> - Set wave boss.");
        default:
            sender.sendMessage("/raid " + cmd.key);
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
            return raid.waves.get(instance.editWave);
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
        player.sendMessage("Raid `" + worldName + "' created.");
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
        plugin.saveRaid(raid);
        player.sendMessage("Wave #" + raid.waves.indexOf(wave) + " type=" + type);
        return true;
    }

    boolean placeCommand(@NonNull Player player, String[] args) throws Wrong {
        if (args.length != 0) return false;
        Place place = Place.of(player.getLocation());
        Raid raid = requireRaid(player);
        Wave wave = requireWave(player);
        wave.place = place;
        plugin.saveRaid(raid);
        player.sendMessage("Wave #" + raid.waves.indexOf(wave) + " place=" + ShortInfo.of(place));
        return true;
    }

    boolean waveCommand(Player player, String[] args) throws Wrong {
        if (args.length == 0) return false;
        Raid raid = requireRaid(player);
        Instance inst = plugin.raidInstance(raid);
        // Select Wave
        if (args.length == 1) {
            try {
                int waveIndex = Integer.parseInt(args[0]);
                Wave wave = requireWave(player, waveIndex);
                inst.editWave = waveIndex;
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
            inst.editWave = index;
            raid.waves.add(index, wave);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Wave #" + index + " created.");
            return true;
        } case REMOVE: {
            Wave wave = requireWave(player);
            raid.waves.remove(wave);
            plugin.saveRaid(raid);
            player.sendMessage(y + "Wave #" + inst.editWave + " removed: "
                               + ShortInfo.of(wave));
            return true;
        } case LIST: {
            player.sendMessage("" + y + raid.waves.size() + " waves:");
            for (int i = 0; i < raid.waves.size(); i += 1) {
                Wave wave = raid.waves.get(i);
                player.sendMessage("" + i + ") " + y + ShortInfo.of(wave));
            }
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
            int index = wave.spawns.size();
            wave.spawns.add(spawn);
            plugin.saveRaid(raid);
            player.sendMessage("Mob #" + index + " added: "
                               + ShortInfo.of(spawn));
            return true;
        }
        case REMOVE: {
            if (args.length > 2) return false;
            int index = wave.spawns.size() - 1;
            if (args.length >= 2) {
                try {
                    index = Integer.parseInt(args[1]);
                } catch (NumberFormatException iae) {
                    index = -1;
                }
            }
            if (index < 0 || index >= wave.spawns.size()) {
                throw new Wrong("Invalid index: " + args[1]);
            }
            Spawn spawn = wave.spawns.remove(index);
            plugin.saveRaid(raid);
            player.sendMessage("Mob #" + index + " removed: " + ShortInfo.of(spawn));
            return true;
        }
        case LIST: {
            player.sendMessage("Wave #" + inst.editWave + ": " + wave.spawns.size() + " mobs:");
            for (int i = 0; i < wave.spawns.size(); i += 1) {
                player.sendMessage(i + ") " + y + ShortInfo.of(wave.spawns.get(i)));
            }
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
            player.sendMessage("Wave boss=" + boss.getShortInfo());
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
            player.sendMessage("Set " + y + "radius=" + wave.radius);
            return true;
        default: throw new Wrong("Unknown key: " + key);
        }
    }

    boolean saveCommand(@NonNull Player player, String[] args) throws Wrong {
        if (args.length != 0) return false;
        Raid raid = requireRaid(player);
        boolean res = plugin.saveRaid(raid);
        player.sendMessage("Saving raid: " + raid.worldName + ": " + res);
        return true;
    }

    boolean reloadCommand(@NonNull CommandSender sender, String[] args) throws Wrong {
        if (args.length != 0) return false;
        plugin.raids.clear();
        plugin.instances.clear();
        plugin.loadRaids();
        for (World world : plugin.getServer().getWorlds()) {
            plugin.raidInstance(world);
        }
        sender.sendMessage(y + "Raids reloaded.");
        return true;
    }

    boolean debugCommand(@NonNull Player player, String[] args) throws Wrong {
        Raid raid = requireRaid(player);
        Instance inst = plugin.raidInstance(raid);
        int newWave = Integer.parseInt(args[0]);
        inst.clearWave();
        inst.waveIndex = newWave;
        inst.waveComplete = false;
        inst.waveTicks = 0;
        player.sendMessage("Wave " + newWave + " activated.");
        return true;
    }
}

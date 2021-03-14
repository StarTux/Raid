package com.cavetale.raid;

import com.cavetale.raid.util.Text;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class RaidCommand implements CommandExecutor {
    final RaidPlugin plugin;

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player expected");
            return true;
        }
        Player player = (Player) sender;
        if (alias.equalsIgnoreCase("back")) {
            return onBack(player);
        }
        if (args.length > 1) return false;
        String raidName = args.length >= 1
            ? args[0]
            : plugin.getConfig().getString("MainRaid");
        if (raidName == null) {
            player.sendMessage(ChatColor.RED + "Raid not found");
            return true;
        }
        Raid raid = plugin.raids.get(raidName);
        if (raid == null) {
            player.sendMessage(ChatColor.RED + "Raid not found");
            return true;
        }
        Instance instance = plugin.raidInstance(raid);
        if (instance == null) {
            player.sendMessage(ChatColor.RED + "Raid not found");
            return true;
        }
        if (instance.getPhase() == Phase.PRE_WORLD) {
            player.sendMessage(ChatColor.RED + "Raid not ready!");
            return true;
        }
        Wave wave = instance.getCurrentWave();
        if (wave == null || wave.type == Wave.Type.WIN) {
            player.sendMessage(ChatColor.RED + "Raid already over!");
            return true;
        }
        player.sendMessage(ChatColor.GREEN + "Joining " + Text.colorize(raid.displayName) + "...");
        instance.joinPlayer(player);
        return true;
    }

    boolean onBack(Player player) {
        Instance instance = plugin.raidInstance(player.getWorld());
        if (instance == null) {
            player.sendMessage(ChatColor.RED + "You're not in a raid!");
            return true;
        }
        Wave wave = instance.getCurrentWave();
        if (wave == null || wave.type == Wave.Type.WIN) {
            player.sendMessage(ChatColor.RED + "Raid already over!");
            return true;
        }
        player.sendMessage(ChatColor.GREEN + "Returning...");
        player.teleport(wave.place.toLocation(instance.getWorld()));
        return true;
    }
}

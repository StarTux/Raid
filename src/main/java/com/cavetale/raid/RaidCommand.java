package com.cavetale.raid;

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
        if (args.length != 0) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player expected");
            return true;
        }
        Player player = (Player) sender;
        String mainRaid = plugin.getConfig().getString("MainRaid");
        if (mainRaid == null) {
            player.sendMessage(ChatColor.RED + "Raid not found");
            return true;
        }
        Raid raid = plugin.raids.get(mainRaid);
        if (raid == null) {
            player.sendMessage(ChatColor.RED + "Raid not found: " + mainRaid);
            return true;
        }
        Instance instance = plugin.raidInstance(raid);
        if (instance == null) {
            player.sendMessage(ChatColor.RED + "Instance not found");
            return true;
        }
        if (player.getWorld().equals(instance.world)) {
            player.sendMessage(ChatColor.RED + "Already joined");
            return true;
        }
        Wave wave = instance.getCurrentWave();
        if (wave == null || wave.type == Wave.Type.WIN) {
            player.sendMessage(ChatColor.RED + "Bad wave");
            return true;
        }
        player.sendMessage(ChatColor.GREEN + "Joining " + raid.displayName + "...");
        player.teleport(wave.place.toLocation(instance.world));
        return true;
    }
}

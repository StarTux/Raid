package com.cavetale.raid;

import com.cavetale.raid.util.Text;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        if (args.length != 1) return false;
        String raidName = args[0];
        if (raidName == null) {
            player.sendMessage(Component.text("Raid not found", NamedTextColor.RED));
            return true;
        }
        Raid raid = plugin.raids.get(raidName);
        if (raid == null) {
            player.sendMessage(Component.text("Raid not found", NamedTextColor.RED));
            return true;
        }
        Instance instance = plugin.raidInstance(raid);
        if (instance == null) {
            player.sendMessage(Component.text("Raid not found", NamedTextColor.RED));
            return true;
        }
        if (instance.getPhase() == Phase.PRE_WORLD) {
            player.sendMessage(Component.text("Raid not ready!", NamedTextColor.RED));
            return true;
        }
        Wave wave = instance.getPreviousGoalWave();
        if (wave == null || wave.type == Wave.Type.WIN) {
            player.sendMessage(Component.text("Raid already over!", NamedTextColor.RED));
            return true;
        }
        player.sendMessage(Component.text("Joining " + Text.colorize(raid.displayName) + "...", NamedTextColor.GREEN));
        instance.joinPlayer(player);
        return true;
    }

    boolean onBack(Player player) {
        Instance instance = plugin.raidInstance(player.getWorld());
        if (instance == null) {
            player.sendMessage(Component.text("You're not in a raid!", NamedTextColor.RED));
            return true;
        }
        Wave wave = instance.getPreviousGoalWave();
        if (wave == null || wave.type == Wave.Type.WIN) {
            player.sendMessage(Component.text("Raid already over!", NamedTextColor.RED));
            return true;
        }
        player.sendMessage(Component.text("Returning...", NamedTextColor.GREEN));
        player.teleport(wave.place.toLocation(instance.getWorld()));
        return true;
    }
}

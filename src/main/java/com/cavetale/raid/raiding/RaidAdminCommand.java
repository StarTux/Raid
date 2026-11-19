package com.cavetale.raid.raiding;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.raid.RaidPlugin;
import com.cavetale.raid.config.RaidConfig;
import com.cavetale.raid.config.RaidConfigs;
import com.cavetale.raid.raiding.wave.Wave;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class RaidAdminCommand extends AbstractCommand<RaidPlugin> {

    public RaidAdminCommand(final RaidPlugin plugin) {
        super(plugin, "raidadmin");
    }

    @Override
    public void onEnable() {
        rootNode.addChild("skip")
            .description("Skip the current main wave for debugging")
            .playerCaller(this::skip);
        rootNode.addChild("start").arguments("<raid>")
            .description("Start a raid")
            .completers(CommandArgCompleter.supplyList(() -> RaidConfigs.getInstance().getAllRaidNames()))
            .senderCaller(this::start);
        rootNode.addChild("addplayer").arguments("<player> <raid>")
            .description("Add an online player to an active raid")
            .completers(
                CommandArgCompleter.supplyStream(() -> Bukkit.getOnlinePlayers().stream().map(Player::getName)),
                CommandArgCompleter.supplyList(() -> Raiding.getInstance().getRaids().getActiveRaidNames())
            )
            .senderCaller(this::addPlayer);
    }

    private void skip(final Player player) {
        final Raid raid = Raider.of(player).getCurrentRaid();
        if (raid == null || !raid.isEnabled()) throw new CommandWarn("You are not in an active raid");
        for (final WaveProcessor processor : raid.getWaveProcessors()) {
            if (!processor.isMainWave()) continue;
            final Wave wave = processor.getActiveWave();
            if (wave == null || wave.isFinished()) break;
            wave.setFinished(true);
            player.sendMessage(text("Skipping main wave " + processor.getWaveIndex() + " (" + wave.getType() + ")", YELLOW));
            return;
        }
        throw new CommandWarn("No active main wave to skip");
    }

    private boolean start(final CommandSender sender, final String[] args) {
        if (args.length != 1) return false;
        final String raidName = args[0];
        final RaidConfig raidConfig = RaidConfig.named(raidName);
        if (raidConfig == null) {
            throw new CommandWarn("Raid not found: " + raidName);
        }
        final Raid raid = Raiding.getInstance().getRaids().startRaid(raidConfig);
        if (raid == null) {
            throw new CommandWarn("Raid could not be started. See console.");
        }
        sender.sendMessage(
            textOfChildren(
                text("Raid started: "),
                text(raid.getName(), WHITE),
                text(", \""),
                raid.getConfig().getDisplayNameComponent(),
                text("\"")
            ).color(YELLOW)
        );
        return true;
    }

    private boolean addPlayer(final CommandSender sender, final String[] args) {
        if (args.length != 2) return false;
        final Player player = CommandArgCompleter.requirePlayer(args[0]);
        final Raid raid = Raid.named(args[1]);
        if (raid == null) {
            throw new CommandWarn("Raid not found: " + args[1]);
        }
        if (!raid.isEnabled() || raid.isDisabled()) {
            throw new CommandWarn("Raid is not active: " + args[1]);
        }
        raid.addRaider(player);
        sender.sendMessage(
            textOfChildren(
                text("Player added to raid: "),
                text(player.getName(), WHITE),
                text(" -> "),
                text(raid.getName(), WHITE)
            ).color(YELLOW)
        );
        return true;
    }
}

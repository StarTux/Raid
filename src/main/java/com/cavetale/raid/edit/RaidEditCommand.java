package com.cavetale.raid.edit;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.raid.RaidPlugin;
import com.cavetale.raid.config.RaidConfig;
import com.cavetale.raid.config.RaidConfigs;
import java.util.ArrayList;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class RaidEditCommand extends AbstractCommand<RaidPlugin> {

    public RaidEditCommand(final RaidPlugin plugin) {
        super(plugin, "raidedit");
    }

    @Override
    public void onEnable() {
        rootNode.addChild("create").arguments("<name>")
            .description("Create a new raid")
            .completers(CommandArgCompleter.supplyList(() -> RaidConfigs.getInstance().getAllRaidNames()))
            .playerCaller(this::create);
    }

    private boolean create(final Player player, final String[] args) {
        if (args.length != 1) return false;
        final String raidName = args[0];
        if (!raidName.matches("[a-z0-9_]+")) {
            throw new CommandWarn("Illegal raid name: '" + raidName + "', must be lower cased, alphanumeric, underscores allowed!");
        }
        if (RaidConfig.named(raidName) != null) {
            throw new CommandWarn("Raid already exists: " + raidName);
        }
        final RaidConfig raidConfig = new RaidConfig();
        final String mainWorld = player.getWorld().getName();
        raidConfig.getBuildWorlds().put(RaidConfig.MAIN_WORLD, mainWorld);
        raidConfig.getWaves().put(RaidConfig.MAIN_WAVE, new ArrayList<>());
        RaidConfigs.getInstance().addRaidConfig(raidName, raidConfig);
        player.sendMessage(
            textOfChildren(
                text("Raid created: "),
                text(raidName, WHITE),
                text(". Main world: "),
                text(raidConfig.getBuildWorlds().get(RaidConfig.MAIN_WORLD), WHITE)
            ).color(GREEN)
        );
        return true;
    }
}

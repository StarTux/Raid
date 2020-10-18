package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

public final class DialogueAbility extends AbstractAbility {
    private int dialogueIndex = 0;
    private List<String> dialogues = new ArrayList<>(4);

    public DialogueAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
    }

    public void addDialogue(String line) {
        dialogues.add(line);
    }

    @Override
    public void onBegin() { }

    @Override
    public void onEnd() { }

    @Override
    public boolean onTick(int ticks) {
        if (ticks != warmup) return true;
        if (dialogues.isEmpty()) return true;
        String line = dialogues.get(dialogueIndex);
        String message = "" + ChatColor.WHITE + ": " + ChatColor.YELLOW + ChatColor.ITALIC + line;
        dialogueIndex += 1;
        if (dialogueIndex >= dialogues.size()) dialogueIndex = 0;
        for (Player player : context.getPlayers()) {
            player.sendMessage(enemy.getDisplayName() + message);
            player.sendActionBar(enemy.getDisplayName() + message);
        }
        return true;
    }
}

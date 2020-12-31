package com.cavetale.raid;

import org.bukkit.entity.Player;
import com.cavetale.inventory.InventoryPlugin;

public final class InventoryHook {
    private InventoryHook() { }

    public static void restore(Player player) {
        InventoryPlugin.getInstance().restoreInventory(player);
    }
}

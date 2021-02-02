package com.cavetale.raid;

import org.bukkit.entity.Player;
import com.cavetale.inventory.InventoryPlugin;

public final class InventoryHook {
    private InventoryHook() { }

    public static void store(Player player) {
        InventoryPlugin.getInstance().storeInventory(player);
    }

    public static void restore(Player player, Runnable callback) {
        InventoryPlugin.getInstance().restoreInventory(player, callback);
    }
}
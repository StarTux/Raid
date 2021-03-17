package com.cavetale.raid;

import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public final class LootDrop {
    protected final ItemStack itemStack;
    protected final int bonusAmount;
    protected final int weight;
}

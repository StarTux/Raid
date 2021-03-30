package com.cavetale.raid;

import com.cavetale.mytems.Mytems;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.util.Vector;

@RequiredArgsConstructor
public final class CustomBlock {
    protected final Block block;
    protected final Mytems mytems;
    private ItemFrame itemFrame;

    void place() {
        Location location = block.getLocation()
            .setDirection(new Vector(0, 1, 0));
        itemFrame = block.getWorld().spawn(location, ItemFrame.class, e -> {
                e.setPersistent(false);
                e.setFixed(true);
                e.setItem(mytems.getMytem().getItem());
                e.setItemDropChance(0.0f);
                e.setVisible(false);
                e.setFacingDirection(BlockFace.UP);
            });
        block.setType(Material.BARRIER);
    }

    void remove() {
        itemFrame.remove();
        itemFrame = null;
        block.setType(Material.AIR);
    }
}

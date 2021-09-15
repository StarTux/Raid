package com.cavetale.raid;

import com.destroystokyo.paper.block.BlockSoundGroup;
import java.util.function.Consumer;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

@Data
public final class Roadblock implements ShortInfo {
    private int x;
    private int y;
    private int z;
    private String blocked;
    private String unblocked;

    public Roadblock() { }

    public Roadblock(final int x, final int y, final int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Roadblock(final Block block, final BlockData blocked, final BlockData unblocked) {
        this(block.getX(), block.getY(), block.getZ());
        this.blocked = blocked.getAsString(true);
        this.unblocked = unblocked.getAsString(true);
    }

    static BlockData parse(String in) {
        if (in == null) return Material.AIR.createBlockData();
        try {
            return Bukkit.createBlockData(in);
        } catch (IllegalArgumentException iae) { }
        if (in.contains("[")) {
            in = in.split("[", 2)[0];
            try {
                return Bukkit.createBlockData(in);
            } catch (IllegalArgumentException iae) { }
        }
        return Material.AIR.createBlockData();
    }

    public void block(World world) {
        world.getChunkAtAsync(x >> 4, z >> 4, (Consumer<Chunk>) unusedChunk -> {
                world.getBlockAt(x, y, z).setBlockData(parse(blocked));
            });
    }

    public void unblock(World world, boolean effect) {
        world.getChunkAtAsync(x >> 4, z >> 4, (Consumer<Chunk>) unusedChunk -> {
                Block block = world.getBlockAt(x, y, z);
                BlockData blockData = parse(unblocked);
                if (blockData.getMaterial().isEmpty()) {
                    // Break
                    if (effect) {
                        BlockSoundGroup group = block.getSoundGroup();
                        world.playSound(block.getLocation(), group.getBreakSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
                        world.spawnParticle(Particle.BLOCK_DUST, block.getLocation(), 8, 0.0, 0.0, 0.0, 0.0, block.getBlockData());
                    }
                    block.setBlockData(blockData);
                } else {
                    // Place
                    block.setBlockData(blockData);
                    if (effect) {
                        BlockSoundGroup group = block.getSoundGroup();
                        world.playSound(block.getLocation(), group.getPlaceSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
                    }
                }
            });
    }

    public void unblock(World world) {
        unblock(world, false);
    }

    @Override
    public String getShortInfo() {
        return x + "," + y + "," + z + ":" + blocked + "/" + unblocked;
    }

    public boolean isInSamePlace(Roadblock other) {
        return x == other.x && y == other.y && z == other.z;
    }
}

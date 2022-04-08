package com.cavetale.raid.struct;

import com.cavetale.core.editor.EditMenuAdapter;
import com.cavetale.core.editor.EditMenuButton;
import com.cavetale.core.editor.EditMenuNode;
import com.cavetale.raid.util.WorldEdit;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Data
public final class Cuboid implements EditMenuAdapter {
    public static final Cuboid ZERO = new Cuboid(Vec3i.ZERO, Vec3i.ZERO);
    private Vec3i min;
    private Vec3i max;

    public Cuboid() {
        this.min = Vec3i.ZERO;
        this.max = Vec3i.ZERO;
    }

    public Cuboid(final Vec3i min, final Vec3i max) {
        this.min = min;
        this.max = max;
    }

    public boolean contains(int x, int y, int z) {
        return x >= min.x && x <= max.x
            && y >= min.y && y <= max.y
            && z >= min.z && z <= max.z;
    }

    public boolean contains(Block block) {
        return contains(block.getX(), block.getY(), block.getZ());
    }

    public boolean contains(Location loc) {
        return contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public boolean contains(Vec3i v) {
        return contains(v.x, v.y, v.z);
    }

    @Override
    public String toString() {
        return "(" + min + ")-(" + max + ")";
    }

    public int getSizeX() {
        return max.x - min.x + 1;
    }

    public int getSizeY() {
        return max.y - min.y + 1;
    }

    public int getSizeZ() {
        return max.z - min.z + 1;
    }

    public int getVolume() {
        return getSizeX() * getSizeY() * getSizeZ();
    }

    public Vec3i getCenter() {
        return new Vec3i((min.x + max.x) / 2, (min.y + max.y) / 2, (min.z + max.z) / 2);
    }

    public List<Vec3i> enumerate() {
        List<Vec3i> result = new ArrayList<>();
        for (int y = min.y; y <= max.y; y += 1) {
            for (int z = min.z; z <= max.z; z += 1) {
                for (int x = min.x; x <= max.x; x += 1) {
                    result.add(new Vec3i(x, y, z));
                }
            }
        }
        return result;
    }

    public Vec3i clamp(Vec3i other) {
        int x = Math.max(other.x, min.x);
        int y = Math.max(other.y, min.y);
        int z = Math.max(other.z, min.z);
        x = Math.min(x, max.x);
        y = Math.min(y, max.y);
        z = Math.min(z, max.z);
        return new Vec3i(x, y, z);
    }

    public boolean isZero() {
        return equals(ZERO);
    }

    @Override
    public List<EditMenuButton> getEditMenuButtons(EditMenuNode node) {
        return List.of(new EditMenuButton[] {
                new EditMenuButton() {
                    @Override
                    public ItemStack getMenuIcon() {
                        return new ItemStack(Material.WOODEN_AXE);
                    }

                    @Override
                    public List<Component> getTooltip() {
                        return List.of(text("Set to selection", GREEN));
                    }

                    @Override
                    public void onClick(Player player, ClickType clickType) {
                        if (clickType.isLeftClick()) {
                            Cuboid selection = WorldEdit.getSelection(player);
                            if (selection == null) {
                                player.sendMessage(text("No selection!", RED));
                                return;
                            }
                            min = selection.min;
                            max = selection.max;
                            player.sendMessage(text("Set to selection: " + Cuboid.this.toString(), GREEN));
                        }
                    }
                },
            });
    }

    @Override
    public ItemStack getMenuIcon(EditMenuNode node) {
        return new ItemStack(Material.STRUCTURE_BLOCK);
    }

    @Override
    public List<Component> getTooltip(EditMenuNode node) {
        Component sep = text(": ", DARK_GRAY);
        return List.of(text("Cuboid", LIGHT_PURPLE),
                       text(toString(), GRAY));
    }
}

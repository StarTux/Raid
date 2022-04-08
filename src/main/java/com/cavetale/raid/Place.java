package com.cavetale.raid;

import com.cavetale.core.editor.EditMenuAdapter;
import com.cavetale.core.editor.EditMenuButton;
import com.cavetale.core.editor.EditMenuNode;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@AllArgsConstructor
@NoArgsConstructor
public final class Place implements ShortInfo, EditMenuAdapter {
    double x;
    double y;
    double z;
    float yaw;
    float pitch;

    public Location toLocation(@NonNull World w) {
        return new Location(w, x, y, z, yaw, pitch);
    }

    public static Place of(@NonNull Location loc) {
        return new Place(loc.getX(), loc.getY(), loc.getZ(),
                         loc.getYaw(), loc.getPitch());
    }

    public void load(@NonNull Location loc) {
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
    }

    @Override
    public String getShortInfo() {
        return "(" + (int) Math.floor(x)
            + " " + (int) Math.floor(y)
            + " " + (int) Math.floor(z) + ")";
    }

    public Vec2i getChunk() {
        return new Vec2i(((int) Math.floor(x)) >> 4,
                         ((int) Math.floor(z)) >> 4);
    }

    @Override
    public List<EditMenuButton> getEditMenuButtons(EditMenuNode node) {
        return List.of(new EditMenuButton[] {
                new EditMenuButton() {
                    @Override
                    public ItemStack getMenuIcon() {
                        return new ItemStack(Material.END_CRYSTAL);
                    }

                    @Override
                    public List<Component> getTooltip() {
                        return List.of(text("Set current location", GREEN));
                    }

                    @Override
                    public void onClick(Player player, ClickType clickType) {
                        if (clickType.isLeftClick()) {
                            load(player.getLocation());
                            player.sendMessage(text("Set to current location: " + getShortInfo(), GREEN));
                        }
                    }
                },
                new EditMenuButton() {
                    @Override
                    public ItemStack getMenuIcon() {
                        return new ItemStack(Material.ENDER_PEARL);
                    }

                    @Override
                    public List<Component> getTooltip() {
                        return List.of(text("Teleport", GREEN),
                                       text(getShortInfo(), GRAY));
                    }

                    @Override
                    public void onClick(Player player, ClickType clickType) {
                        if (clickType.isLeftClick()) {
                            player.teleport(toLocation(player.getWorld()));
                            player.sendMessage(text("Teleported to: " + getShortInfo(), GREEN));
                        }
                    }
                },
            });
    }

    @Override
    public ItemStack getMenuIcon(EditMenuNode node) {
        return new ItemStack(Material.ENDER_PEARL);
    }

    @Override
    public List<Component> getTooltip(EditMenuNode node) {
        Component sep = text(": ", DARK_GRAY);
        return List.of(text("Place", LIGHT_PURPLE),
                       join(separator(sep), text("x", GRAY), text(String.format("%.2f", x), WHITE)),
                       join(separator(sep), text("y", GRAY), text(String.format("%.2f", y), WHITE)),
                       join(separator(sep), text("z", GRAY), text(String.format("%.2f", z), WHITE)),
                       join(separator(sep), text("yaw", GRAY), text(String.format("%.2f", yaw), WHITE)),
                       join(separator(sep), text("pitch", GRAY), text(String.format("%.2f", pitch), WHITE)));
    }
}

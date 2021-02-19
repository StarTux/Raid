package com.cavetale.raid;

import com.cavetale.worldmarker.entity.EntityMarker;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

public final class EscortMarker {
    public static final String WORLD_MARKER_ID = "raid:escort";
    private final RaidPlugin plugin;
    @Getter private final Villager entity;
    @Getter @Setter private List<String> dialogue;
    @Getter @Setter private int dialogueIndex;
    @Getter @Setter private int dialogueCooldown;
    private ArmorStand textArmorStand = null;
    private Location pathLocation;
    @Getter private boolean pathing;
    private final double up = 0.25;

    public EscortMarker(final RaidPlugin plugin, final Villager entity) {
        this.plugin = plugin;
        this.entity = entity;
    }

    public EscortMarker enable() {
        EntityMarker.setId(entity, WORLD_MARKER_ID);
        RaidPlugin.getInstance().getIdEscortMap().put(entity.getEntityId(), new Handle());
        return this;
    }

    /**
     * Reference to this.
     */
    public final class Handle {
        public EscortMarker getEscortMarker() {
            return EscortMarker.this;
        }
    }

    public static EscortMarker of(Entity e) {
        Handle handle = RaidPlugin.getInstance().getIdEscortMap().get(e.getEntityId());
        if (handle == null) return null;
        return handle.getEscortMarker();
    }

    public boolean isValid() {
        return entity.isValid();
    }

    public void remove() {
        entity.remove();
        if (textArmorStand != null) {
            textArmorStand.remove();
            textArmorStand = null;
        }
    }

    public void tick(List<Player> players) {
        if (textArmorStand != null) {
            if (!textArmorStand.isValid()) {
                textArmorStand = null;
            } else if (textArmorStand.getTicksLived() > 100) {
                textArmorStand.remove();
                textArmorStand = null;
            } else {
                textArmorStand.teleport(entity.getEyeLocation().add(0, up, 0));
            }
        }
        if (dialogue != null && dialogueIndex < dialogue.size()) {
            if (dialogueCooldown > 0) {
                dialogueCooldown -= 1;
            } else {
                String line = dialogue.get(dialogueIndex++);
                dialogueCooldown = 100;
                sayLine(players, line);
            }
        }
        if (pathLocation != null && entity.getPathfinder().getCurrentPath() == null) {
            double dist = entity.getLocation().distanceSquared(pathLocation);
            if (dist < 1) {
                pathLocation = null;
            } else {
                refreshPath();
            }
        }
    }

    public void sayLine(List<Player> players, String line) {
        final String txt = ChatColor.translateAlternateColorCodes('&', line);
        for (Player player : players) {
            player.sendActionBar(txt);
        }
        if (textArmorStand != null) {
            textArmorStand.remove();
            textArmorStand = null;
        }
        dialogueCooldown = 100;
        textArmorStand = entity.getWorld().spawn(entity.getEyeLocation().add(0, up, 0), ArmorStand.class, a -> {
                a.setPersistent(false);
                a.setCustomName(txt);
                a.setCustomNameVisible(true);
                a.setInvisible(true);
                a.setMarker(true);
                a.setCanTick(false);
            });
    }

    public void pathTo(Location location) {
        pathLocation = location;
        pathing = true;
        if (!entity.getPathfinder().moveTo(location)) {
            entity.teleport(location);
        }
        pathing = false;
    }

    public void refreshPath() {
        if (pathLocation == null) return;
        pathTo(pathLocation);
    }
}

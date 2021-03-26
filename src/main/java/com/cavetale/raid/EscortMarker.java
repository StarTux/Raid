package com.cavetale.raid;

import com.cavetale.worldmarker.entity.EntityMarker;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class EscortMarker {
    public static final String WORLD_MARKER_ID = "raid:escort";
    private final RaidPlugin plugin;
    private final Instance instance;
    @Getter private final Villager entity;
    @Getter private List<String> dialogue;
    @Getter private int dialogueIndex;
    @Getter private int dialogueCooldown;
    private ArmorStand textArmorStand = null;
    private Location pathLocation;
    // This variable tells the EventHandler to allow the pathfinding event
    @Getter private boolean pathing;
    private final double up = 0.25;
    private double pathDistance = 0;
    //
    private boolean dialogueRequired;
    private boolean pathRequired;
    private int noProgressTicks;

    public EscortMarker enable() {
        EntityMarker.setId(entity, WORLD_MARKER_ID);
        RaidPlugin.getInstance().getIdEscortMap().put(entity.getEntityId(), new Handle());
        return this;
    }

    public void clearWave() {
        pathLocation = null;
        pathing = false;
        pathDistance = 0;
        pathRequired = false;
        dialogue = null;
        dialogueIndex = 0;
        dialogueCooldown = 0;
        dialogueRequired = false;
        noProgressTicks = 0;
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

    protected void tick(List<Player> players) {
        if (textArmorStand != null) {
            if (!textArmorStand.isValid()) {
                textArmorStand = null;
            } else if (textArmorStand.getTicksLived() > 100) {
                textArmorStand.remove();
                textArmorStand = null;
                if (instance.getWave().getType() == Wave.Type.ESCORT) {
                    instance.getBossBar().setTitle("");
                    instance.getBossBar().setProgress(0);
                }
            } else {
                textArmorStand.teleport(entity.getEyeLocation().add(0, up, 0));
            }
        }
        if (dialogueRequired) {
            if (dialogueCooldown > 0) {
                dialogueCooldown -= 1;
            } else {
                if (dialogueIndex >= dialogue.size()) {
                    dialogueRequired = false;
                } else {
                    String line = dialogue.get(dialogueIndex++);
                    int words = line.split(" ").length;
                    dialogueCooldown = 20 + words * 20;
                    sayLine(players, line);
                }
            }
        }
        if (pathRequired) {
            double newPathDistance = entity.getLocation().distanceSquared(pathLocation);
            noProgressTicks = newPathDistance < pathDistance ? 0 : noProgressTicks + 1;
            pathDistance = newPathDistance;
            if (pathDistance < 1.0) {
                pathRequired = false;
            } else if (noProgressTicks > 200) {
                instance.warn("NPC: No progress. Teleporting!");
                entity.teleport(pathLocation);
                pathDistance = 0;
                pathRequired = false;
            } else if (entity.getPathfinder().getCurrentPath() == null) {
                refreshPath();
            }
        }
    }

    private void sayLine(List<Player> players, String line) {
        final String txt = ChatColor.translateAlternateColorCodes('&', line);
        if (textArmorStand != null) {
            textArmorStand.remove();
            textArmorStand = null;
        }
        textArmorStand = entity.getWorld().spawn(entity.getEyeLocation().add(0, up, 0), ArmorStand.class, a -> {
                a.setPersistent(false);
                a.setCustomName(txt);
                a.setCustomNameVisible(true);
                a.setInvisible(true);
                a.setMarker(true);
                a.setCanTick(false);
            });
        if (instance.getWave().getType() == Wave.Type.ESCORT) {
            instance.getBossBar().setTitle(txt);
            instance.getBossBar().setProgress(1);
        }
    }

    /**
     * Instance calls this escort to path to location.
     */
    public void pathTo(@NonNull Location location) {
        pathLocation = location;
        pathRequired = true;
        pathing = true;
        if (!entity.getPathfinder().moveTo(location)) {
            entity.teleport(location);
        }
        pathing = false;
    }

    /**
     * Instance calls this escort to say these lines.
     */
    public void sayLines(@NonNull List<String> lines) {
        dialogue = lines;
        dialogueRequired = true;
        dialogueCooldown = 20;
    }

    public void refreshPath() {
        if (pathLocation == null) return;
        pathTo(pathLocation);
    }

    public boolean isDone() {
        return !pathRequired && !dialogueRequired;
    }

    public String debugString() {
        return "path=" + pathRequired
            + " dist=" + pathDistance
            + " dialogue=" + dialogueRequired + (dialogue != null ? (": " + dialogueIndex + "/" + dialogue.size()) : "");
    }
}

package com.cavetale.raid;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * JSONable structure.
 * Created and managed by RaidPlugin.
 */
@Getter
public final class Raid {
    transient String worldName;
    List<Wave> waves = new ArrayList<>();
    String displayName = "";
    List<String> winCommands = new ArrayList<>();
    List<String> joinCommands = new ArrayList<>();

    public Raid() { }

    public Raid(@NonNull final String worldName) {
        this.worldName = worldName;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public void onSave() {
        for (Wave wave : waves) {
            wave.onSave();
        }
    }
}

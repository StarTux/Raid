package com.cavetale.raid;

import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.World;

final class Raid {
    transient String worldName;
    List<Wave> waves = new ArrayList<>();

    Raid() { }

    Raid(@NonNull final String worldName) {
        this.worldName = worldName;
    }

    World getWorld() {
        return Bukkit.getWorld(worldName);
    }
}

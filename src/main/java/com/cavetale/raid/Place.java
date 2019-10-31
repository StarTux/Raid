package com.cavetale.raid;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.World;

@AllArgsConstructor
@NoArgsConstructor
final class Place implements ShortInfo {
    double x;
    double y;
    double z;
    float yaw;
    float pitch;

    Location toLocation(@NonNull World w) {
        return new Location(w, x, y, z, yaw, pitch);
    }

    static Place of(@NonNull Location loc) {
        return new Place(loc.getX(), loc.getY(), loc.getZ(),
                         loc.getYaw(), loc.getPitch());
    }

    @Override
    public String getShortInfo() {
        return "(" + (int) Math.floor(x)
            + " " + (int) Math.floor(y)
            + " " + (int) Math.floor(z) + ")";
    }

    Vec2i getChunk() {
        return new Vec2i(((int) Math.floor(x)) >> 4,
                         ((int) Math.floor(z)) >> 4);
    }
}

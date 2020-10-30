package com.cavetale.raid.enemy;

import java.util.Objects;
import org.bukkit.Location;

/**
 * Location utility methods.
 */
public final class Loc {
    private Loc() { }

    public static double distance(Location a, Location b) {
        if (a == null || b == null) return Double.MAX_VALUE;
        if (!Objects.equals(a.getWorld(), b.getWorld())) return Double.MAX_VALUE;
        return a.distance(b);
    }

    public static double distanceSquared(Location a, Location b) {
        if (a == null || b == null) return Double.MAX_VALUE;
        if (!Objects.equals(a.getWorld(), b.getWorld())) return Double.MAX_VALUE;
        return a.distanceSquared(b);
    }

    public static boolean isNearby(Location a, Location b, double distance) {
        if (a == null || b == null) return false;
        if (!Objects.equals(a.getWorld(), b.getWorld())) return false;
        return a.distanceSquared(b) < distance * distance;
    }

    public static
        String toString(Location loc) {
        return String.format("%s:%.2f,%.2f,%.2f", loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }
}

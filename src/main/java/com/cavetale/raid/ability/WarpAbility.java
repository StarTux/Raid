package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

public final class WarpAbility extends AbstractAbility {
    @Getter @Setter private double maxDistance = 32.0;
    private boolean done = false;
    private final Random random = new Random();

    public WarpAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 100;
    }

    @Override
    public void onBegin() {
        done = false;
    }

    @Override
    public void onEnd() { }

    private double rnd() {
        return random.nextBoolean()
            ? random.nextDouble()
            : -random.nextDouble();
    }

    @Override
    public boolean onTick(int ticks) {
        if (done) return true;
        Location center = context.getSpawnLocation();
        Location loc = enemy.getLocation();
        if (center.distanceSquared(loc) > maxDistance * maxDistance) {
            done = true;
            enemy.teleport(center);
        }
        Location to = loc.clone().add(rnd() * 8.0, rnd() * 4.0, rnd() * 8.0);
        if (to.distanceSquared(center) > maxDistance * maxDistance) return true;
        if (to.getY() < 5.0 && to.getY() > 250.0) return true;
        if (!to.getBlock().getRelative(0, -1, 0).getType().isSolid()) return true;
        BoundingBox bb = enemy.getBoundingBox().shift(to.clone().subtract(loc));
        World w = to.getWorld();
        final int ax = (int) Math.floor(bb.getMinX());
        final int ay = (int) Math.floor(bb.getMinY());
        final int az = (int) Math.floor(bb.getMinZ());
        final int bx = (int) Math.floor(bb.getMaxX());
        final int by = (int) Math.floor(bb.getMaxY());
        final int bz = (int) Math.floor(bb.getMaxZ());
        for (int y = ay; y <= by; y += 1) {
            for (int z = az; z <= bz; z += 1) {
                for (int x = ax; x <= bx; x += 1) {
                    if (!w.getBlockAt(x, y, z).isEmpty()) return true;
                }
            }
        }
        enemy.teleport(to);
        done = true;
        return true;
    }
}

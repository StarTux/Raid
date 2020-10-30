package com.cavetale.raid.enemy;

import com.cavetale.raid.ability.VampirismAbility;
import org.bukkit.Location;
import org.bukkit.entity.Bat;

public final class VampireBat extends LivingEnemy {
    private VampirismAbility vampirism;
    private Location safeLocation;

    public VampireBat(final Context context) {
        super(context);
    }

    @Override
    public void spawn(Location location) {
        if (!location.isChunkLoaded()) return;
        living = location.getWorld().spawn(location, Bat.class, this::prep);
        markLiving();
        vampirism = new VampirismAbility(this, context);
        vampirism.setDuration(9999);
        vampirism.setWarmup(20);
        vampirism.setActive(100);
        vampirism.setInterval(20);
        vampirism.setDamagePerTick(0.05);
        vampirism.setFoodPerSecond(1);
        vampirism.setSaturationPerTick(0.1f);
        vampirism.begin();
        safeLocation = location;
    }

    @Override
    public void onRemove() {
        vampirism.end();
    }

    @Override
    public void tick() {
        if (!vampirism.tick()) {
            vampirism.end();
            vampirism.begin();
        }
        Bat bat = (Bat) living;
        if (!bat.isAwake()) {
            bat.setAwake(true);
        }
        Location location = living.getLocation();
        Location spawn = getSpawnLocation();
        if (!location.getWorld().equals(spawnLocation.getWorld())) {
            safeLocation = spawnLocation;
            living.teleport(safeLocation);
        } else if (location.distanceSquared(spawnLocation) > 25.0) {
            living.teleport(safeLocation);
        } else {
            safeLocation = location;
        }
    }

    private void prep(Bat bat) {
        bat.setPersistent(false);
        Prep.health(bat, 20);
    }
}

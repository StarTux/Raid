package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Spider;

public final class MountAbility extends AbstractAbility {
    boolean spawned = false;

    public MountAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 100;
    }

    @Override
    public void onBegin() {
        spawned = false;
    }

    @Override
    public void onEnd() { }

    @Override
    public boolean onTick(int ticks) {
        if (spawned) return true;
        LivingEntity living = enemy.getLivingEntity();
        if (living == null) return false;
        if (living.getVehicle() != null) return true;
        double health = 200.0;
        Spider mount = enemy.getWorld().spawn(enemy.getLocation(), Spider.class, e -> {
                e.setPersistent(false);
                e.setRemoveWhenFarAway(true);
                e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
                e.setHealth(health);
            });
        context.registerTemporaryEntity(mount);
        mount.addPassenger(living);
        spawned = true;
        return true;
    }
}

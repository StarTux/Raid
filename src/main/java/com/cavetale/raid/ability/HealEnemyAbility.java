package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.util.Vector;

public final class HealEnemyAbility extends AbstractAbility {
    @Getter @Setter private int interval = 40;
    private int intervalTicks = 0;
    @Getter @Setter private double healAmount = 10;

    public HealEnemyAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 100;
    }

    @Override
    public void onBegin() { }

    @Override
    public void onEnd() { }

    @Override
    public boolean onTick(int ticks) {
        if (intervalTicks > 0) {
            intervalTicks -= 1;
            return true;
        }
        Enemy target = null;
        double max = 0;
        for (Enemy other : context.getEnemies()) {
            if (other.isDead()) continue;
            if (other == enemy) continue;
            double missing = other.getMaxHealth() - other.getHealth();
            if (missing <= 0) continue;
            if (missing > max) {
                target = other;
                max = missing;
            }
        }
        if (target == null) return true;
        intervalTicks = interval;
        target.setHealth(Math.min(target.getMaxHealth(), target.getHealth() + healAmount));
        enemy.getWorld().playSound(enemy.getEyeLocation(), Sound.ENTITY_EVOKER_PREPARE_WOLOLO, SoundCategory.HOSTILE, 1.0f, 1.25f);
        Location from = enemy.getEyeLocation();
        Location to = target.getEyeLocation();
        int steps = 1 + (int) Math.floor(from.distance(to));
        Vector vec = to.clone().subtract(from).toVector();
        for (int i = 0; i < steps; i += 1) {
            Location loc = from.clone().add(vec.clone().multiply(Math.random()));
            loc.getWorld().spawnParticle(Particle.NOTE, loc, 1, 0, 0, 0, 0);
        }
        return true;
    }
}

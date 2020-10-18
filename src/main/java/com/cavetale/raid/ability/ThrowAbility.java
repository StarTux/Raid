package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Find a target which is on ground and throw it.
 */
public final class ThrowAbility extends AbstractAbility {
    @Getter @Setter private int interval = 10;
    private int intervalTicks = 0;
    private final Random random = new Random();

    public ThrowAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 400;
    }

    @Override
    public void onBegin() { }

    @Override
    public void onEnd() { }

    private Player findTarget(List<Player> players) {
        Location eye = enemy.getEyeLocation();
        double minVisible = Double.MAX_VALUE;
        double minBlind = Double.MAX_VALUE;
        Player visible = null;
        Player blind = null;
        final double maxVisible = 32 * 32;
        final double maxBlind = 16 * 16;
        LivingEntity living = enemy.getLivingEntity();
        for (Player player : players) {
            if (!player.isOnGround()) continue;
            double dist = player.getEyeLocation().distanceSquared(eye);
            if (living == null || living.hasLineOfSight(player)) {
                if (dist < minVisible && dist < maxVisible) {
                    visible = player;
                    minVisible = dist;
                }
            } else {
                if (dist < minBlind && dist < maxBlind) {
                    blind = player;
                    minBlind = dist;
                }
            }
        }
        return visible != null ? visible : null;
    }

    private double rnd() {
        return random.nextBoolean()
            ? random.nextDouble()
            : -random.nextDouble();
    }

    @Override
    public boolean onTick(int ticks) {
        if (intervalTicks > 0) {
            intervalTicks -= 1;
            return true;
        }
        intervalTicks = interval;
        //
        Player target = findTarget(context.getPlayers());
        if (target == null) return true;
        Vector vec = new Vector(rnd(), 0, rnd()).multiply(1.5).setY(1.5);
        target.setVelocity(target.getVelocity().add(vec));
        target.playSound(target.getEyeLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 1.0f, 1.0f);
        return true;
    }
}

package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Enemy;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

/**
 * Shoot fireworks at all visible players.
 * Initially wait for the warmup. After each shot, wait for the
 * interval.
 * Immobilizing.
 */
public final class FireworkAbility extends AbstractAbility {
    @Getter @Setter private int interval = 40;
    @Getter @Setter private int fireworks = 2;
    private int intervalTicks = 0;

    public FireworkAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 100;
        warmup = 40;
    }

    @Override
    public void onBegin() {
        enemy.setImmobile(true);
    }

    @Override
    public void onEnd() {
        enemy.setImmobile(false);
        intervalTicks = 0;
    }

    @Override
    public boolean onTick(int ticks) {
        if (intervalTicks > 0) {
            intervalTicks -= 1;
            return true;
        }
        intervalTicks = interval;
        //
        Location eye = enemy.getEyeLocation();
        for (Player player : context.getPlayers()) {
            Location loc = player.getLocation();
            if (loc.distance(eye) > 64) continue;
            if (!enemy.hasLineOfSight(player)) continue;
            Vector vec = loc.subtract(eye).toVector()
                .normalize();
            Location from = eye.clone().add(vec);
            Firework firework = enemy.getWorld().spawn(from, Firework.class, fw -> {
                    fw.setVelocity(vec.multiply(2.0));
                    FireworkMeta meta = fw.getFireworkMeta();
                    for (int i = 0; i < fireworks; i += 1) {
                        FireworkEffect.Builder builder = FireworkEffect.builder();
                        builder.with(FireworkEffect.Type.BALL)
                            .withColor(Color.BLACK).withTrail();
                        builder.with(FireworkEffect.Type.BALL)
                            .withColor(Color.RED).withTrail();
                        builder.with(FireworkEffect.Type.BALL)
                            .withColor(Color.ORANGE).withTrail();
                        builder.with(FireworkEffect.Type.BALL)
                            .withColor(Color.YELLOW).withTrail();
                        meta.addEffect(builder.build());
                    }
                    fw.setFireworkMeta(meta);
                    fw.setPersistent(false);
                });
            context.registerTemporaryEntity(firework);
        }
        return true;
    }
}

package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import com.cavetale.worldmarker.EntityMarker;
import java.util.Random;
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
    @Getter @Setter private int fireworkEffects = 3;
    private int intervalTicks = 0;
    private final Random random = new Random();
    public static final String FIREWORK_ID = "raid:firework";

    public FireworkAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 100;
        warmup = 20;
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
            Location target = player.getEyeLocation();
            if (target.distance(eye) > 64) continue;
            if (!enemy.hasLineOfSight(player)) continue;
            Vector vec = target.subtract(eye).toVector().normalize();
            Location from = eye.clone().add(vec);
            Firework firework = enemy.getWorld().spawn(from, Firework.class, fw -> {
                    fw.setVelocity(vec.multiply(1.0));
                    FireworkMeta meta = fw.getFireworkMeta();
                    for (int i = 0; i < fireworkEffects; i += 1) {
                        FireworkEffect.Builder builder = FireworkEffect.builder();
                        switch (random.nextInt(4)) {
                        case 0:
                            builder.with(FireworkEffect.Type.BALL).withColor(Color.BLACK);
                            break;
                        case 1:
                            builder.with(FireworkEffect.Type.BALL).withColor(Color.RED);
                            break;
                        case 2:
                            builder.with(FireworkEffect.Type.BALL).withColor(Color.ORANGE);
                            break;
                        case 3:
                            builder.with(FireworkEffect.Type.BALL).withColor(Color.YELLOW);
                            break;
                        default: break;
                        }
                        meta.addEffect(builder.build());
                    }
                    fw.setFireworkMeta(meta);
                    fw.setPersistent(false);
                    fw.setShotAtAngle(true);
                });
            EntityMarker.setId(firework, FIREWORK_ID);
            context.registerTemporaryEntity(firework);
        }
        return true;
    }
}

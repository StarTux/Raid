package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Enemy;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class FireballAbility extends AbstractAbility {
    @Getter @Setter private int interval;
    private int intervalTicks = 0;

    public FireballAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 200;
        warmup = 20;
        interval = 20;
    }

    @Override
    public void onBegin() { }

    @Override
    public void onEnd() {
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
            if (!enemy.hasLineOfSight(player)) continue;
            Location loc = player.getEyeLocation();
            Vector vec = loc.subtract(eye).toVector().normalize().multiply(1.5);
            LargeFireball fireball = enemy.launchProjectile(LargeFireball.class, vec);
            fireball.setPersistent(false);
            context.registerTemporaryEntity(fireball);
        }
        return true;
    }
}

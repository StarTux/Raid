package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class PullAbility extends AbstractAbility {
    @Getter @Setter private int interval;
    private int intervalTicks = 0;

    public PullAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 100;
        warmup = 50;
        interval = 100;
    }

    @Override
    public void onBegin() {
        enemy.setImmobile(true);
        duration = 100;
        warmup = 60;
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
            if (loc.distance(eye) < 8) continue;
            Vector vec = eye.subtract(loc).toVector().normalize().multiply(3.0);
            player.setVelocity(vec);
        }
        return true;
    }
}

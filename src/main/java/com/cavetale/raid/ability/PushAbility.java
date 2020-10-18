package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class PushAbility extends AbstractAbility {
    @Getter @Setter private int interval = 40;
    private int intervalTicks = 0;

    public PushAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 60;
        warmup = 20;
        interval = 60;
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
            if (loc.distance(eye) < 8) continue;
            Vector vec = loc.subtract(eye)
                .toVector()
                .setY(0)
                .normalize()
                .multiply(2.0)
                .setY(1.0);
            player.setVelocity(vec);
        }
        return true;
    }
}

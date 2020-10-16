package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Enemy;
import java.util.Random;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class SlipIceAbility extends AbstractAbility {
    private boolean done = false;
    private final Random random = new Random();

    public SlipIceAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 20;
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
        done = true;
        for (Player player : context.getPlayers()) {
            Vector vec = new Vector(rnd(), 0, rnd())
                .normalize().multiply(1.5);
            player.setVelocity(player.getVelocity().add(vec));
        }
        return true;
    }
}

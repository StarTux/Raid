package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.LlamaSpit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class LlamaSpitAbility extends AbstractAbility {
    @Getter @Setter private int interval = 40;
    private int intervalTicks = 0;
    private final Random random = new Random();

    public LlamaSpitAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 200;
    }

    @Override
    public void onBegin() { }

    @Override
    public void onEnd() { }

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
        List<Player> players = context.getPlayers();
        Player target = players.get(random.nextInt(players.size()));
        Vector velo = target.getEyeLocation()
            .subtract(enemy.getEyeLocation())
            .toVector().normalize()
            .add(new Vector(rnd() * 0.1,
                            random.nextDouble() * 0.2,
                            rnd() * 0.1))
            .multiply(2.0);
        enemy.launchProjectile(LlamaSpit.class, velo);
        return true;
    }
}

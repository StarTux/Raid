package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.util.Vector;

public final class TridentAbility extends AbstractAbility {
    @Getter @Setter private int cooldown = 20;
    private int cooldownTicks = 0;
    private Random random = new Random();

    public TridentAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 200;
        warmup = 20;
    }

    @Override
    public void onBegin() {
        enemy.setImmobile(true);
        enemy.setInvulnerable(true);
    }

    @Override
    public void onEnd() {
        enemy.setImmobile(false);
        enemy.setInvulnerable(false);
    }

    @Override
    public boolean onTick(int ticks) {
        List<Player> players = context.getPlayers();
        players.removeIf(p -> !enemy.hasLineOfSight(p));
        if (players.isEmpty()) return true;
        Player target = players.get(random.nextInt(players.size()));
        Vector velo = target.getEyeLocation()
            .subtract(enemy.getEyeLocation())
            .toVector().normalize().multiply(2.0);
        Trident trident = (Trident) enemy.launchProjectile(Trident.class, velo);
        trident.setPersistent(false);
        trident.setPickupStatus(Trident.PickupStatus.DISALLOWED);
        context.registerTemporaryEntity(trident);
        return true;
    }
}

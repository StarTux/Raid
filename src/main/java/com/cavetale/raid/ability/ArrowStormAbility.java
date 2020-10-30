package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

/**
 * Shoot 1 arrow per tick at a random nearby player.
 */
public final class ArrowStormAbility extends AbstractAbility {
    @Getter @Setter private int interval = 0;
    private int intervalTicks = 0;
    private Random random = new Random();
    private List<PotionEffect> potionEffects = new ArrayList<>();
    @Getter @Setter private double damage = 5.0;
    @Getter @Setter private int pierceLevel = 3;

    public ArrowStormAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 200;
        warmup = 20;
    }

    @Override
    public void onBegin() {
        enemy.setInvulnerable(true);
        enemy.setImmobile(true);
        enemy.getWorld().spawnParticle(Particle.FLASH, enemy.getEyeLocation(), 5, 0.5, 0.5, 0.5, 0.0);
    }

    @Override
    public void onEnd() {
        enemy.setInvulnerable(false);
        enemy.setImmobile(false);
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
        List<Player> players = context.getPlayers();
        players.removeIf(p -> !enemy.hasLineOfSight(p));
        if (players.isEmpty()) return true;
        Player target = players.get(random.nextInt(players.size()));
        Vector velo = target.getEyeLocation()
            .subtract(enemy.getEyeLocation())
            .toVector().normalize()
            .add(new Vector(rnd() * 0.1,
                            random.nextDouble() * 0.2,
                            rnd() * 0.1))
            .multiply(2.0);
        Arrow arrow = enemy.launchProjectile(Arrow.class, velo);
        arrow.setDamage(damage);
        arrow.setPierceLevel(pierceLevel);
        arrow.setPersistent(false);
        context.registerTemporaryEntity(arrow);
        if (!potionEffects.isEmpty()) {
            PotionEffect potionEffect = potionEffects.get(random.nextInt(potionEffects.size()));
            arrow.addCustomEffect(potionEffect, true);
        }
        enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 1.0f, 1.2f);
        return true;
    }

    public void clearPotionEffects() {
        potionEffects.clear();
    }

    public void addPotionEffect(PotionEffect effect) {
        potionEffects.add(effect);
    }
}

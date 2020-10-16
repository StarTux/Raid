package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Enemy;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

/**
 * Shoot 1 arrow per tick at a random nearby player.
 */
public final class ArrowStormAbility extends AbstractAbility {
    @Getter private boolean damaging = true;
    @Getter private boolean freezing = false;
    private Random random = new Random();

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

    public void setDamaging(final boolean damaging) {
        this.damaging = true;
        this.freezing = false;
    }

    public void setFreezing(final boolean freezing) {
        this.freezing = true;
        this.damaging = false;
    }

    private double rnd() {
        return random.nextBoolean()
            ? random.nextDouble()
            : -random.nextDouble();
    }

    @Override
    public boolean onTick(int ticks) {
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
        arrow.setDamage(5.0);
        arrow.setPierceLevel(3);
        arrow.setPersistent(false);
        context.registerTemporaryEntity(arrow);
        if (damaging) {
            arrow.setFireTicks(6000);
            switch (random.nextInt(4)) {
            case 0:
                arrow.setBasePotionData(new PotionData(PotionType.INSTANT_DAMAGE, false, true));
                break;
            case 1:
                arrow.setBasePotionData(new PotionData(PotionType.POISON, false, true));
                break;
            case 2:
                arrow.setBasePotionData(new PotionData(PotionType.WEAKNESS, true, false));
                break;
            case 3:
                arrow.addCustomEffect(new PotionEffect(PotionEffectType.HUNGER, 400, 1), true);
                break;
            default: break;
            }
        } else if (freezing) {
            switch (random.nextInt(2)) {
            case 0:
                arrow.setBasePotionData(new PotionData(PotionType.SLOWNESS, false, true));
                break;
            case 1:
                arrow.setBasePotionData(new PotionData(PotionType.WEAKNESS, true, false));
                break;
            default: break;
            }
        }
        enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 1.0f, 1.2f);
        return true;
    }
}

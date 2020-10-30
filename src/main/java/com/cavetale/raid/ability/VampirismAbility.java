package com.cavetale.raid.ability;

import com.cavetale.mytems.MytemsPlugin;
import com.cavetale.mytems.item.AculaItemSet;
import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class VampirismAbility extends AbstractAbility {
    @Getter @Setter private int interval = 20;
    private int intervalTicks = 0;
    @Getter @Setter private int active = 60;
    private int activeTicks = 0;
    @Getter @Setter private double damagePerTick = 1.0;
    @Getter @Setter private int foodPerSecond = 0;
    @Getter @Setter private float saturationPerTick = 0f;

    public VampirismAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 200;
    }

    @Override
    public void onBegin() { }

    @Override
    public void onEnd() { }

    @Override
    public boolean onTick(int ticks) {
        if (intervalTicks > 0) {
            intervalTicks -= 1;
            if (intervalTicks == 0) {
                activeTicks = active;
            }
            return true;
        }
        if (activeTicks > 0) {
            activeTicks -= 1;
        } else {
            intervalTicks = interval;
        }
        Location eye = enemy.getEyeLocation();
        for (Player player : context.getPlayers()) {
            if (!enemy.hasLineOfSight(player)) continue;
            Vector vec = player.getEyeLocation().subtract(eye).toVector().multiply(Math.random());
            Location particleLocation = eye.clone().add(vec);
            if (isImmune(player)) {
                enemy.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.BLUE, 1.0f));
            } else {
                double damage = Math.min(player.getHealth(), damagePerTick);
                player.setHealth(player.getHealth() - damage);
                enemy.setHealth(Math.min(enemy.getMaxHealth(), enemy.getHealth() + damage));
                enemy.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 0.5f));
                if (activeTicks % 16 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.5f, 2.0f);
                }
                if (foodPerSecond > 0 && activeTicks % 20 == 0) {
                    player.setFoodLevel(Math.max(0, player.getFoodLevel() - foodPerSecond));
                }
                if (saturationPerTick > 0f) {
                    player.setSaturation(Math.max(0, player.getSaturation() - saturationPerTick));
                }
            }
        }
        return true;
    }

    public boolean isImmune(Player player) {
        return MytemsPlugin.getInstance().getEquipment(player).hasSetBonus(AculaItemSet.getInstance().vampirismResistance);
    }
}

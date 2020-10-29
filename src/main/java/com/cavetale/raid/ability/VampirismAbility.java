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
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class VampirismAbility extends AbstractAbility {
    @Getter @Setter private int interval = 20;
    private int intervalTicks = 0;
    @Getter @Setter private int active = 60;
    private int activeTicks = 0;
    @Getter @Setter private double damagePerTick = 1.0;

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
            if (activeTicks == 0) {
                intervalTicks = interval;
            }
        }
        Location eye = enemy.getEyeLocation();
        for (Player player : context.getPlayers()) {
            if (!enemy.hasLineOfSight(player)) continue;
            Vector vec = player.getEyeLocation().subtract(eye).toVector().multiply(Math.random());
            Location particleLocation = eye.clone().add(vec);
            if (MytemsPlugin.getInstance().getEquipment(player).hasSetBonus(AculaItemSet.getInstance().vampirismResistance)) {
                enemy.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.BLUE, 1.0f));
            } else {
                player.setHealth(Math.max(0, player.getHealth() - damagePerTick));
                enemy.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.RED, 0.5f));
            }
        }
        return true;
    }
}

package com.cavetale.raid.ability;

import com.cavetale.raid.ItemBuilder;
import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

public final class SplashPotionAbility extends AbstractAbility {
    @Getter @Setter private int interval = 10;
    private int intervalTicks = 0;
    private Random random = new Random();

    public SplashPotionAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 100;
        warmup = 20;
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
            Vector vec = loc.subtract(eye).toVector().normalize().multiply(2.0);
            ThrownPotion potion = enemy.launchProjectile(ThrownPotion.class, vec);
            if (random.nextBoolean()) {
                potion.setItem(new ItemBuilder(Material.SPLASH_POTION)
                               .basePotion(PotionType.POISON, false, false)
                               .create());
            } else {
                potion.setItem(new ItemBuilder(Material.SPLASH_POTION)
                               .customEffect(PotionEffectType.CONFUSION, 100, 0)
                               .create());
            }
        }
        return true;
    }
}

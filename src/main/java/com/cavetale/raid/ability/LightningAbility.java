package com.cavetale.raid.ability;

import com.cavetale.raid.Place;
import com.cavetale.raid.enemy.Enemy;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.Particle;

public final class LightningAbility extends AbstractAbility {
    @Getter @Setter private int interval = 40;
    private int intervalTicks = 0; // count down
    private int lightningTicks = 0; //count up
    private UUID lightningTarget;
    private List<Place> lightningSpots = new ArrayList<>();
    private final Random random = new Random();

    public LightningAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 400;
    }

    @Override
    public void onBegin() {
        lightningTarget = null;
        enemy.getWorld().spawnParticle(Particle.FLASH, enemy.getEyeLocation(), 5, 0.5, 0.5, 0.5, 0.0);
        lightningSpots.clear();
    }

    @Override
    public void onEnd() {
    }

    @Override
    public boolean onTick(int ticks) {
        if (intervalTicks > 0) {
            intervalTicks -= 1;
            if (intervalTicks == 0) lightningTicks = 0;
            return true;
        }
        //
        final int stateTicks = lightningTicks++;
        if (stateTicks == 10) {
            findLightningSpot(context.getPlayers());
        } else if (stateTicks == 30) {
            for (Place spot : lightningSpots) {
                enemy.getWorld().strikeLightning(spot.toLocation(enemy.getWorld()));
            }
            intervalTicks = interval;
        }
        return true;
    }

    void findLightningSpot(List<Player> players) {
        lightningSpots.clear();
        Player target;
        if (lightningTarget == null) {
            target = players
                .get(random.nextInt(players.size()));
            lightningTarget = target.getUniqueId();
        } else {
            target = null;
            for (Player p : players) {
                if (p.getUniqueId().equals(lightningTarget)) {
                    target = p;
                }
            }
            if (target == null) {
                lightningTarget = null;
                return;
            }
        }
        target.getWorld().spawnParticle(Particle.END_ROD, target.getEyeLocation(),
                                     6, 0.2, 0.2, 0.2, 0);
        lightningSpots.add(Place.of(target.getLocation()));
    }
}

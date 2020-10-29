package com.cavetale.raid;

import com.cavetale.raid.enemy.Context;
import com.cavetale.raid.enemy.Enemy;
import com.cavetale.raid.enemy.LivingEnemy;
import com.cavetale.worldmarker.EntityMarker;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.event.entity.EntityTargetEvent;

/**
 * An enemy based on a regular bukkit entity type.
 * They will mostly do their vanilla thing.
 */
public final class SpawnEnemy extends LivingEnemy {
    private final Location location;
    private Class<? extends Mob> type;
    private boolean targeting;

    public SpawnEnemy(final Context context, final Class<? extends Mob> type, final Location location) {
        super(context);
        this.type = type;
        this.location = location;
    }

    @Override
    public void spawn(Location location2) {
        if (!location2.isChunkLoaded()) return;
        living = location.getWorld().spawn(location2, type, this::prep);
        markLiving();
    }

    @Override
    public void spawn() {
        spawn(location);
    }

    @Override
    public void tick() {
        if (!isAcceptableMobTarget(((Mob) living).getTarget())) {
            Player target = findTarget();
            if (target != null) {
                targeting = true;
                ((Mob) living).setTarget(target);
                targeting = false;
            }
        }
    }

    @Override
    public void onRemove() {
    }

    private void prep(Mob mob) {
        if (mob instanceof Bee) {
            Bee bee = (Bee) mob;
            bee.setAnger(12000);
        }
        double multiplier = 1.0 + 0.25 * (double) context.getPlayers().size();
        // Health
        AttributeInstance inst = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = inst.getBaseValue();
        if (mob instanceof Rabbit) {
            Rabbit rabbit = (Rabbit) mob;
            rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
            maxHealth = 20.0;
        }
        double health = maxHealth * multiplier;
        inst.setBaseValue(health);
        mob.setHealth(health);
        // Damage
        if (inst != null) {
            double damage = inst.getBaseValue();
            if (mob instanceof Bee) {
                inst = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                inst.setBaseValue(5.0);
            }
        } else {
            context.getPlugin().getLogger().info("No attack damage: " + mob.getType());
        }
    }

    /**
     * Return the closest visible player within 32 blocks distance.
     * If none exists, pick the closest non-visible player within 16 blocks.
     */
    public Player findTarget() {
        Location eye = living.getEyeLocation();
        double minVisible = Double.MAX_VALUE;
        double minBlind = Double.MAX_VALUE;
        Player visible = null;
        Player blind = null;
        final double maxVisible = 32 * 32;
        final double maxBlind = 16 * 16;
        for (Player player : context.getPlayers()) {
            double dist = player.getEyeLocation().distanceSquared(eye);
            if (living.hasLineOfSight(player)) {
                if (dist < minVisible && dist < maxVisible) {
                    visible = player;
                    minVisible = dist;
                }
            } else {
                if (dist < minBlind && dist < maxBlind) {
                    blind = player;
                    minBlind = dist;
                }
            }
        }
        return visible != null ? visible : null;
    }

    static boolean isAcceptableMobTarget(Entity target) {
        if (target == null) return false;
        if (EntityMarker.hasId(target, Enemy.WORLD_MARKER_ID)) return false;
        switch (target.getType()) {
        case PLAYER:
        case WOLF:
        case CAT:
            return true;
        default:
            return false;
        }
    }

    @Override
    public void onTarget(EntityTargetEvent event) {
        if (targeting) return;
        if (isAcceptableMobTarget(event.getTarget())) return;
        Player newTarget = findTarget();
        if (newTarget != null) event.setTarget(newTarget);
    }
}

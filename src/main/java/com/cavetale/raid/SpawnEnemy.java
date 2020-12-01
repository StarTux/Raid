package com.cavetale.raid;

import com.cavetale.enemy.Context;
import com.cavetale.enemy.Enemy;
import com.cavetale.enemy.LivingEnemy;
import com.cavetale.enemy.util.Prep;
import com.cavetale.worldmarker.EntityMarker;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

/**
 * An enemy based on a regular bukkit entity type.
 * They will mostly do their vanilla thing.
 */
public final class SpawnEnemy extends LivingEnemy {
    private Class<? extends Mob> type;
    private Spawn spawn;
    private boolean targeting;
    private Entity mount;

    public SpawnEnemy(final Context context, final Class<? extends Mob> type, final Spawn spawn) {
        super(context);
        this.type = type;
        this.spawn = spawn;
    }

    @Override
    public void spawn(Location location) {
        if (!location.isChunkLoaded()) return;
        living = location.getWorld().spawn(location, type, this::prep);
        markLiving();
        if (spawn.mount != null) {
            mount = location.getWorld().spawn(location, spawn.mount.getEntityClass(), this::prepMount);
            context.registerTemporaryEntity(mount);
            mount.addPassenger(living);
        }
    }

    @Override
    public void tick() {
        if (!isAcceptableMobTarget(((Mob) living).getTarget(), context)) {
            Player target = findTarget(living, context);
            if (target != null) {
                targeting = true;
                ((Mob) living).setTarget(target);
                targeting = false;
            }
        }
    }

    @Override
    public void onRemove() {
        if (mount != null) {
            mount.remove();
            mount = null;
        }
    }

    private void prepAttr(Mob mob, Attribute attribute, double multiplier) {
        if (!Prep.hasAttr(mob, attribute)) return;
        Double base = spawn.attributes != null && spawn.attributes.containsKey(attribute)
            ? spawn.attributes.get(attribute)
            : Prep.getAttr(mob, attribute);
        double value = base * multiplier;
        Prep.attr(mob, attribute, value);
        if (attribute == Attribute.GENERIC_MAX_HEALTH) {
            mob.setHealth(value);
        }
    }

    private void prep(Mob mob) {
        mob.setPersistent(false);
        // Attributes
        double multiplier = spawn.scaling != 0
            ? 1.0 + (context.getPlayers().size() - 1) * spawn.scaling * spawn.scaling
            : 1.0;
        prepAttr(mob, Attribute.GENERIC_MAX_HEALTH, 1.0);
        prepAttr(mob, Attribute.GENERIC_ATTACK_DAMAGE, multiplier);
        prepAttr(mob, Attribute.GENERIC_ARMOR, multiplier);
        prepAttr(mob, Attribute.GENERIC_ARMOR_TOUGHNESS, 1.0); // No scaling
        prepAttr(mob, Attribute.GENERIC_MOVEMENT_SPEED, 1.0);
        prepAttr(mob, Attribute.GENERIC_ATTACK_KNOCKBACK, 1.0);
        prepAttr(mob, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
        // Equipment
        EntityEquipment equipment = mob.getEquipment();
        if (spawn.hand != null) equipment.setItemInHand(new ItemStack(spawn.hand));
        equipment.setHelmet(spawn.helmet != null ? new ItemStack(spawn.helmet) : null);
        equipment.setChestplate(spawn.chestplate != null ? new ItemStack(spawn.chestplate) : null);
        equipment.setLeggings(spawn.leggings != null ? new ItemStack(spawn.leggings) : null);
        equipment.setBoots(spawn.boots != null ? new ItemStack(spawn.boots) : null);
        Prep.disableEquipmentDrop(mob);
        // Special
        if (mob instanceof Bee) {
            Bee bee = (Bee) mob;
            bee.setAnger(12000);
        }
        if (mob instanceof Creeper) {
            Creeper creeper = (Creeper) mob;
            creeper.setPowered(spawn.powered);
        }
        if (mob instanceof Ageable) {
            Ageable ageable = (Ageable) mob;
            if (spawn.baby) {
                ageable.setBaby();
            } else {
                ageable.setAdult();
            }
            ageable.setAgeLock(true);
        }
        if (mob instanceof Rabbit) {
            Rabbit rabbit = (Rabbit) mob;
            rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
        }
    }

    private void prepMount(Entity entity) {
        entity.setPersistent(false);
        if (entity instanceof Mob) {
            Mob mob = (Mob) entity;
            Prep.disableEquipmentDrop(mob);
        }
    }

    /**
     * Return the closest visible player within 32 blocks distance.
     * If none exists, pick the closest non-visible player within 16 blocks.
     */
    public static Player findTarget(LivingEntity living, Context context) {
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

    public static boolean isAcceptableMobTarget(Entity target, Context context) {
        if (target == null) return false;
        if (EntityMarker.hasId(target, Enemy.WORLD_MARKER_ID)) return false;
        if (context.isTemporaryEntity(target)) return false;
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
        if (isAcceptableMobTarget(event.getTarget(), context)) return;
        Player newTarget = findTarget(living, context);
        if (newTarget != null) event.setTarget(newTarget);
    }
}

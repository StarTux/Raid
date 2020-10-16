package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Enemy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;

/**
 * Spawn one or more simple entities as adds.
 */
public final class SpawnAddsAbility extends AbstractAbility {
    @Getter @Setter private int interval = 20;
    @Getter @Setter private int simultaneous = 1;
    private int intervalTicks = 0;
    private List<Add> adds = new ArrayList<>();

    public SpawnAddsAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
    }

    @Value
    private static final class Add<T extends Entity> {
        private final Class<T> type;
        private final int maximum;
        private final int simultaneous;
        private final Consumer<T> callback;
    }

    public <T> void add(Class<T> type, int maximum, int simul, Consumer<T> callback) {
        adds.add(new Add(type, maximum, simul, callback));
    }

    @Override
    public void onBegin() {
    }

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
        // Spawner block particle animation
        enemy.getWorld().spawnParticle(Particle.FLAME, enemy.getEyeLocation(), 8, 0.5, 0.5, 0.5, 0.0);
        int spawned = 0;
        for (Add add : adds) {
            spawned += spawnAdd(add);
            if (spawned >= simultaneous) break;
        }
        return true;
    }

    int spawnAdd(Add add) {
        int count = context.countTemporaryEntities(add.type);
        while (count < add.maximum && count < add.simultaneous) {
            Entity entity = enemy.getWorld().spawn(enemy.getLocation(), add.type, e -> spawnCallback(e, add));
            if (entity != null) {
                count += 1;
                context.registerTemporaryEntity(entity);
            }
        }
        return count;
    }

    private <T extends Entity> void spawnCallback(T entity, Add<T> add) {
        entity.setPersistent(false);
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            living.setRemoveWhenFarAway(true);
            EntityEquipment eq = living.getEquipment();
            if (eq != null) {
                eq.setHelmetDropChance(0.0f);
                eq.setChestplateDropChance(0.0f);
                eq.setLeggingsDropChance(0.0f);
                eq.setBootsDropChance(0.0f);
                eq.setItemInMainHandDropChance(0.0f);
                eq.setItemInOffHandDropChance(0.0f);
            }
        }
        if (add.callback != null) add.callback.accept(entity);
    }
}

// switch (boss.type) {
// case DECAYED:
//     if (phaseTicks == 40
//         || phaseTicks == 60
//         || phaseTicks == 80) {
//         adds.add(mob.getWorld().spawn(mob.getLocation(),
//                                       WitherSkeleton.class, this::prepAdd));
//     }
//     break;
// case FORGOTTEN:
//     if (phaseTicks == 20
//         || phaseTicks == 40
//         || phaseTicks == 60
//         || phaseTicks == 80
//         || phaseTicks == 100) {
//         Location loc = mob.getLocation()
//             .add(instance.plugin.rnd() * 8.0,
//                  instance.plugin.rnd() * 8.0,
//                  instance.plugin.rnd() * 8.0);
//         adds.add(mob.getWorld().spawn(loc, Vex.class, this::prepVex));
//     }
//     break;
// case SKELLINGTON:
//     if (phaseTicks > 0 && phaseTicks % 15 == 0) {
//         adds.add(mob.getWorld().spawn(mob.getLocation(),
//                                       Skeleton.class, this::prepAdd));
//     }
//     break;
// case DEEP_FEAR:
//     long guardians = adds.stream().filter(m -> m instanceof Guardian).count();
//     if (guardians < 2) {
//         if (phaseTicks == 30) {
//             adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
//                                           Guardian.class, this::prepAdd));
//         }
//     }
//     if (phaseTicks > 0 && phaseTicks % 20 == 0) {
//         long drowned = adds.stream().filter(m -> m instanceof Drowned).count();
//         if (drowned < 5) {
//             ItemStack trident = new ItemBuilder(Material.TRIDENT).create();
//             Drowned d = instance.world
//                 .spawn(mob.getEyeLocation(),
//                        Drowned.class, e -> {
//                            e.getEquipment().setItemInMainHand(trident);
//                            e.setHealth(2.0);
//                            prepAdd(e);
//                        });
//             adds.add(d);
//         }
//     }
//     if (phaseTicks > 0 && phaseTicks % 20 == 10) {
//         adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
//                                       PufferFish.class, this::prepAdd));
//     }
//     break;
// case LAVA_LORD:
//     if (phaseTicks > 0 && phaseTicks % 20 == 0) {
//         adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
//                                       Blaze.class, this::prepAdd));
//     }
//     break;
// case ICE_GOLEM:
//     if (phaseTicks > 0 && phaseTicks % 10 == 0) {
//         int num = instance.plugin.random.nextInt(10);
//         if (num == 0) {
//             adds.add(mob.getWorld().spawn(mob.getLocation(),
//                                           Creeper.class, this::prepAdd));
//         } else if (num < 4) {
//             adds.add(mob.getWorld().spawn(mob.getLocation(),
//                                           Drowned.class, this::prepAdd));
//         } else {
//             adds.add(mob.getWorld().spawn(mob.getLocation(),
//                                           Stray.class, this::prepAdd));
//         }
//     }
//     break;
// case ICEKELLY:
//     if (phaseTicks > 0 && phaseTicks % 20 == 0) {
//         adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
//                                       Vex.class, this::prepVex));
//     }
//     break;
// case SNOBEAR:
//     if (phaseTicks > 0 && phaseTicks % 10 == 0) {
//         adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
//                                       Blaze.class, this::prepAdd));
//     }
//     break;
// case QUEEN_BEE:
//     if (phaseTicks > 0 && phaseTicks % 5 == 0) {
//         adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
//                                       Bee.class, e -> {
//                                           e.setAnger(72000);
//                                           prepAdd(e);
//                                       }));
//     }
//     break;
// case HEINOUS_HEN:
//     if (phaseTicks > 0 && phaseTicks % 5 == 0) {
//         adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
//                                       Bee.class, e -> {
//                                           e.setAnger(72000);
//                                           prepAdd(e);
//                                           e.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12.0f);
//                                       }));
//         adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
//                                       Rabbit.class, e -> {
//                                           e.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
//                                           e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0f);
//                                           e.setHealth(20.0f);
//                                       }));
//     }
//     break;
// case SPECTER:
//     if (phaseTicks > 0 && phaseTicks % 20 == 0) {
//         if (instance.plugin.random.nextBoolean()) {
//             adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
//                                           Blaze.class, this::prepAdd));
//         } else {
//             adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
//                                           Phantom.class, this::prepAdd));
//         }
//     }
//     break;
// default: break;
// }

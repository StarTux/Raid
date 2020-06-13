package com.cavetale.raid;

import com.cavetale.worldmarker.EntityMarker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Egg;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Illusioner;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LlamaSpit;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Stray;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * Runtime class for Boss.
 */
@RequiredArgsConstructor
final class BossFight {
    final Instance instance;
    final Boss boss;
    Mob mob;
    boolean killed = false;
    Phase phase = Phase.IDLE;
    List<Phase> phases;
    int phaseIndex = 0;
    long phaseTicks;
    double backupSpeed;
    double mountBackupSpeed;
    boolean phaseComplete = false;
    int maxHealth = 1000;
    final List<Mob> adds = new ArrayList<>();
    List<String> dialogue;
    int dialogueIndex;
    boolean invulnerable;
    boolean shield;
    int shieldCooldown;
    boolean tookDamage;
    final Set<UUID> damagers = new HashSet<>();
    UUID lightningTarget;
    List<Place> lightningSpots = new ArrayList<>();
    static final int ADDS_LIMIT = 40;
    static final double MAX_DISTANCE = 32;
    static final String EXPLOSIVE_EGG = "raid:explosive_egg";

    enum Phase {
        IDLE,
        FIREWORK,
        ADDS,
        DIALOGUE,
        PAUSE,
        PULL,
        PUSH,
        FIREBALL,
        POTION,
        ARROWS,
        MOUNT,
        TRIDENTS,
        HOME,
        WARP,
        LIGHTNING_SINGLE,
        SLIP_ICE,
        LLAMA_SPIT,
        THROW,
        LEVITATE,
        EGG_LAUNCHER;
    }

    boolean isPresent() {
        return mob != null
            && mob.isValid();
    }

    void cleanup() {
        if (mob != null && mob.isValid()) mob.remove();
        for (Mob add : adds) {
            if (add.isValid()) add.remove();
        }
        adds.clear();
    }

    Mob spawn(@NonNull Location loc) {
        World w = loc.getWorld();
        switch (boss.type) {
        case DECAYED:
            return w.spawn(loc, WitherSkeleton.class, e -> {
                    EntityEquipment eq = e.getEquipment();
                    eq.setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN).create());
                    eq.setItemInMainHand(new ItemBuilder(Material.DIAMOND_SWORD)
                                         .ench(Enchantment.KNOCKBACK, 2)
                                         .ench(Enchantment.DAMAGE_ALL, 5).create());
                    prep(e);
                });
        case FORGOTTEN:
            return w.spawn(loc, Evoker.class, e -> {
                    prep(e);
                });
        case VENGEFUL:
            return w.spawn(loc, Wither.class, e -> {
                    prep(e);
                });
        case SKELLINGTON:
            return w.spawn(loc, Skeleton.class, e -> {
                    EntityEquipment eq = e.getEquipment();
                    eq.setHelmet(new ItemBuilder(Material.GOLDEN_HELMET).create());
                    eq.setChestplate(new ItemBuilder(Material.GOLDEN_CHESTPLATE).create());
                    eq.setLeggings(new ItemBuilder(Material.GOLDEN_LEGGINGS).create());
                    eq.setBoots(new ItemBuilder(Material.GOLDEN_BOOTS).create());
                    prep(e);
                });
        case DEEP_FEAR:
            return w.spawn(loc, ElderGuardian.class, e -> {
                    prep(e);
                });
        case LAVA_LORD:
            return w.spawn(loc, MagmaCube.class, e -> {
                    e.setSize(1);
                    prep(e);
                });
        case FROSTWRECKER:
            return w.spawn(loc, Illusioner.class, e -> {
                    prep(e);
                });
        case ICE_GOLEM:
            return w.spawn(loc, Snowman.class, e -> {
                    e.setDerp(true);
                    prep(e);
                });
        case ICEKELLY:
            return w.spawn(loc, Skeleton.class, e -> {
                    EntityEquipment eq = e.getEquipment();
                    eq.setHelmet(new ItemBuilder(Material.CHAINMAIL_HELMET).create());
                    eq.setChestplate(new ItemBuilder(Material.CHAINMAIL_CHESTPLATE).create());
                    eq.setLeggings(new ItemBuilder(Material.CHAINMAIL_LEGGINGS).create());
                    eq.setBoots(new ItemBuilder(Material.CHAINMAIL_BOOTS).create());
                    prep(e);
                });
        case SNOBEAR:
            return w.spawn(loc, PolarBear.class, e -> {
                    prep(e);
                });
        case QUEEN_BEE:
            return w.spawn(loc, Bee.class, e -> {
                    e.setHasNectar(true);
                    e.setAnger(72000);
                    prep(e);
                });
        case HEINOUS_HEN:
            return w.spawn(loc, Chicken.class, e -> {
                    prep(e);
                });
        case SPECTER:
            return w.spawn(loc, Phantom.class, e -> {
                    e.setSize(10);
                    prep(e);
                });
        default:
            throw new IllegalArgumentException(boss.type.name());
        }
    }

    List<String> getDialogue() {
        switch (boss.type) {
        case DECAYED:
            return Arrays
                .asList("Your effort is in vain.",
                        "The show's over with my next move.",
                        "Welcome home.");
        case FORGOTTEN:
            return Arrays
                .asList("The journey ends here.",
                        "My powers are beyond your understanding.",
                        "You were foolish to come here.");
        case VENGEFUL:
            return Arrays
                .asList("Like the crops in the field, you too shall wither away.",
                        "Your time has come.",
                        "Death draws nearer with every moment.");
        case SKELLINGTON:
            return Arrays.
                asList("Who dares to enter these halls?",
                       "You shall be added to my collection.",
                       "This room will be your tomb.");
        case DEEP_FEAR:
            return Arrays
                .asList("Blub!",
                        "Blub blub blubbb blllub",
                        "Blooob blub blub blub blub...");
        case LAVA_LORD:
            return Arrays
                .asList("You've made it this far. Now you shall perish.",
                        "Christmas will come this year over my dead bones.",
                        "This is where it ends. For you.");
        case FROSTWRECKER:
            return Arrays
                .asList("Join the others on the icy floor.",
                        "Give in to the stream.",
                        "Become one with the ocean.");
        case ICE_GOLEM:
            return Arrays
                .asList("Prepare to slip on my grounds!",
                        "Behold your own reflection.",
                        "No foothold for you!");
        case ICEKELLY:
            return Arrays
                .asList("My arrows are cooler than yours.",
                        "These halls are not for you!",
                        "Take them, my minions!");
        case SNOBEAR:
            return Arrays
                .asList("Out of my cave!",
                        "You think you're stronger than me?",
                        "I'll show you.");
        case QUEEN_BEE:
            return Arrays
                .asList("Bzzzzz!",
                        "Bww bww bwwww buzzzzz!",
                        "Zzzzzzzzz...");
        case HEINOUS_HEN:
            return Arrays
                .asList("Easter belongs to ME!",
                        "You want eggs?  Here's some more!",
                        "You won't stop me from hatching my plan!");
        case SPECTER:
            return Arrays
                .asList("Down here we all float",
                        "You can run but you can't hide!",
                        "Don't fall asleep");
        default:
            return Arrays.asList("You'll never defeat StarTuuuuux!!!");
        }
    }

    List<Phase> getPhases() {
        switch (boss.type) {
        case DECAYED:
            return Arrays.asList(Phase.DIALOGUE, Phase.FIREWORK, Phase.PAUSE, Phase.ADDS);
        case FORGOTTEN:
            return Arrays.asList(Phase.DIALOGUE, Phase.PULL, Phase.POTION, Phase.PAUSE, Phase.ADDS);
        case VENGEFUL:
            return Arrays.asList(Phase.DIALOGUE, Phase.PUSH, Phase.PAUSE, Phase.FIREBALL);
        case SKELLINGTON:
            return Arrays
                .asList(Phase.DIALOGUE, Phase.MOUNT, Phase.PAUSE,
                        Phase.ARROWS, Phase.ADDS, Phase.PAUSE);
        case DEEP_FEAR:
            return Arrays
                .asList(Phase.DIALOGUE, Phase.PAUSE, Phase.ADDS, Phase.PAUSE);
        case LAVA_LORD:
            return Arrays
                .asList(Phase.DIALOGUE, Phase.PAUSE, Phase.ADDS, Phase.PAUSE, Phase.HOME);
        case FROSTWRECKER:
            return Arrays
                .asList(Phase.DIALOGUE, Phase.PAUSE, Phase.LIGHTNING_SINGLE, Phase.WARP,
                        Phase.PAUSE);
        case ICE_GOLEM:
            return Arrays
                .asList(Phase.DIALOGUE, Phase.SLIP_ICE, Phase.PAUSE, Phase.LLAMA_SPIT,
                        Phase.HOME, Phase.ADDS, Phase.PAUSE);
        case ICEKELLY:
            return Arrays
                .asList(Phase.DIALOGUE, Phase.PAUSE, Phase.HOME,
                        Phase.ARROWS, Phase.ADDS, Phase.PAUSE);
        case SNOBEAR:
            return Arrays
                .asList(Phase.DIALOGUE, Phase.THROW, Phase.PAUSE,
                        Phase.LEVITATE, Phase.ADDS);
        case QUEEN_BEE:
            return Arrays
                .asList(Phase.DIALOGUE, Phase.PAUSE,
                        Phase.ADDS, Phase.HOME);
        case HEINOUS_HEN:
            return Arrays
                .asList(Phase.DIALOGUE, Phase.EGG_LAUNCHER,
                        Phase.ADDS, Phase.PUSH);
        case SPECTER:
            return Arrays
                .asList(Phase.DIALOGUE, Phase.PAUSE, Phase.FIREBALL, Phase.PAUSE, Phase.ADDS);
        default: return Arrays.asList(Phase.PAUSE);
        }
    }

    void onTick(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phase == Phase.IDLE) {
            phases = getPhases();
            phaseIndex = 0;
            phase = phases.get(0);
            phaseTicks = 0;
        }
        adds.removeIf(e -> !e.isValid());
        for (Mob add : adds) {
            if (!instance.isAcceptableMobTarget(add.getTarget())) {
                Player target = instance.findTarget(add, players);
                if (target != null) add.setTarget(target);
            }
        }
        switch (boss.type) {
        case DEEP_FEAR:
            tickDeepFear(wave, players);
            break;
        case LAVA_LORD:
            tickLavaLord(wave, players);
            break;
        case QUEEN_BEE: {
            Bee bee = (Bee) mob;
            bee.setAnger(72000);
            bee.setHasStung(false);
            break;
        }
        default: break;
        }
        switch (phase) {
        case FIREWORK:
            tickFirework(wave, players);
            break;
        case ADDS:
            tickAdds(wave, players);
            break;
        case DIALOGUE:
            tickDialogue(wave, players);
            break;
        case PULL:
            tickPull(wave, players);
            break;
        case PUSH:
            tickPush(wave, players);
            break;
        case FIREBALL:
            tickFireball(wave, players);
            break;
        case POTION:
            tickPotion(wave, players);
            break;
        case ARROWS:
            tickArrows(wave, players);
            break;
        case TRIDENTS:
            tickTridents(wave, players);
            break;
        case MOUNT:
            tickMount(wave, players);
            break;
        case PAUSE:
            phaseComplete = phaseTicks >= 100;
            break;
        case HOME:
            phaseComplete = phaseTicks >= 40;
            if (phaseTicks == 20) {
                mob.teleport(wave.place.toLocation(instance.world));
            }
            break;
        case WARP: tickWarp(wave, players); break;
        case LIGHTNING_SINGLE: tickLightningSingle(wave, players); break;
        case SLIP_ICE: tickSlipIce(wave, players); break;
        case LLAMA_SPIT: tickLlamaSpit(wave, players); break;
        case THROW: tickThrow(wave, players); break;
        case LEVITATE: tickLevitate(wave, players); break;
        case EGG_LAUNCHER: tickEggLauncher(wave, players); break;
        default: break;
        }
        if (phaseComplete) {
            phaseComplete = false;
            phaseIndex += 1;
            if (phaseIndex >= phases.size()) phaseIndex = 0;
            phase = phases.get(phaseIndex);
            phaseTicks = 0;
        } else {
            phaseTicks += 1;
            if (!instance.isAcceptableMobTarget(mob.getTarget())) {
                Player target = instance.findTarget(mob, players);
                if (target != null) mob.setTarget(target);
            }
        }
    }


    void onBossDeath() {
        Player killer = mob.getKiller();
        if (killer != null) damagers.add(killer.getUniqueId());
        ItemBuilder reward;
        switch (boss.type) {
        case DEEP_FEAR:
            reward = new ItemBuilder(Material.TRIDENT);
            break;
        case VENGEFUL:
            reward = new ItemBuilder(Material.NETHER_STAR);
            break;
        case DECAYED:
            reward = new ItemBuilder(Material.WITHER_SKELETON_SKULL);
            break;
        case FORGOTTEN:
            reward = new ItemBuilder(Material.TOTEM_OF_UNDYING);
            break;
        case SKELLINGTON:
            reward = new ItemBuilder(Material.SKELETON_SKULL);
            break;
        case LAVA_LORD:
            reward = new ItemBuilder(Material.GHAST_TEAR).amount(5);
            break;
        case FROSTWRECKER:
            reward = new ItemBuilder(Material.ENCHANTED_BOOK)
                .enchStore(Enchantment.CHANNELING, 1);
            break;
        case ICE_GOLEM:
            reward = new ItemBuilder(Material.ENCHANTED_BOOK)
                .enchStore(Enchantment.FROST_WALKER, 2);
            break;
        case ICEKELLY:
            reward = new ItemBuilder(Material.ENCHANTED_BOOK)
                .enchStore(Enchantment.ARROW_INFINITE, 1);
            break;
        case SNOBEAR:
            reward = new ItemBuilder(Material.ENCHANTED_BOOK)
                .enchStore(Enchantment.DAMAGE_ALL, 5);
            break;
        case QUEEN_BEE:
            reward = new ItemBuilder(Material.BEE_NEST);
            break;
        case SPECTER:
            reward = new ItemBuilder(Material.PHANTOM_MEMBRANE).amount(64);
            break;
        default:
            reward = new ItemBuilder(Material.DIAMOND_BLOCK);
            break;
        }
        for (Player player : instance.getPlayers()) {
            player.sendTitle(ChatColor.GOLD + boss.type.displayName,
                             ChatColor.RED + "Defeated!");
            if (!damagers.contains(player.getUniqueId())) continue;
            player.playSound(player.getEyeLocation(),
                             Sound.UI_TOAST_CHALLENGE_COMPLETE,
                             SoundCategory.MASTER,
                             0.5f, 2.0f);
            for (ItemStack drop : player.getInventory().addItem(reward.create()).values()) {
                player.getWorld().dropItem(player.getEyeLocation(), drop);
            }
        }
    }

    void setAttr(Mob entity, Attribute attribute, double value) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst == null) return;
        inst.setBaseValue(value);
    }

    void prep(@NonNull Mob entity) {
        double health = 1000.0;
        setAttr(entity, Attribute.GENERIC_MAX_HEALTH, health);
        entity.setHealth(health);
        setAttr(entity, Attribute.GENERIC_ATTACK_DAMAGE, 10.0);
        setAttr(entity, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
        setAttr(entity, Attribute.GENERIC_MOVEMENT_SPEED, 0.25);
        setAttr(entity, Attribute.GENERIC_ARMOR, 16.0); // dia=20
        setAttr(entity, Attribute.GENERIC_ARMOR_TOUGHNESS, 2.0); // dia=8
        entity.setPersistent(false);
        EntityMarker.setId(entity, "raid:boss");
        EntityEquipment eq = entity.getEquipment();
        eq.setHelmetDropChance(0.0f);
        eq.setChestplateDropChance(0.0f);
        eq.setLeggingsDropChance(0.0f);
        eq.setBootsDropChance(0.0f);
        eq.setItemInMainHandDropChance(0.0f);
        eq.setItemInOffHandDropChance(0.0f);
        entity.setCustomName("" + ChatColor.DARK_RED + ChatColor.BOLD
                          + boss.type.displayName);
        entity.setCustomNameVisible(true);
    }

    void tickAdds(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks > 200) {
            phaseComplete = true;
            return;
        }
        if ((phaseTicks % 20) == 0) {
            mob.getWorld().spawnParticle(Particle.FLAME,
                                         mob.getEyeLocation(),
                                         8,
                                         0.5, 0.5, 0.5,
                                         0.0);
        }
        if (adds.size() > ADDS_LIMIT) return;
        switch (boss.type) {
        case DECAYED:
            if (phaseTicks == 40
                || phaseTicks == 60
                || phaseTicks == 80) {
                adds.add(mob.getWorld().spawn(mob.getLocation(),
                                              WitherSkeleton.class, this::prepAdd));
            }
            break;
        case FORGOTTEN:
            if (phaseTicks == 20
                || phaseTicks == 40
                || phaseTicks == 60
                || phaseTicks == 80
                || phaseTicks == 100) {
                Location loc = mob.getLocation()
                    .add(instance.plugin.rnd() * 8.0,
                         instance.plugin.rnd() * 8.0,
                         instance.plugin.rnd() * 8.0);
                adds.add(mob.getWorld().spawn(loc, Vex.class, this::prepVex));
            }
            break;
        case SKELLINGTON:
            if (phaseTicks > 0 && phaseTicks % 15 == 0) {
                adds.add(mob.getWorld().spawn(mob.getLocation(),
                                              Skeleton.class, this::prepAdd));
            }
            break;
        case DEEP_FEAR:
            long guardians = adds.stream().filter(m -> m instanceof Guardian).count();
            if (guardians < 2) {
                if (phaseTicks == 30) {
                    adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
                                                  Guardian.class, this::prepAdd));
                }
            }
            if (phaseTicks > 0 && phaseTicks % 20 == 0) {
                long drowned = adds.stream().filter(m -> m instanceof Drowned).count();
                if (drowned < 5) {
                    ItemStack trident = new ItemBuilder(Material.TRIDENT).create();
                    Drowned d = instance.world
                        .spawn(mob.getEyeLocation(),
                               Drowned.class, e -> {
                                   e.getEquipment().setItemInMainHand(trident);
                                   e.setHealth(2.0);
                                   prepAdd(e);
                               });
                    adds.add(d);
                }
            }
            if (phaseTicks > 0 && phaseTicks % 20 == 10) {
                adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
                                              PufferFish.class, this::prepAdd));
            }
            break;
        case LAVA_LORD:
            if (phaseTicks > 0 && phaseTicks % 20 == 0) {
                adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
                                              Blaze.class, this::prepAdd));
            }
            break;
        case ICE_GOLEM:
            if (phaseTicks > 0 && phaseTicks % 10 == 0) {
                int num = instance.plugin.random.nextInt(10);
                if (num == 0) {
                    adds.add(mob.getWorld().spawn(mob.getLocation(),
                                                  Creeper.class, this::prepAdd));
                } else if (num < 4) {
                    adds.add(mob.getWorld().spawn(mob.getLocation(),
                                                  Drowned.class, this::prepAdd));
                } else {
                    adds.add(mob.getWorld().spawn(mob.getLocation(),
                                                  Stray.class, this::prepAdd));
                }
            }
            break;
        case ICEKELLY:
            if (phaseTicks > 0 && phaseTicks % 20 == 0) {
                adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
                                              Vex.class, this::prepVex));
            }
            break;
        case SNOBEAR:
            if (phaseTicks > 0 && phaseTicks % 10 == 0) {
                adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
                                              Blaze.class, this::prepAdd));
            }
            break;
        case QUEEN_BEE:
            if (phaseTicks > 0 && phaseTicks % 5 == 0) {
                adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
                                              Bee.class, e -> {
                                                  e.setAnger(72000);
                                                  prepAdd(e);
                                              }));
            }
            break;
        case HEINOUS_HEN:
            if (phaseTicks > 0 && phaseTicks % 5 == 0) {
                adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
                                              Bee.class, e -> {
                                                  e.setAnger(72000);
                                                  prepAdd(e);
                                                  e.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(12.0f);
                                              }));
                adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
                                              Rabbit.class, e -> {
                                                  e.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
                                                  e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0f);
                                                  e.setHealth(20.0f);
                                              }));
            }
            break;
        case SPECTER:
            if (phaseTicks > 0 && phaseTicks % 20 == 0) {
                adds.add(mob.getWorld().spawn(mob.getEyeLocation(),
                                              Blaze.class, this::prepAdd));
            }
            break;
        default: break;
        }
    }

    void prepAdd(@NonNull Mob e) {
        e.setPersistent(false);
        e.setRemoveWhenFarAway(true);
        EntityEquipment eq = mob.getEquipment();
        if (eq != null) {
            eq.setHelmetDropChance(0.0f);
            eq.setChestplateDropChance(0.0f);
            eq.setLeggingsDropChance(0.0f);
            eq.setBootsDropChance(0.0f);
            eq.setItemInMainHandDropChance(0.0f);
            eq.setItemInOffHandDropChance(0.0f);
        }
    }

    void prepVex(@NonNull Vex e) {
        EntityEquipment eq = e.getEquipment();
        eq.setItemInMainHand(new ItemBuilder(Material.IRON_SWORD).create());
        prepAdd(e);
    }

    void immobile() {
        backupSpeed = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
        mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
        Entity vehicle = mob.getVehicle();
        if (vehicle instanceof Mob) {
            Mob veh = (Mob) vehicle;
            mountBackupSpeed = veh.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
            veh.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
        }
    }

    void mobile() {
        mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(backupSpeed);
        Entity vehicle = mob.getVehicle();
        if (vehicle instanceof Mob) {
            Mob veh = (Mob) vehicle;
            veh.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(mountBackupSpeed);
        }
    }

    void tickFirework(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks == 0) {
            immobile();
        } else if (phaseTicks == 40 || phaseTicks == 80) {
            Location eye = mob.getEyeLocation();
            for (Player player : players) {
                Location loc = player.getLocation();
                if (loc.distance(eye) > 64) continue;
                if (!mob.hasLineOfSight(player)) continue;
                Vector vec = loc.subtract(eye).toVector()
                    .normalize();
                Location from = eye.clone().add(vec);
                mob.getWorld().spawn(from, Firework.class, fw -> {
                        fw.setVelocity(vec.multiply(2.0));
                        FireworkMeta meta = fw.getFireworkMeta();
                        for (int i = 0; i < 2; i += 1) {
                            FireworkEffect.Builder builder = FireworkEffect.builder();
                            builder.with(FireworkEffect.Type.BALL)
                                .withColor(Color.BLACK).withTrail();
                            builder.with(FireworkEffect.Type.BALL)
                                .withColor(Color.RED).withTrail();
                            builder.with(FireworkEffect.Type.BALL)
                                .withColor(Color.ORANGE).withTrail();
                            builder.with(FireworkEffect.Type.BALL)
                                .withColor(Color.YELLOW).withTrail();
                            meta.addEffect(builder.build());
                        }
                        fw.setFireworkMeta(meta);
                    });
            }
        } else if (phaseTicks >= 100) {
            mobile();
            phaseComplete = true;
        }
    }

    void tickPull(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks == 0) {
            immobile();
        }
        if (phaseTicks >= 60) {
            phaseComplete = true;
        }
        if (phaseTicks == 60) {
            mobile();
            Location eye = mob.getEyeLocation();
            for (Player player : players) {
                Location loc = player.getLocation();
                if (loc.distance(eye) < 8) continue;
                Vector vec = eye.subtract(loc).toVector().normalize().multiply(3.0);
                player.setVelocity(vec);
            }
        }
    }

    void tickPush(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks == 0) {
            immobile();
        }
        if (phaseTicks >= 60) {
            phaseComplete = true;
        }
        if (phaseTicks == 60) {
            mobile();
            Location eye = mob.getEyeLocation();
            for (Player player : players) {
                Location loc = player.getLocation();
                if (loc.distance(eye) < 8) continue;
                Vector vec = loc.subtract(eye).toVector().setY(0)
                    .normalize().multiply(2.0).setY(1.0);
                player.setVelocity(vec);
            }
        }
    }

    void tickFireball(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks > 200) {
            phaseComplete = true;
            return;
        }
        if (phaseTicks % 10 == 0) {
            Location eye = mob.getEyeLocation();
            for (Player player : players) {
                if (!mob.hasLineOfSight(player)) continue;
                Location loc = player.getEyeLocation();
                Vector vec = loc.subtract(eye).toVector().normalize().multiply(1.5);
                mob.launchProjectile(LargeFireball.class, vec);
            }
        }
    }

    void tickPotion(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks >= 100) {
            phaseComplete = true;
            return;
        }
        if (phaseTicks == 20) {
            Location eye = mob.getEyeLocation();
            for (Player player : players) {
                if (!mob.hasLineOfSight(player)) continue;
                Location loc = player.getEyeLocation();
                Vector vec = loc.subtract(eye).toVector().normalize().multiply(2.0);
                ThrownPotion potion = mob.launchProjectile(ThrownPotion.class, vec);
                if (instance.plugin.random.nextBoolean()) {
                    potion.setItem(new ItemBuilder(Material.SPLASH_POTION)
                                   .basePotion(PotionType.POISON, false, false)
                                   .create());
                } else {
                    potion.setItem(new ItemBuilder(Material.SPLASH_POTION)
                                   .customEffect(PotionEffectType.CONFUSION, 100, 0)
                                   .create());
                }
            }
        }
    }

    void tickArrows(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks == 0) {
            invulnerable = true;
            immobile();
        } else if (phaseTicks >= 200) {
            invulnerable = false;
            mobile();
            phaseComplete = true;
            return;
        }
        if (phaseTicks == 0) {
            instance.world.spawnParticle(Particle.FLASH,
                                         mob.getEyeLocation(),
                                         5,
                                         0.5, 0.5, 0.5,
                                         0.0);
        }
        if (phaseTicks < 20) {
            return; // Grace period
        }
        players.removeIf(p -> !mob.hasLineOfSight(p));
        if (players.isEmpty()) return;
        Player target = players.get(instance.plugin.random.nextInt(players.size()));
        Vector velo = target.getEyeLocation()
            .subtract(mob.getEyeLocation())
            .toVector().normalize()
            .add(new Vector(instance.plugin.rnd() * 0.1,
                            instance.plugin.random.nextDouble() * 0.2,
                            instance.plugin.rnd() * 0.1))
            .multiply(2.0);
        Arrow arrow = (Arrow) mob.launchProjectile(Arrow.class, velo);
        arrow.setDamage(5.0);
        arrow.setPierceLevel(3);
        arrow.setPersistent(false);
        instance.arrows.add(arrow);
        if (boss.type == Boss.Type.SKELLINGTON) {
            arrow.setFireTicks(6000);
            switch (instance.plugin.random.nextInt(4)) {
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
        } else if (boss.type == Boss.Type.ICEKELLY) {
            switch (instance.plugin.random.nextInt(2)) {
            case 0:
                arrow.setBasePotionData(new PotionData(PotionType.SLOWNESS, false, true));
                break;
            case 1:
                arrow.setBasePotionData(new PotionData(PotionType.WEAKNESS, true, false));
                break;
            default: break;
            }
        }
        instance.world
            .playSound(mob.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.2f);
    }

    void tickTridents(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks == 0) {
            invulnerable = true;
            immobile();
        } else if (phaseTicks >= 200) {
            invulnerable = false;
            mobile();
            phaseComplete = true;
            return;
        }
        if (phaseTicks < 20) return; // Grace period
        players.removeIf(p -> !mob.hasLineOfSight(p));
        if (players.isEmpty()) return;
        Player target = players.get(instance.plugin.random.nextInt(players.size()));
        Vector velo = target.getEyeLocation()
            .subtract(mob.getEyeLocation())
            .toVector().normalize().multiply(2.0);
        Trident tri = (Trident) mob.launchProjectile(Trident.class, velo);
        tri.setPickupStatus(Trident.PickupStatus.DISALLOWED);
    }

    void tickMount(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks >= 100) {
            phaseComplete = true;
            return;
        } else if (phaseTicks == 20) {
            if (mob.getVehicle() != null) return;
            Mob mount = mob.getWorld().spawn(mob.getLocation(), Spider.class, this::prepAdd);
            double health = 200.0;
            setAttr(mount, Attribute.GENERIC_MAX_HEALTH, health);
            mount.setHealth(health);
            adds.add(mount);
            mount.addPassenger(mob);
        }
    }

    void tickDialogue(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks >= 100) {
            phaseComplete = true;
            return;
        }
        if (phaseTicks == 0) {
            if (dialogue == null) dialogue = getDialogue();
            if (dialogue.isEmpty()) return;
            if (dialogueIndex >= dialogue.size()) dialogueIndex = 0;
            String dia = "" + ChatColor.DARK_RED + boss.type.displayName
                + ChatColor.WHITE + ": " + ChatColor.YELLOW + ChatColor.ITALIC
                + dialogue.get(dialogueIndex);
            dialogueIndex += 1;
            for (Player player : players) {
                player.sendMessage(dia);
            }
        }
    }

    void onBossDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent edbee =
                (EntityDamageByEntityEvent) event;
            Player damager = Util.getPlayerDamager(edbee.getDamager());
            if (damager != null) {
                damagers.add(damager.getUniqueId());
            }
            if (boss.type == Boss.Type.DEEP_FEAR && shield
                && edbee.getDamager() instanceof Trident) {
                World w = mob.getWorld();
                w.spawnParticle(Particle.END_ROD, mob.getLocation(), 64, 1.0, 1.0, 1.0, 1.0);
                w.playSound(mob.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH_LAND, 2.0f, 0.5f);
                shield = false;
                shieldCooldown = 100;
            } else if (boss.type == Boss.Type.LAVA_LORD) {
                tookDamage = true;
                if (adds.size() < ADDS_LIMIT && instance.plugin.random.nextBoolean()) {
                    World w = mob.getWorld();
                    Location loc = mob.getEyeLocation();
                    MagmaCube mc = (MagmaCube) mob;
                    final int size = mc.getSize() <= 1
                        ? 1
                        : 1 + instance.plugin.random.nextInt(mc.getSize());
                    MagmaCube add = w.spawn(loc, MagmaCube.class, e -> {
                            e.setSize(size);
                            e.setWander(true);
                            prepAdd(e);
                        });
                    adds.add(add);
                }
            }
        }
        if (invulnerable || shield) {
            event.setCancelled(true);
            return;
        }
        switch (event.getCause()) {
        case CRAMMING:
        case DROWNING:
        case DRYOUT:
        case FALL:
        case FIRE:
        case FIRE_TICK:
        case HOT_FLOOR:
        case LAVA:
        case LIGHTNING:
        case MELTING:
        case STARVATION:
        case SUFFOCATION:
            event.setCancelled(true);
        default:
            break;
        }
    }

    void tickDeepFear(Wave wave, List<Player> players) {
        Location eye = mob.getEyeLocation();
        if (this.shield) {
            // Display shield
            for (int i = 0; i < 9; i += 1) {
                Vector vec = new Vector(instance.plugin.random.nextDouble() - 0.5,
                                        instance.plugin.random.nextDouble() - 0.5,
                                        instance.plugin.random.nextDouble() - 0.5)
                    .normalize()
                    .multiply(2.0);
                Location loc = eye.clone().add(vec);
                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.0, 0.0, 0.0, 0.0);
            }
        } else {
            if (shieldCooldown <= 0) {
                eye.getWorld().playSound(eye,
                                         Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 0.5f);
                shield = true;
            } else {
                shieldCooldown -= 1;
            }
        }
    }

    void tickLavaLord(Wave wave, List<Player> players) {
        if (!tookDamage) return;
        tookDamage = false;
        MagmaCube mc = (MagmaCube) mob;
        int health = (int) mc.getHealth();
        int damage = maxHealth - health;
        final int maxSize = 16;
        int size = (damage * maxSize) / maxHealth;
        if (size < 1) size = 1;
        if (size == mc.getSize()) return;
        mc.setSize(size);
        setAttr(mc, Attribute.GENERIC_MAX_HEALTH, (double) maxHealth);
        mc.setHealth(health);
    }

    void tickLlamaSpit(Wave wave, List<Player> players) {
        if (phaseTicks > 200) {
            phaseComplete = true;
            return;
        }
        Player target = players.get(instance.plugin.random.nextInt(players.size()));
        Vector velo = target.getEyeLocation()
            .subtract(mob.getEyeLocation())
            .toVector().normalize()
            .add(new Vector(instance.plugin.rnd() * 0.1,
                            instance.plugin.random.nextDouble() * 0.2,
                            instance.plugin.rnd() * 0.1))
            .multiply(2.0);
        mob.launchProjectile(LlamaSpit.class, velo);
    }

    void tickWarp(Wave wave, List<Player> players) {
        Location center = wave.place.toLocation(instance.world);
        Location loc = mob.getLocation();
        if (phaseTicks == 0) {
            if (center.distanceSquared(loc) > MAX_DISTANCE * MAX_DISTANCE) {
                phaseComplete = true;
                mob.teleport(center);
            }
        } else if (phaseTicks > 100) {
            phaseComplete = true;
            return;
        }
        Location to = loc.clone().add(instance.plugin.rnd() * 8.0,
                                      instance.plugin.rnd() * 4.0,
                                      instance.plugin.rnd() * 8.0);
        if (to.distanceSquared(center) > MAX_DISTANCE * MAX_DISTANCE) return;
        if (to.getY() < 5.0 && to.getY() > 250.0) return;
        if (!to.getBlock().getRelative(0, -1, 0).getType().isSolid()) return;
        BoundingBox bb = mob.getBoundingBox().shift(to.clone().subtract(loc));
        World w = to.getWorld();
        final int ax = (int) Math.floor(bb.getMinX());
        final int ay = (int) Math.floor(bb.getMinY());
        final int az = (int) Math.floor(bb.getMinZ());
        final int bx = (int) Math.floor(bb.getMaxX());
        final int by = (int) Math.floor(bb.getMaxY());
        final int bz = (int) Math.floor(bb.getMaxZ());
        for (int y = ay; y <= by; y += 1) {
            for (int z = az; z <= bz; z += 1) {
                for (int x = ax; x <= bx; x += 1) {
                    if (!instance.world.getBlockAt(x, y, z).isEmpty()) return;
                }
            }
        }
        mob.teleport(to);
        phaseComplete = true;
    }

    void tickLightningSingle(Wave wave, List<Player> players) {
        if (phaseTicks > 400) {
            phaseComplete = true;
            lightningTarget = null;
            return;
        } else if (phaseTicks == 0) {
            lightningTarget = null;
            instance.world.spawnParticle(Particle.FLASH, mob.getEyeLocation(),
                                         5, 0.5, 0.5, 0.5, 0.0);
            return;
        } else if (phaseTicks < 30) {
            return;
        }
        long mod = phaseTicks % 30;
        if (mod == 10) {
            lightningSpots.clear();
            Player target;
            if (lightningTarget == null) {
                target = players
                    .get(instance.plugin.random.nextInt(players.size()));
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
            instance.world.spawnParticle(Particle.END_ROD, target.getEyeLocation(),
                                         6, 0.2, 0.2, 0.2, 0);
            lightningSpots.add(Place.of(target.getLocation()));
        } else if (mod == 29) {
            for (Place spot : lightningSpots) {
                instance.world.strikeLightning(spot.toLocation(instance.world));
            }
        }
    }

    void tickSlipIce(Wave wave, List<Player> players) {
        if (phaseTicks > 20) {
            phaseComplete = true;
            return;
        } else if (phaseTicks == 10) {
            for (Player player : players) {
                Vector vec = new Vector(instance.plugin.rnd(), 0, instance.plugin.rnd())
                    .normalize().multiply(1.5);
                player.setVelocity(player.getVelocity().add(vec));
            }
        }
    }

    void tickThrow(Wave wave, List<Player> players) {
        if (phaseTicks > 400) {
            phaseComplete = true;
            return;
        }
        Player target = instance.findTarget(mob, players);
        if (target == null) return;
        if (!mob.hasLineOfSight(target)) return;
        if (!target.isOnGround()) return;
        Vector vec = new Vector(instance.plugin.rnd(), 0, instance.plugin.rnd())
            .multiply(1.5)
            .setY(1.5);
        target.setVelocity(target.getVelocity().add(vec));
        target.playSound(target.getEyeLocation(),
                         Sound.ENTITY_POLAR_BEAR_WARNING,
                         SoundCategory.HOSTILE,
                         1.0f, 1.0f);
    }

    void tickLevitate(Wave wave, List<Player> players) {
        if (phaseTicks > 20) {
            phaseComplete = true;
            return;
        }
        PotionEffect effect = new PotionEffect(PotionEffectType.LEVITATION,
                                               200, 0);
        if (phaseTicks == 10) {
            for (Player player : players) {
                player.addPotionEffect(effect, false);
            }
        }
    }

    void tickEggLauncher(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks == 0) {
            invulnerable = true;
            immobile();
        } else if (phaseTicks >= 200) {
            invulnerable = false;
            mobile();
            phaseComplete = true;
            return;
        }
        if (phaseTicks == 0) {
            instance.world.spawnParticle(Particle.FLASH,
                                         mob.getEyeLocation(),
                                         5,
                                         0.5, 0.5, 0.5,
                                         0.0);
        }
        if (phaseTicks < 20) {
            return; // Grace period
        }
        players.removeIf(p -> !mob.hasLineOfSight(p));
        if (players.isEmpty()) return;
        Player target = players.get(instance.plugin.random.nextInt(players.size()));
        Vector velo = target.getEyeLocation()
            .subtract(mob.getEyeLocation())
            .toVector().normalize()
            .add(new Vector(instance.plugin.rnd() * 0.1,
                            instance.plugin.random.nextDouble() * 0.2,
                            instance.plugin.rnd() * 0.1))
            .multiply(2.0);
        Egg egg = (Egg) mob.launchProjectile(Egg.class, velo);
        instance.plugin.metadata.set(egg, EXPLOSIVE_EGG, true);
    }
}

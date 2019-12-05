package com.cavetale.raid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Vex;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
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
        TRIDENTS;
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

    void onTick(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phase == Phase.IDLE) {
            phases = getPhases();
            phaseIndex = 0;
            phase = phases.get(0);
            phaseTicks = 0;
        }
        adds.removeIf(e -> !e.isValid());
        switch (boss.type) {
        case DEEP_FEAR:
            tickDeepFear(wave, players);
            break;
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
                .asList(Phase.DIALOGUE, Phase.PAUSE, Phase.ARROWS, Phase.PAUSE, Phase.MOUNT);
        case DEEP_FEAR:
            return Arrays
                .asList(Phase.DIALOGUE, Phase.PAUSE, Phase.TRIDENTS, Phase.PAUSE, Phase.ADDS);
        default: return Arrays.asList(Phase.PAUSE);
        }
    }

    void tickAdds(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks > 100) {
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
                adds.add(mob.getWorld().spawn(loc, Vex.class, this::prepAdd));
            }
            break;
        default: break;
        }
    }

    void prepAdd(@NonNull Mob e) {
        e.setPersistent(false);
        e.setRemoveWhenFarAway(true);
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
        }
        if (phaseTicks < 20) return; // Grace period
        players.removeIf(p -> mob.hasLineOfSight(p));
        if (players.isEmpty()) return;
        Player target = players.get(instance.plugin.random.nextInt(players.size()));
        Vector velo = target.getEyeLocation()
            .subtract(mob.getEyeLocation())
            .toVector().normalize().multiply(2.0);
        Arrow arrow = (Arrow) mob.launchProjectile(Arrow.class, velo);
        arrow.setFireTicks(6000);
    }

    void tickTridents(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks == 0) {
            invulnerable = true;
            immobile();
        } else if (phaseTicks >= 200) {
            invulnerable = false;
            mobile();
        }
        if (phaseTicks < 20) return; // Grace period
        players.removeIf(p -> mob.hasLineOfSight(p));
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
        }
        if (mob.getVehicle() == null) return;
        Mob mount = mob.getWorld().spawn(mob.getLocation(), Spider.class, this::prepAdd);
        adds.add(mount);
        mount.addPassenger(mob);
    }

    void tickDialogue(@NonNull Wave wave, @NonNull List<Player> players) {
        if (phaseTicks >= 100) {
            phaseComplete = true;
            return;
        }
        if (phaseTicks == 0) {
            if (dialogue == null) dialogue = boss.getDialogue();
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
            if (boss.type == Boss.Type.DEEP_FEAR && shield
                && edbee.getDamager() instanceof Trident) {
                World w = mob.getWorld();
                w.spawnParticle(Particle.END_ROD, mob.getLocation(), 64, 1.0, 1.0, 1.0, 1.0);
                w.playSound(mob.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH_LAND, 2.0f, 0.5f);
                shield = false;
                shieldCooldown = 100;
                return;
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
}

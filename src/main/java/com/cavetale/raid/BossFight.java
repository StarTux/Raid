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
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Vex;
import org.bukkit.entity.WitherSkeleton;
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
    boolean phaseComplete = false;
    int maxHealth = 1000;
    final List<Mob> adds = new ArrayList<>();
    List<String> dialogue;
    int dialogueIndex;

    enum Phase {
        IDLE,
        FIREWORK,
        ADDS,
        DIALOGUE,
        PAUSE,
        PULL,
        PUSH,
        FIREBALL,
        POTION;
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
    }

    void mobile() {
        mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(backupSpeed);
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
                        for (int i = 0; i < 5; i += 1) {
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
}

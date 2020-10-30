package com.cavetale.raid.enemy;

import com.cavetale.raid.ability.AbilityPhases;
import com.cavetale.raid.ability.DialogueAbility;
import com.cavetale.raid.ability.PauseAbility;
import com.cavetale.raid.ability.SpawnAddsAbility;
import com.cavetale.raid.ability.VampirismAbility;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.util.Vector;

public final class SadisticVampireBoss extends LivingEnemy {
    @Getter private double maxHealth = 500;
    @Getter private double health = 500;
    @Getter private final String displayName = "" + ChatColor.DARK_RED + ChatColor.BOLD + "Sadistic Vampire";
    AbilityPhases batPhases;
    AbilityPhases evokerPhases;
    Phase phase = Phase.BAT;
    Bat bat;
    Evoker evoker;
    int phaseTicks = 0;
    int phaseDuration;
    Location location;
    Location targetLocation;
    Vector targetVector;
    private final int ticksPerBlock = 2;

    public SadisticVampireBoss(final Context context) {
        super(context);
    }

    enum Phase {
        BAT,
        TRAVEL,
        EVOKER,
        TRAVEL2,
        DEAD;
    }

    @Override
    public void spawn(Location loc) {
        location = loc;
        if (phase == Phase.BAT) {
            bat = loc.getWorld().spawn(loc, Bat.class, this::prep);
            living = bat;
            markLiving();
        } else if (phase == Phase.EVOKER) {
            evoker = loc.getWorld().spawn(loc, Evoker.class, this::prep);
            living = evoker;
            markLiving();
        }
        if (batPhases == null) {
            batPhases = new AbilityPhases();
            DialogueAbility batDialogue = batPhases.addAbility(new DialogueAbility(this, context));
            batDialogue.addDialogue("This feast comes but once a year!");
            batDialogue.addDialogue("Come and stick out your neck for me!");
            batDialogue.addDialogue("So many meals await before me!");
            VampirismAbility vampirism = batPhases.addAbility(new VampirismAbility(this, context));
            vampirism.setDamagePerTick(0.1);
            vampirism.setTicksPerHunger(10);
            vampirism.setSaturationPerTick(0.2f);
            vampirism.setWarmup(0);
            vampirism.setDuration(200);
            vampirism.setActive(200);
        }
        if (evokerPhases == null) {
            evokerPhases = new AbilityPhases();
            SpawnAddsAbility addsAbility = evokerPhases.addAbility(new SpawnAddsAbility(this, context));
            addsAbility.add(WitherSkeleton.class, 7, 1, Prep::add);
            addsAbility.setDuration(200);
            addsAbility.setInterval(40);
            PauseAbility pause = evokerPhases.addAbility(new PauseAbility(this, context));
            pause.setDuration(80);
        }
    }

    public void setPhase(Phase newPhase) {
        phase = newPhase;
        phaseTicks = 0;
        switch (phase) {
        case BAT:
            targetLocation = null;
            spawn(location);
            break;
        case TRAVEL: {
            if (bat != null) {
                bat.remove();
                bat = null;
            }
            living = null;
            List<Player> targets = context.getPlayers().stream()
                .filter(p -> Loc.isNearby(p.getLocation(), location, 32.0))
                .collect(Collectors.toList());
            if (targets.isEmpty()) {
                targetLocation = getSpawnLocation();
            } else {
                Collections.shuffle(targets);
                targetLocation = targets.get(0).getLocation();
            }
            targetVector = targetLocation.clone().subtract(location).toVector();
            phaseDuration = (int) targetVector.length() * ticksPerBlock;
            break;
        }
        case EVOKER:
            targetLocation = null;
            location.getWorld().createExplosion(location, 4f);
            spawn(location);
            break;
        case TRAVEL2:
            if (evoker != null) {
                for (int i = 0; i < 10; i += 1) {
                    spawnBat(evoker.getLocation().add(Math.random() * 0.5 - Math.random() * 0.5,
                                                      Math.random() * 1.0 - Math.random() * 1.0,
                                                      Math.random() * 0.5 - Math.random() * 0.5));
                    evoker.getWorld().spawnParticle(Particle.SMOKE_LARGE, evoker.getLocation().add(0, 1, 0), 16, 0, 0, 0, 0);
                }
                evoker.remove();
                evoker = null;
            }
            targetLocation = getSpawnLocation();
            for (int i = 0; i < 3; i += 1) {
                Location loc = targetLocation.clone().add(Math.random() * 8.0 - Math.random() * 8.0,
                                                          Math.random() * 8.0 - Math.random() * 8.0,
                                                          Math.random() * 8.0 - Math.random() * 8.0);
                if (loc.getBlock().isEmpty()) {
                    targetLocation = loc;
                    break;
                }
            }
            targetVector = targetLocation.toVector().subtract(location.toVector());
            phaseDuration = (int) targetVector.length() * ticksPerBlock;
            living = null;
            break;
        case DEAD:
        default:
            if (bat != null) {
                bat.remove();
                bat = null;
            }
            if (evoker != null) {
                evoker.remove();
                evoker = null;
            }
            location = null;
            targetLocation = null;
            break;
        }
    }

    @Override
    public void tick() { }

    public void tickBossFight() {
        switch (phase) {
        case BAT:
            if (!bat.isValid()) {
                spawn(location);
                if (!bat.isValid()) return;
            }
            location = bat.getLocation();
            health = bat.getHealth();
            if (health <= 0) {
                setPhase(Phase.DEAD);
                return;
            }
            if (!batPhases.tick()) {
                setPhase(Phase.TRAVEL);
                return;
            }
            if (phaseTicks % 20 == 0) {
                bat.getWorld().spawnParticle(Particle.FLAME, bat.getLocation(), 5, 0, 0, 0, 0);
            }
            break;
        case TRAVEL:
        case TRAVEL2:
            if (phaseTicks > phaseDuration) {
                location = targetLocation;
                targetLocation = null;
                setPhase(phase == Phase.TRAVEL ? Phase.EVOKER : Phase.BAT);
                return;
            }
            double progress = (double) phaseTicks / (double) phaseDuration;
            Location loc = location.clone().add(targetVector.clone().multiply(progress));
            loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 4, 0, 0, 0, 0);
            if (Math.random() < 0.1) {
                spawnBat(loc);
            }
            break;
        case EVOKER:
            if (!evoker.isValid()) {
                spawn(location);
                if (!evoker.isValid()) return;
            }
            location = evoker.getLocation();
            health = evoker.getHealth();
            if (health <= 0) {
                setPhase(Phase.DEAD);
                return;
            }
            if (!evokerPhases.tick()) {
                setPhase(Phase.TRAVEL2);
                return;
            }
            break;
        case DEAD:
        default:
            return;
        }
        phaseTicks += 1;
    }

    @Override
    public boolean isValid() {
        return (phase == Phase.TRAVEL || phase == Phase.TRAVEL2)
            || (living != null && living.isValid());
    }

    @Override
    public boolean isAlive() {
        return phase != Phase.DEAD;
    }

    @Override
    public boolean isDead() {
        return phase == Phase.DEAD;
    }

    @Override
    public void onRemove() {
        if (bat != null) {
            bat.remove();
            bat = null;
        }
        if (evoker != null) {
            evoker.remove();
            evoker = null;
        }
        location = null;
        targetLocation = null;
    }

    private void prep(Mob mob) {
        mob.setCustomName(displayName);
        mob.setCustomNameVisible(true);
        Prep.health(mob, health, maxHealth);
        Prep.boss(mob);
    }

    private void spawnBat(Location loc) {
        Bat add = loc.getWorld().spawn(loc, Bat.class, this::prepBat);
        if (add == null) return;
        context.registerTemporaryEntity(add);
        Bukkit.getScheduler().runTaskLater(context.getPlugin(), () -> add.remove(), 40L);
    }

    private void prepBat(Bat add) {
        add.setPersistent(false);
        add.setRemoveWhenFarAway(true);
    }
}

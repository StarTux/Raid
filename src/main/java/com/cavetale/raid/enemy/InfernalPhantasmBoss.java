package com.cavetale.raid.enemy;

import com.cavetale.raid.ability.AbilityPhases;
import com.cavetale.raid.ability.DialogueAbility;
import com.cavetale.raid.ability.FireballAbility;
import com.cavetale.raid.ability.PauseAbility;
import com.cavetale.raid.ability.SpawnAddsAbility;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Zoglin;

public final class InfernalPhantasmBoss extends LivingEnemy {
    @Getter private double maxHealth = 500;
    @Getter private double health = 500;
    @Getter private final String displayName = "" + ChatColor.DARK_RED + ChatColor.BOLD + "Infernal Phantasm";
    AbilityPhases phases;

    public InfernalPhantasmBoss(final Context context) {
        super(context);
    }

    public void spawn(Location loc) {
        living = loc.getWorld().spawn(loc, Blaze.class, this::prep);
        if (living == null) return;
        markLiving();
        if (phases == null) {
            phases = new AbilityPhases();
            DialogueAbility dialogue = phases.addAbility(new DialogueAbility(this, context));
            dialogue.addDialogue("Did you enjoy this Halloween night?");
            dialogue.addDialogue("Say farewell to this world!");
            dialogue.addDialogue("You will never escape from me!");
            dialogue.setDuration(100);
            FireballAbility fireballs = phases.addAbility(new FireballAbility(this, context));
            fireballs.setVelocity(2.5);
            fireballs.setLargeChance(0.5);
            fireballs.setDuration(200);
            fireballs.setInterval(10);
            phases.addAbility(new PauseAbility(this, context)).setDuration(60);
            SpawnAddsAbility adds = phases.addAbility(new SpawnAddsAbility(this, context));
            adds.setDuration(100);
            adds.setWarmup(20);
            adds.setInterval(20);
            adds.add(Zoglin.class, 8, 1, Prep::add);
            adds.add(PiglinBrute.class, 8, 1, this::prepPiglinBrute);
            adds.setSimultaneous(2);
        }
    }

    private void prep(Blaze blaze) {
        blaze.setCustomName(displayName);
        blaze.setCustomNameVisible(true);
        Prep.health(blaze, health, maxHealth);
        Prep.boss(blaze);
    }

    private void prepPiglinBrute(PiglinBrute brute) {
        brute.setBaby(false);
        brute.setImmuneToZombification(true);
        Prep.add(brute);
    }

    @Override
    public void tick() {
        phases.tick();
        health = living.getHealth();
    }

    @Override
    public void onRemove() {
        if (phases != null) {
            phases.end();
        }
    }
}

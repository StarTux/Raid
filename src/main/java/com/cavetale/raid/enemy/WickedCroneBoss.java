package com.cavetale.raid.enemy;

import com.cavetale.raid.ItemBuilder;
import com.cavetale.raid.ability.AbilityPhases;
import com.cavetale.raid.ability.ArrowStormAbility;
import com.cavetale.raid.ability.DialogueAbility;
import com.cavetale.raid.ability.PauseAbility;
import com.cavetale.raid.ability.SpawnAddsAbility;
import com.cavetale.raid.ability.SplashPotionAbility;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Illusioner;
import org.bukkit.entity.Witch;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public final class WickedCroneBoss extends LivingEnemy {
    @Getter private double maxHealth = 500;
    @Getter private double health = 500;
    @Getter private final String displayName = "" + ChatColor.DARK_RED + ChatColor.BOLD + "Wicked Crone";
    AbilityPhases phases;

    public WickedCroneBoss(final Context context) {
        super(context);
    }

    @Override
    public void spawn(Location loc) {
        living = loc.getWorld().spawn(loc, Witch.class, this::prep);
        if (living == null) return;
        markLiving();
        if (phases == null) {
            phases = new AbilityPhases();
            DialogueAbility dialogue = phases.addAbility(new DialogueAbility(this, context));
            dialogue.addDialogue("Nobody likes unwelcomed guests!");
            dialogue.addDialogue("What a bunch of pests!");
            dialogue.addDialogue("The ritual is nearly complete!");
            SplashPotionAbility splash = phases.addAbility(new SplashPotionAbility(this, context));
            splash.clearPotionItems();
            splash.addPotionItem(new ItemBuilder(Material.SPLASH_POTION).basePotion(PotionType.SLOWNESS, false, true).create());
            splash.addPotionItem(new ItemBuilder(Material.SPLASH_POTION).basePotion(PotionType.POISON, false, true).create());
            splash.addPotionItem(new ItemBuilder(Material.SPLASH_POTION).basePotion(PotionType.WEAKNESS, true, false).create());
            splash.addPotionItem(new ItemBuilder(Material.SPLASH_POTION).basePotion(PotionType.INSTANT_DAMAGE, false, true).create());
            splash.setDuration(200);
            splash.setInterval(20);
            PauseAbility pause = phases.addAbility(new PauseAbility(this, context));
            pause.setDuration(60);
            SpawnAddsAbility adds = phases.addAbility(new SpawnAddsAbility(this, context));
            adds.add(Illusioner.class, 3, 3, Prep::add);
            adds.setWarmup(20);
            adds.setDuration(200);
            adds.setInterval(40);
            ArrowStormAbility arrowStorm = phases.addAbility(new ArrowStormAbility(this, context));
            arrowStorm.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 300, 0, true, false, true));
            arrowStorm.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 300, 0, true, false, true));
            arrowStorm.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 300, 0, true, false, true));
            arrowStorm.addPotionEffect(new PotionEffect(PotionEffectType.HARM, 1, 0, true, false, true));
            arrowStorm.setDuration(200);
            arrowStorm.setInterval(10);
        }
    }

    private void prep(Witch witch) {
        witch.setCustomName(displayName);
        witch.setCustomNameVisible(true);
        Prep.health(witch, health, maxHealth);
        Prep.boss(witch);
    }

    @Override
    public void tick() {
        phases.tick();
        health = living.getHealth();
    }

    @Override
    public void onRemove() { }
}

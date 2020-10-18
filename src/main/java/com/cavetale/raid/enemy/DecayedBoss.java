package com.cavetale.raid.enemy;

import com.cavetale.raid.ItemBuilder;
import com.cavetale.raid.ability.AbilityPhases;
import com.cavetale.raid.ability.DialogueAbility;
import com.cavetale.raid.ability.FireworkAbility;
import com.cavetale.raid.ability.PauseAbility;
import com.cavetale.raid.ability.SpawnAddsAbility;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class DecayedBoss extends LivingEnemy {
    @Getter private double maxHealth = 500;
    @Getter private double health = 500;
    @Getter private final String displayName = "" + ChatColor.DARK_RED + ChatColor.BOLD + "The Decayed";
    private AbilityPhases phases;

    public DecayedBoss(final JavaPlugin plugin, final Context context) {
        super(plugin, context);
    }

    @Override
    public void spawn(Location location) {
        living = location.getWorld().spawn(location, WitherSkeleton.class, this::prep);
        markLiving();
        phases = new AbilityPhases();
        DialogueAbility dialogue = phases.addAbility(new DialogueAbility(this, context));
        dialogue.addDialogue("Your effort is in vain.");
        dialogue.addDialogue("The show's over with my next move.");
        dialogue.addDialogue("Welcome home.");
        FireworkAbility firework = phases.addAbility(new FireworkAbility(this, context));
        firework.setInterval(5);
        PauseAbility pause = phases.addAbility(new PauseAbility(this, context, 100));
        SpawnAddsAbility adds = phases.addAbility(new SpawnAddsAbility(this, context));
        adds.add(WitherSkeleton.class, 8, 1, this::prepAdd);
        phases.begin();
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        super.onDeath(event);
    }

    @Override
    public void onRemove() {
        if (phases != null) {
            phases.end();
        }
    }

    @Override
    public void tick() {
        phases.tick();
        health = living.getHealth();
    }

    private void prep(LivingEntity entity) {
        EntityEquipment eq = entity.getEquipment();
        eq.setHelmet(new ItemBuilder(Material.CARVED_PUMPKIN).create());
        eq.setItemInMainHand(new ItemBuilder(Material.DIAMOND_SWORD)
                             .ench(Enchantment.KNOCKBACK, 2)
                             .ench(Enchantment.DAMAGE_ALL, 5)
                             .removeDamage().create());
        entity.setCustomName(displayName);
        Prep.health(entity, health, maxHealth);
        Prep.boss(entity);
    }

    private void prepAdd(WitherSkeleton witherSkeleton) {
        Prep.disableEquipmentDrop(witherSkeleton);
        witherSkeleton.setPersistent(false);
        witherSkeleton.setRemoveWhenFarAway(true);
    }

    @Override
    public List<ItemStack> getDrops() {
        return Arrays.asList(new ItemStack(Material.WITHER_SKELETON_SKULL));
    }
}

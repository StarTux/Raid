package com.cavetale.raid;

import com.cavetale.core.editor.EditMenuAdapter;
import com.cavetale.core.editor.EditMenuItem;
import com.cavetale.enemy.Enemy;
import com.cavetale.enemy.EnemyType;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.font.Glyph;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * Configuration.
 * A mob to be spawned in a location.
 */
final class Spawn implements ShortInfo, EditMenuAdapter {
    @EditMenuItem(deletable = true)
    protected EntityType entity;
    @EditMenuItem(deletable = true)
    protected EnemyType enemy;
    @EditMenuItem(deletable = true)
    protected String entityType;
    protected Place place = new Place(0f, 0f, 0f, 0f, 0.0f);
    protected int amount = 1;
    @EditMenuItem(deletable = true)
    protected EntityType mount;
    @EditMenuItem(deletable = true)
    protected Material helmet;
    @EditMenuItem(deletable = true)
    protected Material chestplate;
    @EditMenuItem(deletable = true)
    protected Material leggings;
    @EditMenuItem(deletable = true)
    protected Material boots;
    @EditMenuItem(deletable = true)
    protected Material hand;
    protected boolean baby;
    protected boolean powered;
    @EditMenuItem(deletable = true)
    protected Map<Attribute, Double> attributes;
    protected double scaling = 0.25;
    private static final List<EntityType> ENTITY_TYPES = Stream.of(EntityType.values())
        .filter(e -> e != EntityType.UNKNOWN && Mob.class.isAssignableFrom(e.getEntityClass()))
        .collect(Collectors.toList());

    Spawn() { }

    Spawn(@NonNull final String entityType, @NonNull final Location location, final int amount) {
        this.entityType = entityType;
        this.place = Place.of(location);
        this.amount = amount;
    }

    public EnemyType getEnemyType() {
        if (enemy != null) return enemy;
        if (entityType == null) return null;
        try {
            return EnemyType.valueOf(entityType.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    public EntityType getBukkitType() {
        if (entity != null) return entity;
        if (entityType == null) return null;
        try {
            return EntityType.valueOf(entityType.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Enemy createEnemy(Instance instance) {
        EnemyType enemyType = getEnemyType();
        if (enemyType != null) return enemyType.create(instance);
        EntityType bukkitType = getBukkitType();
        if (bukkitType != null && Mob.class.isAssignableFrom(bukkitType.getEntityClass())) {
            return new SpawnEnemy(instance, (Class<? extends Mob>) bukkitType.getEntityClass(), this);
        }
        return null;
    }

    static String camel(String in) {
        return Stream.of(in.split("_"))
            .map(i -> i.substring(0, 1) + i.substring(1).toLowerCase())
            .collect(Collectors.joining(""));
    }

    @Override
    public String getShortInfo() {
        StringBuilder sb = new StringBuilder("" + entityType);
        if (place != null) {
            sb.append(" " + place.getShortInfo());
        }
        if (amount != 1) sb.append(" x" + amount);
        if (mount != null) sb.append(" mount=" + mount);
        if (helmet != null) sb.append(" helmet=" + helmet);
        if (chestplate != null) sb.append(" chestplate=" + chestplate);
        if (leggings != null) sb.append(" leggings=" + leggings);
        if (boots != null) sb.append(" boots=" + boots);
        if (attributes != null) {
            for (Map.Entry<Attribute, Double> entry : attributes.entrySet()) {
                sb.append(" " + camel(entry.getKey().name().replace("GENERIC_", "").replace("MAX_", ""))
                          + "=" + String.format("%.2f", entry.getValue()));
            }
        }
        if (scaling != 0) sb.append(" scaling=" + String.format("%.2f", scaling));
        return sb.toString();
    }

    public Map<Attribute, Double> getAttributes() {
        if (attributes == null) attributes = new EnumMap<>(Attribute.class);
        return attributes;
    }

    public void onSave() {
        if (attributes != null && attributes.isEmpty()) attributes = null;
    }

    @Override
    public ItemStack getMenuIcon() {
        ItemStack result;
        EnemyType enemyType = getEnemyType();
        if (enemyType != null) {
            Glyph glyph = Glyph.toGlyph(enemyType.toString().toLowerCase().charAt(0));
            return glyph != null
                ? glyph.mytems.createIcon()
                : Mytems.QUESTION_MARK.createIcon();
        }
        EntityType bukkitType = getBukkitType();
        if (bukkitType != null) {
            ItemStack egg = Bukkit.getItemFactory().getSpawnEgg(bukkitType);
            if (egg != null) return egg;
            Glyph glyph = Glyph.toGlyph(bukkitType.toString().toLowerCase().charAt(0));
            return glyph != null
                ? glyph.mytems.createIcon()
                : Mytems.QUESTION_MARK.createIcon();
        }
        return new ItemStack(Material.BARRIER);
    }

    @Override
    public List<Component> getTooltip() {
        EnemyType enemyType = getEnemyType();
        EntityType bukkitType = getBukkitType();
        if (enemyType == null && bukkitType == null) return List.of(text("?", DARK_GRAY));
        String displayName = enemyType != null ? enemyType.toString() : bukkitType.toString();
        Component sep = text(": ", DARK_GRAY);
        return List.of(text(amount + " " + displayName, WHITE),
                       join(separator(sep), text("Place", GRAY), text(place.getShortInfo(), WHITE)));
    }

    @Override
    public List<Object> getPossibleValues(String fieldName, int valueIndex) {
        switch (fieldName) {
        case "entity": return List.copyOf(ENTITY_TYPES);
        default: return null;
        }
    }

    @Override
    public Boolean validateValue(String fieldName, Object newValue, int valueIndex) {
        switch (fieldName) {
        case "amount": return newValue instanceof Integer integer && integer > 0;
        default: return null;
        }
    }
}

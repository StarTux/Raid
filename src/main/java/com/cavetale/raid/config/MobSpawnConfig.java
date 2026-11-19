package com.cavetale.raid.config;

import com.cavetale.raid.config.validate.ConfigValidation;
import com.cavetale.raid.config.validate.ValidationContext;
import java.io.Serializable;
import lombok.Data;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

@Data
public final class MobSpawnConfig implements Serializable, ConfigValidation {
    private LocationConfig location;
    private String mob;

    public EntityType resolveType() {
        final NamespacedKey key = mob == null ? null : NamespacedKey.fromString(mob);
        if (key == null || !key.getNamespace().equals("minecraft")) return null;
        final EntityType type = Registry.ENTITY_TYPE.get(key);
        return type != null && type.isSpawnable() && type.getEntityClass() != null
            && Mob.class.isAssignableFrom(type.getEntityClass()) ? type : null;
    }

    @Override
    public void validate(final ValidationContext context) {
        if (location == null) context.child("location").error("Spawn location is required");
        if (resolveType() == null) {
            context.child("mob").error(
                mob != null && mob.startsWith("raid:")
                ? "Custom raid mobs are not implemented yet"
                : "Unknown or unsupported mob: " + mob
            );
        }
    }

    @Override
    public void validateDeep(final ValidationContext context) {
        validate(context);
        if (location != null) location.validateDeep(context.child("location"));
    }
}

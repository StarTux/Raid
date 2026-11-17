package com.cavetale.raid.config;

import com.cavetale.raid.config.validate.ConfigValidation;
import com.cavetale.raid.config.validate.ValidationContext;
import java.io.Serializable;
import lombok.Data;

/**
 * The configuration of one location within a raid.
 *
 * The worldName refers to the name given to the world in RaidConfig.
 *
 * In a live Raid, RaidWorlds will manage all available worlds.  Call
 * RaidWorlds.getLocation(LocationConfig) to retrieve the location
 * from this.
 */
@Data
public final class LocationConfig implements Serializable, ConfigValidation {
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    @Override
    public void validate(final ValidationContext context) {
        context.child("worldName").validateWorldName(worldName);
        if (!Double.isFinite(x)) context.child("x").error("Must be finite");
        if (!Double.isFinite(y)) context.child("y").error("Must be finite");
        if (!Double.isFinite(z)) context.child("z").error("Must be finite");
        if (!Float.isFinite(yaw)) context.child("yaw").error("Must be finite");
        if (!Float.isFinite(pitch)) context.child("pitch").error("Must be finite");
    }
}

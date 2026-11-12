package com.cavetale.raid.config;

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
public final class LocationConfig implements Serializable {
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
}

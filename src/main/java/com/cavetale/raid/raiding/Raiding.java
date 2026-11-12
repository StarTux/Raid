package com.cavetale.raid.raiding;

import com.cavetale.raid.RaidPlugin;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The manager for all raiding related business.
 *
 * Raiding means running and playing a raid, hoping to finish and get
 * rewards.
 *
 * As opposed to editing or remote requesting, which lives in other
 * packages.
 */
@Getter
@RequiredArgsConstructor
public final class Raiding {
    private final RaidPlugin plugin;

    private final Raids raids = new Raids();
    private final Raiders raiders = new Raiders();

    public void enable() {
        raids.enable();
        raiders.enable();
    }

    public void disable() {
        raiders.disable();
        raids.disable();
    }

    public static Raiding getInstance() {
        return RaidPlugin.getInstance().getRaiding();
    }
}

package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveConfig.KillConfig;
import com.cavetale.raid.config.WaveType;

public final class KillWave extends AbstractWave {
    private final KillConfig config;

    public KillWave(final KillConfig config) {
        super(WaveType.KILL);
        this.config = config;
    }

    @Override
    public void enable() {
    }
}

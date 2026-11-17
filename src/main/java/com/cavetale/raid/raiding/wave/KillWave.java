package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveType;
import com.cavetale.raid.config.wave.KillWaveConfig;

public final class KillWave extends AbstractWave {
    private final KillWaveConfig config;

    public KillWave(final KillWaveConfig config) {
        super(WaveType.KILL);
        this.config = config;
    }

    @Override
    public void enable() {
    }
}

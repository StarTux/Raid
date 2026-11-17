package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveType;
import com.cavetale.raid.config.wave.WarmupWaveConfig;

public final class WarmupWave extends AbstractWave {
    private final WarmupWaveConfig config;

    public WarmupWave(final WarmupWaveConfig config) {
        super(WaveType.WARMUP);
        this.config = config;
    }

    @Override
    public void enable() {
    }
}

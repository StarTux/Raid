package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveConfig.WarmupConfig;
import com.cavetale.raid.config.WaveType;

public final class WarmupWave extends AbstractWave {
    private final WarmupConfig config;

    public WarmupWave(final WarmupConfig config) {
        super(WaveType.WARMUP);
        this.config = config;
    }

    @Override
    public void enable() {
    }
}

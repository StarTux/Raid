package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveType;
import com.cavetale.raid.config.wave.WinWaveConfig;

public final class WinWave extends AbstractWave {
    private final WinWaveConfig config;

    public WinWave(final WinWaveConfig config) {
        super(WaveType.WIN);
        this.config = config;
    }

    @Override
    public void enable() {
    }
}

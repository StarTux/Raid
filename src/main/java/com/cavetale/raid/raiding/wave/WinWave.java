package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveConfig.WinConfig;
import com.cavetale.raid.config.WaveType;

public final class WinWave extends AbstractWave {
    private final WinConfig config;

    public WinWave(final WinConfig config) {
        super(WaveType.WIN);
        this.config = config;
    }

    @Override
    public void enable() {
    }
}

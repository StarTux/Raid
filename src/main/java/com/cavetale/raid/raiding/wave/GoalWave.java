package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveType;
import com.cavetale.raid.config.wave.GoalWaveConfig;

public final class GoalWave extends AbstractWave {
    private final GoalWaveConfig config;

    public GoalWave(final GoalWaveConfig config) {
        super(WaveType.GOAL);
        this.config = config;
    }

    @Override
    public void enable() {
    }
}

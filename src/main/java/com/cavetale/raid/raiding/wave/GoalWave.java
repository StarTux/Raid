package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveConfig.GoalConfig;
import com.cavetale.raid.config.WaveType;

public final class GoalWave extends AbstractWave {
    private final GoalConfig config;

    public GoalWave(final GoalConfig config) {
        super(WaveType.GOAL);
        this.config = config;
    }

    @Override
    public void enable() {
    }
}

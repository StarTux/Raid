package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveConfig;
import com.cavetale.raid.config.WaveType;

public interface Wave {
    WaveType getType();

    String getLabel();

    void enable();

    default boolean isBlocking() {
        return true;
    }

    static Wave create(WaveConfig config) {
        return switch (config.getType()) {
        case WARMUP -> new WarmupWave(config.getWarmup());
        case KILL -> new KillWave(config.getKill());
        case GOAL -> new GoalWave(config.getGoal());
        case WIN -> new WinWave(config.getWin());
        };
    }
}

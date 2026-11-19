package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveConfig;
import com.cavetale.raid.config.WaveType;
import com.cavetale.raid.raiding.Raid;

public interface Wave {
    WaveType getType();

    String getLabel();

    /** Initialize when the raid enables, even if this wave is never reached. */
    void enable();

    /** Called when this wave becomes current. */
    default void activate() {
    }

    /** Called when this wave stops being current, including during shutdown. */
    default void deactivate() {
    }

    /** Release resources when the raid shuts down, whether activated or not. */
    default void disable() {
    }

    void tick();

    boolean isFinished();

    void setFinished(boolean finished);

    default boolean isBlocking() {
        return true;
    }

    static Wave create(final Raid raid, final WaveConfig config) {
        final AbstractWave wave = switch (config.getType()) {
        case WARMUP -> new WarmupWave(raid, config.getWarmup());
        case KILL -> new KillWave(raid, config.getKill());
        case GOAL -> new GoalWave(raid, config.getGoal());
        case WIN -> new WinWave(raid, config.getWin());
        };
        wave.setLabel(config.getLabel());
        return wave;
    }
}

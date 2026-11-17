package com.cavetale.raid.config;

import com.cavetale.raid.config.wave.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public enum WaveType {

    WARMUP(WaveConfig::getWarmup, WaveConfig::setWarmup, WarmupWaveConfig::new),
    KILL(WaveConfig::getKill, WaveConfig::setKill, KillWaveConfig::new),
    GOAL(WaveConfig::getGoal, WaveConfig::setGoal, GoalWaveConfig::new),
    WIN(WaveConfig::getWin, WaveConfig::setWin, WinWaveConfig::new),

    ;

    private final Function<WaveConfig, WaveTypeConfig> configGetter;

    <T extends WaveTypeConfig> WaveType(
        final Function<WaveConfig, T> getter,
        final BiConsumer<WaveConfig, T> setter,
        final Supplier<T> supplier
    ) {
        this.configGetter = waveConfig -> {
            final T current = getter.apply(waveConfig);
            if (current != null) return current;
            final T created = supplier.get();
            setter.accept(waveConfig, created);
            return created;
        };
    }

    public WaveTypeConfig getWaveTypeConfig(final WaveConfig waveConfig) {
        return configGetter.apply(waveConfig);
    }
}

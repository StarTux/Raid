package com.cavetale.raid.raiding;

import com.cavetale.raid.config.WaveConfig;
import com.cavetale.raid.config.WaveType;
import com.cavetale.raid.raiding.wave.Wave;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import static com.cavetale.raid.config.RaidConfig.MAIN_WAVE;

@Getter
public final class WaveProcessor {
    private final Raid raid;
    private final String name;
    private final List<Wave> waves = new ArrayList<>();
    private int waveIndex;
    private boolean active;
    private boolean waveStarted;

    public WaveProcessor(final Raid raid, final String name, final List<WaveConfig> configs) {
        this.raid = raid;
        this.name = name;
        for (WaveConfig config : configs) {
            waves.add(Wave.create(raid, config));
        }
    }

    public void enable() {
        waves.forEach(Wave::enable);
        if (isMainWave()) active = true;
    }

    public void disable() {
        active = false;
        if (waveStarted) {
            waves.get(waveIndex).deactivate();
            waveStarted = false;
        }
        waves.forEach(Wave::disable);
    }

    public void tick() {
        while (active && waveIndex < waves.size()) {
            final Wave currentWave = waves.get(waveIndex);
            if (!waveStarted) {
                waveStarted = true;
                if (currentWave.getType() == WaveType.WIN) raid.markVictory(this);
                currentWave.activate();
                if (!active) return;
            }
            final boolean blocking = currentWave.isBlocking();
            if (blocking) {
                currentWave.tick();
                if (!active || !currentWave.isFinished()) return;
            }
            currentWave.deactivate();
            waveIndex += 1;
            waveStarted = false;
            if (blocking) return;
        }
    }

    public Wave getActiveWave() {
        return active && waveStarted ? waves.get(waveIndex) : null;
    }

    /** Allow initial admission before the first player enters the raid. */
    public boolean isAdmittingPlayers() {
        return active && waveIndex < waves.size()
            && (waves.get(waveIndex).getType() == WaveType.WARMUP || !waves.get(waveIndex).isBlocking());
    }

    public boolean isMainWave() {
        return MAIN_WAVE.equals(name);
    }
}

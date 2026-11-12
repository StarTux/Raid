package com.cavetale.raid.raiding;

import com.cavetale.raid.config.WaveConfig;
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
    private Wave currentWave;
    private boolean active;

    public WaveProcessor(final Raid raid, final String name, final List<WaveConfig> configs) {
        this.raid = raid;
        this.name = name;
        for (WaveConfig config : configs) {
            waves.add(Wave.create(config));
        }
    }

    public void enable() {
        waves.forEach(Wave::enable);
        if (isMainWave()) active = true;
    }

    public void tick() {
        if (!active) return;
    }

    public boolean isMainWave() {
        return MAIN_WAVE.equals(name);
    }
}

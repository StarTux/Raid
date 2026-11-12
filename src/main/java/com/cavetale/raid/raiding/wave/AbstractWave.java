package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveType;
import lombok.Data;

@Data
public abstract class AbstractWave implements Wave {
    private final WaveType type;

    private String label;
}

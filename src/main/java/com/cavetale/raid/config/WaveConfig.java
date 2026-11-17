package com.cavetale.raid.config;

import com.cavetale.raid.config.wave.*;
import java.io.Serializable;
import lombok.Data;

@Data
public final class WaveConfig implements Serializable {
    private WaveType type;
    private String label;

    private WarmupWaveConfig warmup;
    private KillWaveConfig kill;
    private GoalWaveConfig goal;
    private WinWaveConfig win;

    public WaveTypeConfig getWaveTypeConfig() {
        if (type == null) {
            throw new NullPointerException("type must not be null!");
        }
        return type.getWaveTypeConfig(this);
    }
}

package com.cavetale.raid.config;

import java.io.Serializable;
import lombok.Data;

@Data
public final class WaveConfig implements Serializable {
    private WaveType type;

    private WarmupConfig warmup;
    private KillConfig kill;
    private GoalConfig goal;
    private WinConfig win;

    public static final class WarmupConfig {
    }

    public static final class KillConfig {
    }

    public static final class GoalConfig {
    }

    public static final class WinConfig {
    }
}

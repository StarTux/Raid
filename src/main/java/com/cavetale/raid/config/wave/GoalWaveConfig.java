package com.cavetale.raid.config.wave;

import com.cavetale.raid.config.LocationConfig;
import java.util.List;
import lombok.Data;

@Data
public final class GoalWaveConfig implements WaveTypeConfig {
    private LocationConfig goal;
    private int range = 8;

    @Override
    public List<WaveEditField<?>> getEditFields() {
        return List.of(
            new WaveEditField<>("goal", LocationConfig.class, this::getGoal, this::setGoal),
            new WaveEditField<>("range", Integer.class, this::getRange, this::setRange)
        );
    }
}

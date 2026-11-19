package com.cavetale.raid.config.wave;

import com.cavetale.raid.config.LocationConfig;
import com.cavetale.raid.config.validate.ValidationContext;
import java.util.List;
import lombok.Data;

@Data
public final class GoalWaveConfig implements WaveTypeConfig {
    private LocationConfig goal;
    private int range = 8;
    /** Goal duration in seconds. */
    private int duration = 60;

    @Override
    public List<WaveEditField<?>> getEditFields() {
        return List.of(
            new WaveEditField<>("goal", LocationConfig.class, this::getGoal, this::setGoal),
            new WaveEditField<>("range", Integer.class, this::getRange, this::setRange),
            new WaveEditField<>("duration", Integer.class, this::getDuration, this::setDuration)
        );
    }

    @Override
    public void validate(final ValidationContext context) {
        if (goal == null) context.child("goal").error("Goal location is required");
        if (duration <= 0) context.child("duration").error("Duration in seconds must be greater than zero");
        if (range <= 0) context.child("range").error("Must be greater than zero");
    }

    @Override
    public void validateDeep(final ValidationContext context) {
        validate(context);
        if (goal != null) goal.validateDeep(context.child("goal"));
    }
}

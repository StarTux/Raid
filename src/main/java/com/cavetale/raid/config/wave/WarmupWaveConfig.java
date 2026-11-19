package com.cavetale.raid.config.wave;

import com.cavetale.raid.config.LocationConfig;
import com.cavetale.raid.config.validate.ValidationContext;
import java.util.List;
import lombok.Data;

@Data
public final class WarmupWaveConfig implements WaveTypeConfig {
    private LocationConfig spawn;
    /** Warmup duration in seconds. */
    private int duration = 60;

    @Override
    public List<WaveEditField<?>> getEditFields() {
        return List.of(
            new WaveEditField<>("spawn", LocationConfig.class, this::getSpawn, this::setSpawn),
            new WaveEditField<>("duration", Integer.class, this::getDuration, this::setDuration)
        );
    }

    @Override
    public void validate(final ValidationContext context) {
        if (spawn == null) context.child("spawn").error("Spawn location is required");
        if (duration <= 0) context.child("duration").error("Duration in seconds must be greater than zero");
    }

    @Override
    public void validateDeep(final ValidationContext context) {
        validate(context);
        if (spawn != null) spawn.validateDeep(context.child("spawn"));
    }
}

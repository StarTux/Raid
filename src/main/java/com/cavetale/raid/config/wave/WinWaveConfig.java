package com.cavetale.raid.config.wave;

import com.cavetale.raid.config.LocationConfig;
import com.cavetale.raid.config.validate.ValidationContext;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public final class WinWaveConfig implements WaveTypeConfig {
    private int duration = 60;
    private List<LocationConfig> fireworks = new ArrayList<>();

    @Override
    public List<WaveEditField<?>> getEditFields() {
        return List.of(new WaveEditField<>("duration", Integer.class, this::getDuration, this::setDuration));
    }

    @Override
    public List<WaveEditList<?>> getEditLists() {
        return List.of(new WaveEditList<>("fireworks", LocationConfig.class, this::getFireworks, this::setFireworks));
    }

    @Override
    public void validate(final ValidationContext context) {
        if (duration <= 0) context.child("duration").error("Duration in seconds must be greater than zero");
        if (fireworks == null) context.child("fireworks").error("Firework list is required");
    }

    @Override
    public void validateDeep(final ValidationContext context) {
        validate(context);
        if (fireworks == null) return;
        for (int i = 0; i < fireworks.size(); i++) {
            final ValidationContext child = context.child("fireworks").index(i);
            final LocationConfig location = fireworks.get(i);
            if (location == null) child.error("Location is required");
            else location.validateDeep(child);
        }
    }
}

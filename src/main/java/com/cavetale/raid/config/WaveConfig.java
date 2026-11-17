package com.cavetale.raid.config;

import com.cavetale.raid.config.validate.ConfigValidation;
import com.cavetale.raid.config.validate.ValidationContext;
import com.cavetale.raid.config.wave.*;
import java.io.Serializable;
import java.util.Locale;
import lombok.Data;

@Data
public final class WaveConfig implements Serializable, ConfigValidation {
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

    @Override
    public void validate(final ValidationContext context) {
        if (type == null) {
            context.child("type").error("Wave type is required");
        } else if (type.getExistingWaveTypeConfig(this) == null) {
            context.child(type.name().toLowerCase(Locale.ROOT)).error("Wave configuration is required");
        }
    }

    @Override
    public void validateDeep(final ValidationContext context) {
        validate(context);
        if (type == null) return;
        final WaveTypeConfig payload = type.getExistingWaveTypeConfig(this);
        if (payload != null) payload.validateDeep(context.child(type.name().toLowerCase(Locale.ROOT)));
    }
}

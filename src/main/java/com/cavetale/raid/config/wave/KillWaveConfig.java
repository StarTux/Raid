package com.cavetale.raid.config.wave;

import com.cavetale.raid.config.MobSpawnConfig;
import com.cavetale.raid.config.validate.ValidationContext;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public final class KillWaveConfig implements WaveTypeConfig {
    private List<MobSpawnConfig> spawns = new ArrayList<>();

    @Override
    public List<WaveEditList<?>> getEditLists() {
        return List.of(new WaveEditList<>("spawns", MobSpawnConfig.class, this::getSpawns, this::setSpawns));
    }

    @Override
    public void validate(final ValidationContext context) {
        if (spawns == null) context.child("spawns").error("Spawn list is required");
    }

    @Override
    public void validateDeep(final ValidationContext context) {
        validate(context);
        if (spawns == null) return;
        for (int i = 0; i < spawns.size(); i++) {
            final ValidationContext child = context.child("spawns").index(i);
            final MobSpawnConfig spawn = spawns.get(i);
            if (spawn == null) child.error("Spawn is required");
            else spawn.validateDeep(child);
        }
    }
}

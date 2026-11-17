package com.cavetale.raid.config.validate;

import com.cavetale.raid.config.RaidConfig;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/** Child contexts share the root raid and issue collection. */
public final class ValidationContext {
    @Getter private final RaidConfig raidConfig;
    @Getter private final String path;
    private final List<ConfigIssue> issues;

    public ValidationContext(final RaidConfig raidConfig) {
        this(raidConfig, "", new ArrayList<>());
    }

    private ValidationContext(final RaidConfig raidConfig, final String path, final List<ConfigIssue> issues) {
        this.raidConfig = raidConfig;
        this.path = path;
        this.issues = issues;
    }

    public ValidationContext child(final String name) {
        return new ValidationContext(raidConfig, path.isEmpty() ? name : path + "." + name, issues);
    }

    public ValidationContext index(final int index) {
        return new ValidationContext(raidConfig, path + "[" + index + "]", issues);
    }

    public void error(final String message) {
        issues.add(new ConfigIssue(path.isEmpty() ? "raid" : path, message));
    }

    public List<ConfigIssue> getIssues() {
        return List.copyOf(issues);
    }

    /** Check a logical world reference against the root raid. */
    public void validateWorldName(final String worldName) {
        if (worldName == null || worldName.isBlank()) {
            error("World name is required");
        } else if (raidConfig.getBuildWorlds() == null || !raidConfig.getBuildWorlds().containsKey(worldName)) {
            error("Unknown world '" + worldName + "'");
        }
    }
}

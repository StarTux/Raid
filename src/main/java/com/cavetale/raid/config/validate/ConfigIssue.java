package com.cavetale.raid.config.validate;

import lombok.Value;

/** One configuration problem at a path relative to the raid. */
@Value
public class ConfigIssue {
    private final String path;
    private final String message;
}

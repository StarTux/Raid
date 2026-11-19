package com.cavetale.raid.config;

import com.cavetale.core.util.Json;
import java.io.File;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public final class RaidConfigFolder {
    private final String raidName;

    private File raidFolder;
    private File raidConfigFile;

    @Setter
    private RaidConfig raidConfig;

    public static RaidConfigFolder named(final String raidName) {
        return RaidConfigs.getInstance().getRaidConfigFolder(raidName);
    }

    public void enable() {
        raidFolder = new File(RaidConfigs.getInstance().getRaidFolder(), raidName);
        raidConfigFile = new File(raidFolder, "raid.json");
    }

    public void create() {
        raidFolder.mkdirs();
        saveRaidConfig();
    }

    public void load() {
        raidConfig = Json.load(raidConfigFile, RaidConfig.class);
        if (raidConfig != null) {
            raidConfig.setName(raidName);
        }
    }

    public void saveRaidConfig() {
        if (raidConfig == null) return;
        Json.save(raidConfigFile, raidConfig, true);
    }
}

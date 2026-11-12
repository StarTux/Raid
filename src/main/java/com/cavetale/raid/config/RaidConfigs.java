package com.cavetale.raid.config;

import com.cavetale.raid.RaidPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class RaidConfigs {
    private final RaidPlugin plugin;

    private File raidFolder;
    private final Map<String, RaidConfigFolder> raidNameMap = new HashMap<>();

    public static RaidConfigs getInstance() {
        return RaidPlugin.getInstance().getConfigs();
    }

    public void enable() {
        raidFolder = new File(plugin.getDataFolder(), "raids");
        raidFolder.mkdirs();
        load();
    }

    public void disable() {
        raidNameMap.clear();
    }

    public void load() {
        for (File file : raidFolder.listFiles()) {
            if (!file.isDirectory()) continue;
            final String raidName = file.getName();
            final RaidConfigFolder raidConfigFolder = new RaidConfigFolder(raidName);
            raidConfigFolder.enable();
            raidConfigFolder.load();
            raidNameMap.put(raidName, raidConfigFolder);
        }
    }

    public RaidConfigFolder getRaidConfigFolder(final String raidName) {
        return raidNameMap.get(raidName);
    }

    public RaidConfig getRaidConfig(final String raidName) {
        final RaidConfigFolder raidConfigFolder = getRaidConfigFolder(raidName);
        if (raidConfigFolder == null) return null;
        return raidConfigFolder.getRaidConfig();
    }

    public List<String> getAllRaidNames() {
        return List.copyOf(raidNameMap.keySet());
    }

    public void addRaidConfig(final String raidName, final RaidConfig raidConfig) {
        final RaidConfigFolder raidConfigFolder = new RaidConfigFolder(raidName);
        raidNameMap.put(raidName, raidConfigFolder);
        raidConfigFolder.enable();
        raidConfigFolder.setRaidConfig(raidConfig);
        raidConfigFolder.create();
    }
}

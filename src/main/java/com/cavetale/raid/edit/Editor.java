package com.cavetale.raid.edit;

import com.cavetale.core.command.CommandWarn;
import com.cavetale.raid.config.RaidConfig;
import com.cavetale.raid.config.WaveConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Getter
@Setter
@RequiredArgsConstructor
public final class Editor {
    private final UUID uuid;

    private boolean enabled;
    private boolean disabled;

    private String currentRaidName;
    private String currentWavesName;
    private int currentWaveIndex;

    public void enable() {
        enabled = true;
    }

    public void disable() {
        disabled = true;
        enabled = false;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public static Editor of(UUID uuid) {
        return Editors.getInstance().getEditor(uuid);
    }

    public static Editor of(Player player) {
        return of(player.getUniqueId());
    }

    public boolean hasCurrentRaid() {
        return getCurrentRaid() != null;
    }

    public RaidConfig getCurrentRaid() {
        if (currentRaidName == null) return null;
        return RaidConfig.named(currentRaidName);
    }

    public RaidConfig requireCurrentRaid() {
        final RaidConfig raidConfig = getCurrentRaid();
        if (raidConfig == null) {
            throw new CommandWarn("No raid selected!");
        }
        return raidConfig;
    }

    public List<WaveConfig> getCurrentWaveList() {
        final RaidConfig raidConfig = getCurrentRaid();
        if (raidConfig == null) return null;
        return raidConfig.getWaves().get(currentWavesName);
    }

    public List<WaveConfig> requireCurrentWaveList() {
        final RaidConfig raidConfig = requireCurrentRaid();
        final List<WaveConfig> waveList = raidConfig.getWaves().get(currentWavesName);
        if (waveList == null) {
            throw new CommandWarn("No wave list selected!");
        }
        return waveList;
    }

    public WaveConfig getCurrentWave() {
        final RaidConfig raidConfig = getCurrentRaid();
        if (raidConfig == null) return null;
        return raidConfig.findWaveConfig(currentWavesName, currentWaveIndex);
    }

    public WaveConfig requireCurrentWave() {
        final RaidConfig raidConfig = requireCurrentRaid();
        final WaveConfig waveConfig = raidConfig.findWaveConfig(currentWavesName, currentWaveIndex);
        if (waveConfig == null) {
            throw new CommandWarn("No wave selected!");
        }
        return waveConfig;
    }

    public void selectRaid(final String raidName) {
        currentRaidName = raidName;
        currentWavesName = "main";
        currentWaveIndex = 0;
    }

    /** For command completion. */
    public List<String> getCurrentWaveListNames() {
        final RaidConfig raid = getCurrentRaid();
        if (raid == null) return List.of();
        return List.copyOf(raid.getWaves().keySet());
    }

    /** For command completion. */
    public List<String> getWaveIndexNames(String wavesName) {
        final RaidConfig raid = getCurrentRaid();
        if (raid == null) return List.of();
        final List<WaveConfig> waveList = raid.getWaves().get(wavesName);
        if (waveList == null) return List.of();
        final List<String> result = new ArrayList<>();
        int nextIndex = 0;
        for (WaveConfig waveConfig : waveList) {
            result.add("" + nextIndex++);
            if (waveConfig.getLabel() != null) {
                result.add(waveConfig.getLabel());
            }
        }
        return result;
    }

    public void tick() { }
}

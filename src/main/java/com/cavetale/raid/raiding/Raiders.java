package com.cavetale.raid.raiding;

import com.cavetale.raid.RaidPlugin;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

/**
 * Raiding sessions manager.
 * A player session is called a Raider.
 */
@RequiredArgsConstructor
public final class Raiders {
    private final Map<UUID, Raider> uuidMap = new HashMap<>();

    public static Raiders getInstance() {
        return RaidPlugin.getInstance().getRaiding().getRaiders();
    }

    public void enable() {
        Bukkit.getScheduler().runTaskTimer(RaidPlugin.getInstance(), this::tick, 1L, 1L);
    }

    public void disable() {
        for (Raider raider : uuidMap.values()) {
            raider.disable();
        }
        uuidMap.clear();
    }

    public void tick() {
        for (Raider raider : uuidMap.values()) {
            raider.tick();
        }
    }

    public Raider getRaider(UUID uuid) {
        return uuidMap.computeIfAbsent(uuid, this::create);
    }

    public List<Raider> getRaiders(Raid raid) {
        return uuidMap.values().stream()
            .filter(raider -> raider.getCurrentRaid() == raid)
            .toList();
    }

    private Raider create(UUID uuid) {
        Raider raider = new Raider(uuid);
        raider.enable();
        return raider;
    }
}

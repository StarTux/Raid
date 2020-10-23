package com.cavetale.raid;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class Sessions {
    private final RaidPlugin plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public Sessions enable() {
        return this;
    }

    public void disable() {
        for (Session session : sessions.values()) {
            session.disable();
        }
        sessions.clear();
    }

    public Session of(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), unused -> new Session(plugin, player));
    }

    public void tick() {
        for (Session session : sessions.values()) {
            session.tick();
        }
    }

    public Session reset(Player player) {
        sessions.remove(player.getUniqueId());
        return of(player);
    }

    public void enter(Player player) {
        of(player);
    }

    public void exit(Player player) {
        // Don't remove session immediately
    }
}

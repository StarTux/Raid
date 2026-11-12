package com.cavetale.raid.edit;

import com.cavetale.raid.RaidPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

@RequiredArgsConstructor
public final class Editors {
    private final Map<UUID, Editor> uuidMap = new HashMap<>();

    public static Editors getInstance() {
        return Editing.getInstance().getEditors();
    }

    public void enable() {
        Bukkit.getScheduler().runTaskTimer(RaidPlugin.getInstance(), this::tick, 1L, 1L);
    }

    public void disable() {
        for (Editor editor : uuidMap.values()) {
            editor.disable();
        }
        uuidMap.clear();
    }

    public void tick() {
        for (Editor editor : uuidMap.values()) {
            editor.tick();
        }
    }

    public Editor getEditor(UUID uuid) {
        return uuidMap.computeIfAbsent(uuid, this::create);
    }

    private Editor create(UUID uuid) {
        Editor editor = new Editor(uuid);
        editor.enable();
        return editor;
    }
}

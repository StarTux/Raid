package com.cavetale.raid.edit;

import com.cavetale.raid.RaidPlugin;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The manager for editing raids on the Creative server.
 */
@Getter
@RequiredArgsConstructor
public final class Editing {
    private final RaidPlugin plugin;
    private final Editors editors = new Editors();

    public static Editing getInstance() {
        return RaidPlugin.getInstance().getEditing();
    }

    public void enable() {
        new RaidEditCommand(plugin).enable();
        editors.enable();
    }

    public void disable() {
    }
}

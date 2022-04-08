package com.cavetale.raid;

import com.cavetale.core.editor.EditMenuAdapter;
import com.cavetale.core.editor.EditMenuItem;
import com.cavetale.core.editor.EditMenuNode;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * JSONable structure.
 * Created and managed by RaidPlugin.
 */
@Getter
public final class Raid implements EditMenuAdapter {
    @EditMenuItem(settable = false,
                  description = "Internal use only")
    transient String worldName;
    List<Wave> waves = new ArrayList<>();
    @EditMenuItem(description = "Use & color codes")
    String displayName = "";

    public Raid() { }

    public Raid(@NonNull final String worldName) {
        this.worldName = worldName;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public void onSave() {
        for (Wave wave : waves) {
            wave.onSave();
        }
    }

    @Override
    public Object createNewValue(EditMenuNode node, String fieldName, int valueIndex) {
        switch (fieldName) {
        case "waves": {
            Wave wave = new Wave();
            wave.type = Wave.Type.GOAL;
            wave.place.load(node.getContext().getPlayer().getLocation());
            wave.setRadius(7.0);
            return wave;
        }
        default: return null;
        }
    }
}

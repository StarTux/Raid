package com.cavetale.raid.raiding;

import com.winthier.creative.BuildWorld;
import com.winthier.creative.file.Files;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;

/**
 * Just a pairing of BuildWorld and its local copy.
 */
@Getter
@RequiredArgsConstructor
public final class RaidWorld {

    /** The reference name of the world within the Raid. */
    private final String name;

    /** The original build world. */
    private final BuildWorld buildWorld;

    /** The local unique copy. */
    private final World localWorld;

    public void delete() {
        Files.deleteWorld(localWorld);
    }
}

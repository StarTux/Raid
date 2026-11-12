package com.cavetale.raid.raiding;

import com.cavetale.raid.config.LocationConfig;
import com.winthier.creative.BuildWorld;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * A convenient cache of all worlds of a raid.
 *
 * Raids are hosted in local copies with arbitary names, whereas the
 * configuration references by name.  Therefore, we need to be able to
 * map names to worlds easily.
 *
 * The _name_ is the given name based on the raid config.
 * The _path_ is the original path on the creative server.
 * The _world_ and _worldName_ are the local world copy.
 */
@RequiredArgsConstructor
public final class RaidWorlds {
    private final Raid raid;
    private final Map<String, RaidWorld> nameMap = new HashMap<>();

    public void load(final Map<String, String> pathMap, final Runnable callback) {
        pathMap.forEach(
            (name, path) -> {
                final BuildWorld buildWorld = BuildWorld.findWithPath(path);
                if (buildWorld == null) {
                    throw new IllegalStateException("BuildWorld not found: " + path);
                }
                buildWorld.makeLocalCopyAsync(
                    world -> {
                        raid.log("World loaded: '" + name + "' path:'" + buildWorld.getPath() + "' world:'" + world.getName() + "'");
                        final RaidWorld raidWorld = new RaidWorld(name, buildWorld, world);
                        nameMap.put(name, raidWorld);
                        if (nameMap.size() == pathMap.size()) {
                            callback.run();
                        }
                    }
                );
            }
        );
    }

    public void clear() {
        nameMap.values().forEach(RaidWorld::delete);
        nameMap.clear();
    }

    public World findWorld(String name) {
        final RaidWorld raidWorld = nameMap.get(name);
        if (raidWorld == null) throw new IllegalStateException("Unknown raid world: " + name);
        return raidWorld.getLocalWorld();
    }

    public Location getLocation(LocationConfig config) {
        return new Location(
            findWorld(config.getWorldName()),
            config.getX(),
            config.getY(),
            config.getZ(),
            config.getYaw(),
            config.getPitch()
        );
    }
}

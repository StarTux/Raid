package com.cavetale.raid.raiding;

import com.cavetale.raid.config.LocationConfig;
import com.winthier.creative.BuildWorld;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final Map<String, RaidWorld> nameMap = new LinkedHashMap<>();
    private int totalWorldCount;

    public boolean load(final Map<String, String> pathMap) {
        totalWorldCount = pathMap.size();
        final Map<String, BuildWorld> buildWorldMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : pathMap.entrySet()) {
            final String name = entry.getKey();
            final String path = entry.getValue();
            final BuildWorld buildWorld = BuildWorld.findWithPath(path);
            if (buildWorld == null) {
                raid.error("BuildWorld not found: " + path);
                return false;
            }
            buildWorldMap.put(name, buildWorld);
        }
        buildWorldMap.forEach((name, buildWorld) -> buildWorld.makeLocalCopyAsync(world -> worldDidLoad(name, buildWorld, world)));
        return true;
    }

    /**
     * The callback for load() when a single world finished loading.
     */
    private void worldDidLoad(final String name, final BuildWorld buildWorld, final World world) {
        if (world == null) {
            raid.error("World loading filed: '" + name + "' path:'" + buildWorld.getPath());
            raid.disable();
            return;
        }
        raid.log("World loaded: '" + name + "' path:'" + buildWorld.getPath() + "' world:'" + world.getName() + "'");
        final RaidWorld raidWorld = new RaidWorld(name, buildWorld, world);
        if (raid.isDisabled()) {
            raidWorld.delete();
            return;
        }
        nameMap.put(name, raidWorld);
        if (nameMap.size() == totalWorldCount) {
            raid.worldsDidLoad();
        }
    }

    public List<BuildWorld> getBuildWorlds() {
        return nameMap.values().stream().map(RaidWorld::getBuildWorld).toList();
    }

    public void clear() {
        nameMap.values().forEach(RaidWorld::delete);
        nameMap.clear();
    }

    public boolean containsWorld(final World world) {
        return nameMap.values().stream().anyMatch(raidWorld -> raidWorld.getLocalWorld().equals(world));
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

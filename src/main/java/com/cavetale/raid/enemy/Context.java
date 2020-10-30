package com.cavetale.raid.enemy;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Provide some context for an enemy and its abilities. A context
 * includes some contextual information, and callbacks. A context is
 * intended to be provided by the client plugin in order to integrate
 * enemies and abilities into the mode of play.
 */
public interface Context {
    JavaPlugin getPlugin();

    /**
     * List players eligible for combat. Maybe within range, maybe
     * not.
     */
    List<Player> getPlayers();

    World getWorld();

    /**
     * The spawn of the current battle, or the desired spawn location
     * for the enemy receiving this context.
     * NOT the world spawn.
     */
    Location getSpawnLocation();

    /**
     * Register a new enemy. Presumably it was spawned by the boss as
     * an add.
     */
    void registerNewEnemy(Enemy enemy);

    /**
     * Task the context with remembering a temporary entity. Such
     * entities may require removal once the current context
     * expires. Furthermore, keeping tabs on them may be desirable to
     * spawn in adds with a certain upper limit (see
     * SpawnAddsAbility).
     */
    void registerTemporaryEntity(Entity entity);

    boolean isTemporaryEntity(Entity entity);

    /**
     * Count previously registered, and still valid, temporary
     * entities.
     */
    int countTemporaryEntities(Class<? extends Entity> type);

    void onDeath(Enemy enemy);

    List<Enemy> getEnemies();
}

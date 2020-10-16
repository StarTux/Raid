package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Enemy;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface Context {
    /**
     * List players eligible for combat. Maybe within range, maybe
     * not.
     */
    List<Player> getPlayers();

    /**
     * The spawn of the current battle, or the desired spawn location
     * for the ability receiving this context.
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

    /**
     * Count previously registered, and still valid, temporary
     * entities.
     */
    int countTemporaryEntities(Class<? extends Entity> type);
}

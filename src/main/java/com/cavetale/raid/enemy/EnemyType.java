package com.cavetale.raid.enemy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Compile time list of all known enemies. Partially unimplemented
 * because of legacy stuff (see missing ctor). Might become obsolete
 * in the future.
 */
public enum EnemyType {
    // Halloween 2019
    DECAYED(DecayedBoss.class, DecayedBoss::new),
    FORGOTTEN(ForgottenBoss.class, ForgottenBoss::new),
    VENGEFUL(VengefulBoss.class, VengefulBoss::new),
    // Halloween 2018 (Legacy)
    // SKELLINGTON("Skellington"),
    // DEEP_FEAR("Deep Fear"),
    // LAVA_LORD("Lava Lord"),
    // Christmas 2019
    // FROSTWRECKER("Frostwrecker"),
    // ICE_GOLEM("Ice Golem"),
    // ICEKELLY("Icekelly"),
    // SNOBEAR("Snobear"),
    // QUEEN_BEE("Queen Bee"),
    // Easter 2020
    // HEINOUS_HEN("Heinous Hen"),
    // SPECTER("Specter"),
    // Halloween 2020
    VAMPIRE_BAT(VampireBat.class, VampireBat::new),
    HEAL_EVOKER(HealEvoker.class, HealEvoker::new),
    // Bosses
    SADISTIC_VAMPIRE(SadisticVampireBoss.class, SadisticVampireBoss::new),
    WICKED_CRONE(WickedCroneBoss.class, WickedCroneBoss::new),
    INFERNAL_PHANTASM(InfernalPhantasmBoss.class, InfernalPhantasmBoss::new);

    public final Class<? extends Enemy> type;
    private final Function<Context, Enemy> ctor;
    private static final Map<Class<? extends Enemy>, EnemyType> TYPEMAP = new HashMap<>();

    EnemyType(final Class<? extends Enemy> type, final Function<Context, Enemy> ctor) {
        this.type = type;
        this.ctor = ctor;
    }

    static {
        for (EnemyType enemyType : EnemyType.values()) {
            TYPEMAP.put(enemyType.type, enemyType);
        }
    }

    public Enemy create(Context context) {
        return ctor.apply(context);
    }

    public static EnemyType of(Enemy enemy) {
        return TYPEMAP.get(enemy.getClass());
    }
}

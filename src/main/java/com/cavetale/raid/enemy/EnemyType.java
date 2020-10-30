package com.cavetale.raid.enemy;

import java.util.function.Function;

/**
 * Compile time list of all known enemies. Partially unimplemented
 * because of legacy stuff (see missing ctor). Might become obsolete
 * in the future.
 */
public enum EnemyType {
    // Halloween 2019
    DECAYED(DecayedBoss::new),
    FORGOTTEN(ForgottenBoss::new),
    VENGEFUL(VengefulBoss::new),
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
    VAMPIRE_BAT(VampireBat::new),
    HEAL_EVOKER(HealEvoker::new),
    // Bosses
    SADISTIC_VAMPIRE(SadisticVampireBoss::new),
    WICKED_CRONE(WickedCroneBoss::new),
    INFERNAL_PHANTASM(InfernalPhantasmBoss::new);

    private final Function<Context, Enemy> ctor;

    EnemyType(final Function<Context, Enemy> ctor) {
        this.ctor = ctor;
    }

    public Enemy create(Context context) {
        return ctor.apply(context);
    }
}

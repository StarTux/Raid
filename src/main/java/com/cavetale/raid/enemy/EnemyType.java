package com.cavetale.raid.enemy;

import java.util.function.Function;

/**
 * Compile time list of all known enemies. Partially unimplemented
 * because of legacy stuff (see missing ctor). Might become obsolete
 * in the future.
 */
public enum EnemyType {
    // Halloween 2019
    DECAYED("The Decayed", DecayedBoss::new),
    FORGOTTEN("The Forgotten", ForgottenBoss::new),
    VENGEFUL("The Vengeful", VengefulBoss::new),
    // Halloween 2018 (Legacy)
    SKELLINGTON("Skellington"),
    DEEP_FEAR("Deep Fear"),
    LAVA_LORD("Lava Lord"),
    // Christmas 2019
    FROSTWRECKER("Frostwrecker"),
    ICE_GOLEM("Ice Golem"),
    ICEKELLY("Icekelly"),
    SNOBEAR("Snobear"),
    QUEEN_BEE("Queen Bee"),
    // Easter 2020
    HEINOUS_HEN("Heinous Hen"),
    SPECTER("Specter"),
    // Halloween 2020
    VAMPIRE_BAT("Vampire Bat", VampireBat::new),
    HEAL_EVOKER("Heal Evoker", HealEvoker::new);

    public final String displayName;
    private final Function<Context, Enemy> ctor;

    EnemyType(final String displayName, final Function<Context, Enemy> ctor) {
        this.displayName = displayName;
        this.ctor = ctor;
    }

    EnemyType(final String displayName) {
        this(displayName, DecayedBoss::new);
    }

    public Enemy create(Context context) {
        return ctor.apply(context);
    }
}

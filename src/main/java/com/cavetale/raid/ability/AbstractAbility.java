package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Enemy;
import lombok.Getter;
import lombok.Setter;

/**
 * Implementors may use all the protected members, usually read-only.
 * Enemy and context must be provided to the constructor. Duration can
 * be configured and is to be interpreted by an implementor.
 *
 * `onTick` is only called if the warmup is passed and the duration is
 * not exceeded.
 */
public abstract class AbstractAbility implements Ability {
    @Getter protected final Enemy enemy; // owner
    @Getter protected final Context context;
    @Getter private int ticks = 0;
    @Getter @Setter protected int duration = 100;
    @Getter @Setter protected int warmup = 0;

    protected AbstractAbility(final Enemy enemy, final Context context) {
        this.enemy = enemy;
        this.context = context;
    }

    @Override
    public final void begin() {
        ticks = 0;
        onBegin();
    }

    @Override
    public final void end() {
        onEnd();
    }

    @Override
    public final boolean tick() {
        final int currentTicks = ticks++;
        if (currentTicks > duration) return false;
        if (currentTicks < warmup) return true;
        if (!onTick(currentTicks)) return false;
        return true;
    }

    abstract void onBegin();

    abstract boolean onTick(int ticks);

    abstract void onEnd();
}

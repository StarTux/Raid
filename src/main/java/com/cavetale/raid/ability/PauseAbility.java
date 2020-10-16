package com.cavetale.raid.ability;

import com.cavetale.raid.enemy.Enemy;

/**
 * Do nothing for a while.
 */
public final class PauseAbility extends AbstractAbility {
    public PauseAbility(final Enemy enemy, final Context context) {
        super(enemy, context);
        duration = 200;
    }

    @Override
    public void onBegin() { }

    @Override
    public void onEnd() { }

    @Override
    public boolean onTick(int ticks) {
        return true;
    }
}

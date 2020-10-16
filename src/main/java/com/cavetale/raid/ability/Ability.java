package com.cavetale.raid.ability;

/**
 * An ability is managed by an Enemy. When it used it called `begin`
 * once, then immediadely `tick`, which is repeated every tick until
 * it returns false, then `end` is called immediately. An ability may
 * be cancelled prematurely, in which case `end` is called.
 *
 * The above cycle may be repeated many times, so an instance is
 * tasked with resetting its state whenever `begin` or `end` is
 * called.
 *
 * The assumption is that abilities take several ticks to complete and
 * may involve status changes for the duration, such as loss of
 * mobility. They usually target a list of players retrieved from the
 * context.
 */

public interface Ability {
    void begin();

    boolean tick();

    void end();
}

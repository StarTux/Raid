package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveType;
import com.cavetale.raid.raiding.Raid;
import com.cavetale.raid.raiding.Raider;
import java.time.Duration;
import java.time.Instant;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;

public abstract class TimedWave extends AbstractWave {
    private Instant startedAt;
    private Instant expiresAt;
    private long lastShownSeconds;

    protected TimedWave(final WaveType type) {
        super(type);
    }

    protected final void startTimer(final int seconds) {
        lastShownSeconds = -1L;
        startedAt = Instant.now();
        expiresAt = startedAt.plusSeconds(seconds);
        setFinished(false);
    }

    protected final Duration getElapsedDuration() {
        return Duration.between(startedAt, Instant.now());
    }

    public final Duration getRemainingDuration() {
        if (expiresAt == null || isFinished()) return Duration.ZERO;
        final Duration remaining = Duration.between(Instant.now(), expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    protected final void showCountdown(final Raid raid) {
        final long seconds = getRemainingSeconds();
        if (seconds == lastShownSeconds || seconds == 0L) return;
        lastShownSeconds = seconds;
        final Component message = text("Time remaining: " + seconds + "s");
        for (final Raider raider : raid.getRaiders()) {
            final Player player = raider.getPlayer();
            if (!raider.isFallen() && player != null && player.isOnline() && !player.isDead()
                && raid.getWorlds().containsWorld(player.getWorld())) {
                player.sendActionBar(message);
            }
        }
    }

    /** Remaining seconds rounded up, suitable for a countdown. */
    public long getRemainingSeconds() {
        final Duration remaining = getRemainingDuration();
        return remaining.getSeconds() + (remaining.getNano() == 0 ? 0 : 1);
    }
}

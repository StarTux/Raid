package com.cavetale.raid.raiding.wave;

import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.raid.config.WaveType;
import com.cavetale.raid.config.wave.WinWaveConfig;
import com.cavetale.raid.event.RaidCompleteEvent;
import com.cavetale.raid.raiding.Raid;
import com.cavetale.raid.raiding.Raider;
import com.cavetale.raid.util.Fireworks;
import com.winthier.creative.BuildWorld;
import com.winthier.creative.Trust;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public final class WinWave extends TimedWave {
    private final Raid raid;
    private final WinWaveConfig config;
    private List<Location> fireworks;
    private List<String> credits;
    private long lastSlide;
    private int ticksUntilFirework;

    public WinWave(final Raid raid, final WinWaveConfig config) {
        super(WaveType.WIN);
        this.raid = raid;
        this.config = config;
    }

    @Override
    public void enable() {
        fireworks = config.getFireworks().stream().map(raid.getWorlds()::getLocation).toList();
    }

    @Override
    public void activate() {
        startTimer(config.getDuration());
        lastSlide = 0;
        ticksUntilFirework = 0;
        credits = getBuilderNames();
        final List<Player> winners = raid.getRaiders().stream()
            .filter(raider -> !raider.isFallen())
            .map(Raider::getPlayer)
            .filter(player -> player != null && player.isOnline() && !player.isDead() && raid.getWorlds().containsWorld(player.getWorld()))
            .toList();
        showTitle(text("Victory!", GOLD, BOLD), raid.getConfig().getDisplayNameComponent());
        new RaidCompleteEvent(raid, winners).callEvent();
    }

    private List<String> getBuilderNames() {
        final Map<UUID, Integer> ranks = new HashMap<>();
        for (final BuildWorld world : raid.getWorlds().getBuildWorlds()) {
            if (world.getOwner() != null) ranks.put(world.getOwner(), Trust.OWNER.priority + 1);
            for (final Trust trust : Trust.values()) {
                if (!trust.canBuild()) continue;
                for (final UUID uuid : world.listTrusted(trust)) ranks.merge(uuid, trust.priority, Integer::max);
            }
        }
        final Map<Integer, List<String>> groups = new TreeMap<>(Comparator.reverseOrder());
        ranks.forEach((uuid, rank) -> {
                final String name = PlayerCache.nameForUuid(uuid);
                if (name != null) groups.computeIfAbsent(rank, unused -> new ArrayList<>()).add(name);
            });
        final List<String> names = new ArrayList<>();
        for (final List<String> group : groups.values()) {
            group.sort(String.CASE_INSENSITIVE_ORDER);
            names.addAll(group);
        }
        return names;
    }

    private void showTitle(final Component heading, final Component subtitle) {
        final Title title = Title.title(heading, subtitle, Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(4), Duration.ofMillis(600)));
        for (final Raider raider : raid.getRaiders()) {
            final Player player = raider.getPlayer();
            if (player != null && player.isOnline()) player.showTitle(title);
        }
    }

    @Override
    public void tick() {
        if (getRemainingDuration().isZero()) {
            setFinished(true);
            raid.disable();
            return;
        }
        final long slide = getElapsedDuration().toSeconds() / 5;
        if (slide > lastSlide) {
            lastSlide = slide;
            if (slide == 1) {
                showTitle(empty(), text("made by", GRAY));
            } else if (slide - 2 < credits.size()) {
                showTitle(empty(), text(credits.get((int) (slide - 2)), AQUA));
            }
        }
        if (!fireworks.isEmpty() && ticksUntilFirework-- <= 0) {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            final Location location = fireworks.get(random.nextInt(fireworks.size()));
            if (location.isChunkLoaded()) Fireworks.spawnFirework(location);
            ticksUntilFirework = random.nextInt(10, 41);
        }
    }
}

package com.cavetale.raid.raiding.wave;

import com.cavetale.raid.config.WaveType;
import com.cavetale.raid.config.wave.GoalWaveConfig;
import com.cavetale.raid.raiding.Raid;
import com.cavetale.raid.raiding.Raider;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class GoalWave extends TimedWave {
    private static final Particle.DustOptions RED_DUST = new Particle.DustOptions(Color.RED, 1.0f);
    private final Raid raid;
    private final GoalWaveConfig config;
    private Location goal;
    private ItemDisplay banner;
    private double angle;

    public GoalWave(final Raid raid, final GoalWaveConfig config) {
        super(WaveType.GOAL);
        this.raid = raid;
        this.config = config;
    }

    @Override
    public void enable() {
        goal = raid.getWorlds().getLocation(config.getGoal());
    }

    @Override
    public void activate() {
        startTimer(config.getDuration());
        angle = 0.0;
        banner = goal.getWorld().spawn(goal, ItemDisplay.class, display -> {
                display.setItemStack(new ItemStack(Material.RED_BANNER));
                display.setGlowing(true);
                display.setPersistent(false);
                display.setTeleportDuration(1);
            });
    }

    @Override
    public void tick() {
        if (isFinished()) return;
        final boolean expired = getRemainingDuration().isZero();
        boolean someoneInside = false;
        boolean everyoneInside = true;
        for (final Raider raider : raid.getRaiders()) {
            if (raider.isFallen()) continue;
            final Player player = raider.getPlayer();
            if (player != null && player.isOnline() && isInsideGoal(player.getLocation())) {
                someoneInside = true;
            } else {
                everyoneInside = false;
                if (!expired) break;
                raid.kickPlayer(raider);
            }
        }
        if (expired || (someoneInside && everyoneInside)) {
            setFinished(true);
            return;
        }
        showCountdown(raid);
        final int radius = config.getRange();
        banner.setRotation((float) Math.toDegrees(angle), 0f);
        goal.getWorld().spawnParticle(
            Particle.DUST,
            goal.getX() + radius * Math.cos(angle), goal.getY() + 0.1,
            goal.getZ() + radius * Math.sin(angle), 1, 0, 0, 0, 0, RED_DUST
        );
        angle = (angle + 0.2) % (2 * Math.PI);
    }

    private boolean isInsideGoal(final Location location) {
        if (!goal.getWorld().equals(location.getWorld())) return false;
        final double dx = location.getX() - goal.getX();
        final double dz = location.getZ() - goal.getZ();
        final double radius = config.getRange();
        return dx * dx + dz * dz <= radius * radius;
    }

    @Override
    public void deactivate() {
        banner.remove();
        banner = null;
    }
}

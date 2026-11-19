package com.cavetale.raid.event;

import com.cavetale.raid.raiding.Raid;
import java.util.List;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public final class RaidCompleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Raid raid;
    private final List<Player> players;

    public RaidCompleteEvent(final Raid raid, final List<Player> players) {
        this.raid = raid;
        this.players = List.copyOf(players);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

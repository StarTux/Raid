package com.cavetale.raid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

/**
 * Implements an ordered highscore list.
 */
public final class Highscore {
    private List<Entry> entries = new ArrayList<>();

    @RequiredArgsConstructor @Getter
    public final class Entry {
        private final UUID uuid;
        private final String name;
        private double score = 0;
        private int rank;
    }

    public void add(Player player, double score) {
        UUID uuid = player.getUniqueId();
        Entry entry = null;
        for (Entry it : entries) {
            if (uuid.equals(it.uuid)) {
                entry = it;
                break;
            }
        }
        if (entry == null) {
            entry = new Entry(uuid, player.getName());
            entries.add(entry);
        }
        entry.score += score;
    }

    public List<Entry> getEntries() {
        Collections.sort(entries, (b, a) -> Double.compare(a.score, b.score));
        int i = 0;
        for (Entry entry : entries) {
            entry.rank = i++;
        }
        return entries;
    }

    public void reset() {
        entries.clear();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}

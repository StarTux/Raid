package com.cavetale.raid;

import java.util.ArrayList;
import java.util.List;

final class Wave implements ShortInfo {
    Type type = Type.MOBS;
    Place place;
    int radius = 0;
    final List<Spawn> spawns = new ArrayList<>();

    enum Type {
        MOBS,
        GOAL,
        BOSS;
    }

    @Override
    public String getShortInfo() {
        return type
            + " place=" + ShortInfo.of(place)
            + " radius=" + radius
            + " mobs=" + spawns.size();
    }
}

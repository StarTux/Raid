package com.cavetale.raid;

import java.util.ArrayList;
import java.util.List;

final class Wave implements ShortInfo {
    Type type = Type.MOBS;
    Place place;
    double radius = 0;
    List<Spawn> spawns = new ArrayList<>();
    Boss boss;

    enum Type {
        MOBS,
        GOAL,
        BOSS,
        WIN;
    }

    @Override
    public String getShortInfo() {
        return type
            + " place=" + ShortInfo.of(place)
            + " radius=" + radius
            + " mobs=" + spawns.size()
            + " boss=" + ShortInfo.of(boss);
    }
}

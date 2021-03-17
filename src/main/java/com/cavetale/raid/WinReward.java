package com.cavetale.raid;

import java.util.ArrayList;
import java.util.List;

public final class WinReward {
    protected boolean complete;
    List<Integer> unlocked = new ArrayList<>();

    WinReward() {
        for (int i = 0; i < 4; i += 1) {
            unlocked.add(0);
        }
    }
}

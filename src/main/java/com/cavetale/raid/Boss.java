package com.cavetale.raid;

import com.cavetale.enemy.EnemyType;
import lombok.NonNull;

final class Boss implements ShortInfo {
    EnemyType type;

    Boss() { }

    Boss(@NonNull final EnemyType type) {
        this.type = type;
    }

    @Override
    public String getShortInfo() {
        return "" + type;
    }
}

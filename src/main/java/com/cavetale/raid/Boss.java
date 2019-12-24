package com.cavetale.raid;

import lombok.NonNull;

final class Boss implements ShortInfo {
    Type type;

    enum Type {
        // Halloween 2019
        DECAYED("The Decayed"),
        FORGOTTEN("The Forgotten"),
        VENGEFUL("The Vengeful"),
        // Halloween 2018 (Legacy)
        SKELLINGTON("Skellington"),
        DEEP_FEAR("Deep Fear"),
        LAVA_LORD("Lava Lord"),
        // Christmas 2019
        FROSTWRECKER("Frostwrecker"),
        ICE_GOLEM("Ice Golem"),
        ICEKELLY("Icekelly"),
        SNOBEAR("Snobear"),
        QUEEN_BEE("Queen Bee");

        public final String displayName;

        Type(@NonNull final String displayName) {
            this.displayName = displayName;
        }
    }

    Boss() { }

    Boss(@NonNull final Type type) {
        this.type = type;
    }

    @Override
    public String getShortInfo() {
        return "" + type;
    }
}

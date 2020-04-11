package com.cavetale.raid;

import lombok.Value;

@Value
final class Vec3i {
    public final int x;
    public final int y;
    public final int z;

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
}

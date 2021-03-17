package com.cavetale.raid.struct;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public final class Areas {
    protected List<Cuboid> spawn = new ArrayList<>();
    protected List<Cuboid> gear = new ArrayList<>();
}

package com.cavetale.raid.config;

import com.cavetale.core.struct.Cuboid;
import java.io.Serializable;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class RaidAreaConfig implements Serializable {
    private String worldName;
    private Cuboid boundingBox;
    private ArrayList<Child> children;

    public RaidAreaConfig(final String worldName, final Cuboid boundingBox) {
        this.worldName = worldName;
        this.boundingBox = boundingBox;
    }

    public void addChild(final String name, final Cuboid area) {
        if (children == null) children = new ArrayList<>();
        children.add(new Child(name, area));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class Child {
        private String name;
        private Cuboid area;
    }
}

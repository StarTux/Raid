package com.cavetale.raid.config;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.raid.config.validate.ConfigValidation;
import com.cavetale.raid.config.validate.ValidationContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class RaidAreaConfig implements Serializable, ConfigValidation {
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
    public static final class Child implements ConfigValidation {
        private String name;
        private Cuboid area;

        @Override
        public void validate(final ValidationContext context) {
            if (name == null || name.isBlank()) context.child("name").error("Child name is required");
            validateCuboid(area, context.child("area"));
        }
    }

    @Override
    public void validate(final ValidationContext context) {
        context.child("worldName").validateWorldName(worldName);
        validateCuboid(boundingBox, context.child("boundingBox"));
    }

    @Override
    public void validateDeep(final ValidationContext context) {
        validate(context);
        if (children == null) return;
        final Set<String> names = new HashSet<>();
        for (int i = 0; i < children.size(); i += 1) {
            final Child child = children.get(i);
            final ValidationContext childContext = context.child("children").index(i);
            if (child == null) {
                childContext.error("Child area is required");
            } else {
                child.validateDeep(childContext);
                if (child.getName() != null && !names.add(child.getName())) {
                    childContext.child("name").error("Duplicate child name '" + child.getName() + "'");
                }
            }
        }
    }

    private static void validateCuboid(final Cuboid cuboid, final ValidationContext context) {
        if (cuboid == null) {
            context.error("Area bounds are required");
        } else if (cuboid.ax > cuboid.bx || cuboid.ay > cuboid.by || cuboid.az > cuboid.bz) {
            context.error("Minimum bounds must not exceed maximum bounds");
        }
    }
}

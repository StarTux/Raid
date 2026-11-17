package com.cavetale.raid.config.wave;

import java.io.Serializable;
import java.util.List;

public interface WaveTypeConfig extends Serializable {
    /**
     * Return all editable scalar fields of this configuration.
     * Implementations must include inherited fields when overriding this method.
     */
    default List<WaveEditField<?>> getEditFields() {
        return List.of();
    }

    /**
     * Return all editable lists of this configuration.
     * Implementations must include inherited lists when overriding this method.
     */
    default List<WaveEditList<?>> getEditLists() {
        return List.of();
    }
}

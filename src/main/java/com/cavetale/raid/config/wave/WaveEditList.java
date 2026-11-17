package com.cavetale.raid.config.wave;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * An editable list with accessors bound to its configuration instance.
 * The type describes the list elements, not the list itself.
 * The setter replaces the complete list.
 *
 * @param <T> the data type
 */
@Getter
@RequiredArgsConstructor
public final class WaveEditList<T> {
    private final String name;
    private final Class<T> type;
    private final Supplier<List<T>> getter;
    private final Consumer<List<T>> setter;
}

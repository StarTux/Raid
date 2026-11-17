package com.cavetale.raid.config.wave;

import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * An editable value with accessors bound to its configuration instance.
 * Use reference types such as Integer.class for primitive values.
 *
 * @param <T> the data type
 */
@Getter
@RequiredArgsConstructor
public final class WaveEditField<T> {
    private final String name;
    private final Class<T> type;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
}

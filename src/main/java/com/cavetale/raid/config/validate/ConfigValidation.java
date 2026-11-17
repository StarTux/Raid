package com.cavetale.raid.config.validate;

/** Validation never changes configuration data. */
public interface ConfigValidation {
    /** Check only this object's values and references, without visiting children. */
    default void validate(final ValidationContext context) {
    }

    /** Check this object and all its children. Leaf objects use the default. */
    default void validateDeep(final ValidationContext context) {
        validate(context);
    }
}

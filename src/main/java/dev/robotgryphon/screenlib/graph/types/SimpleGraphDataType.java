package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;

import java.util.Collection;
import java.util.Optional;


public record SimpleGraphDataType<T>(Class<T> dataType,
                                     Codec<T> codec,
                                     int color,
                                     Optional<T> defaultValueRaw,
                                     Optional<Collection<T>> allowedValues) implements GraphDataType<T> {

    /**
     * Convenience: no default, no fixed value set. Used for generic port types like minecraft:int.
     */
    public SimpleGraphDataType(Class<T> type, Codec<T> codec, int color) {
        this(type, codec, color, Optional.empty(), Optional.empty());
    }

    /**
     * Convenience: with default, no fixed value set. The common case for
     * numeric properties whose value is free-form within the type's range.
     */
    public SimpleGraphDataType(Class<T> type, Codec<T> codec, int color, T defaultValueRaw) {
        this(type, codec, color, Optional.of(defaultValueRaw), Optional.empty());
    }
}

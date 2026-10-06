package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;

import java.util.Collection;
import java.util.Optional;

public record SimpleGraphDataType<T>(Class<T> dataType,
                                     Codec<T> codec,
                                     int color,
                                     Optional<T> defaultValueRaw,
                                     Optional<Collection<T>> allowedValues) implements GraphDataType<T> {
    public SimpleGraphDataType(Class<T> type, Codec<T> codec, int color) {
        this(type, codec, color, Optional.empty(), Optional.empty());
    }

    public SimpleGraphDataType(Class<T> type, Codec<T> codec, int color, T defaultValueRaw) {
        this(type, codec, color, Optional.of(defaultValueRaw), Optional.empty());
    }
}

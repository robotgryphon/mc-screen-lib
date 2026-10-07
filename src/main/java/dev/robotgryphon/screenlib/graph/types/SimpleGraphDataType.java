package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import net.minecraft.core.RegistryAccess;

import java.util.Collection;
import java.util.Optional;

public record SimpleGraphDataType<T>(Class<T> dataType,
                                     Codec<T> codec,
                                     Optional<T> defaultValueRaw,
                                     Optional<Collection<T>> allowedValues) implements GraphDataType<T> {
    @Override
    public Optional<Collection<T>> allowedValues(RegistryAccess registries) {
        return this.allowedValues;
    }

    public SimpleGraphDataType(Class<T> type, Codec<T> codec) {
        this(type, codec, Optional.empty(), Optional.empty());
    }

    public SimpleGraphDataType(Class<T> type, Codec<T> codec, T defaultValueRaw) {
        this(type, codec, Optional.of(defaultValueRaw), Optional.empty());
    }
}

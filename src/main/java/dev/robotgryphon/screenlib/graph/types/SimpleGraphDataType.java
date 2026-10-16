package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Collection;
import java.util.Optional;

public record SimpleGraphDataType<T>(Class<T> dataType,
                                     Codec<T> codec,
                                     StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec,
                                     Optional<T> defaultValueRaw,
                                     Optional<Collection<T>> allowedValues) implements GraphDataType<T> {
    @Override
    public Optional<Collection<T>> allowedValues(RegistryAccess registries) {
        return this.allowedValues;
    }

    public SimpleGraphDataType(Class<T> type, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        this(type, codec, streamCodec, Optional.empty(), Optional.empty());
    }

    public SimpleGraphDataType(Class<T> type, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec,
                               T defaultValueRaw) {
        this(type, codec, streamCodec, Optional.of(defaultValueRaw), Optional.empty());
    }
}

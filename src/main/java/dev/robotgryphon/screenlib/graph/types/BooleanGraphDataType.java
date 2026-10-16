package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.Optional;

public class BooleanGraphDataType implements GraphDataType<Boolean> {
    @Override
    public Class<Boolean> dataType() {
        return Boolean.class;
    }

    @Override
    public Codec<Boolean> codec() {
        return Codec.BOOL;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, Boolean> streamCodec() {
        return ByteBufCodecs.BOOL;
    }

    @Override
    public Optional<Boolean> defaultValueRaw() {
        return Optional.of(false);
    }
}

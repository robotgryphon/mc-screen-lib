package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class BooleanGraphDataType implements DataTypeWithDefault<Boolean> {
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
    public Boolean defaultValue() {
        return false;
    }
}

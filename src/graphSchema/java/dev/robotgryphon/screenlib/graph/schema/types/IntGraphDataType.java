package dev.robotgryphon.screenlib.graph.schema.types;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;

public class IntGraphDataType implements GraphDataType<Integer> {
    @Override
    public Class<Integer> dataType() {
        return Integer.class;
    }

    @Override
    public Codec<Integer> codec() {
        return Codec.INT;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, Integer> streamCodec() {
        return ByteBufCodecs.VAR_INT;
    }
}

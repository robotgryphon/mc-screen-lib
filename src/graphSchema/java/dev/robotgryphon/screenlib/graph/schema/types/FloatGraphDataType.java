package dev.robotgryphon.screenlib.graph.schema.types;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;

public class FloatGraphDataType implements GraphDataType<Float> {
    @Override
    public Class<Float> dataType() {
        return Float.class;
    }

    @Override
    public Codec<Float> codec() {
        return Codec.FLOAT;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, Float> streamCodec() {
        return ByteBufCodecs.FLOAT;
    }
}

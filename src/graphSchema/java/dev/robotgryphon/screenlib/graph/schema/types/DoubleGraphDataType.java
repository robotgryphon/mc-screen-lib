package dev.robotgryphon.screenlib.graph.schema.types;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;

public class DoubleGraphDataType implements GraphDataType<Double> {
    @Override
    public Class<Double> dataType() {
        return Double.class;
    }

    @Override
    public Codec<Double> codec() {
        return Codec.DOUBLE;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, Double> streamCodec() {
        return ByteBufCodecs.DOUBLE;
    }
}

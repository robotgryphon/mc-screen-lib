package dev.robotgryphon.screenlib.graph.schema.types;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;

public class StringGraphDataType implements GraphDataType<String> {
    @Override
    public Class<String> dataType() {
        return String.class;
    }

    @Override
    public Codec<String> codec() {
        return Codec.STRING;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, String> streamCodec() {
        return ByteBufCodecs.STRING_UTF8;
    }
}

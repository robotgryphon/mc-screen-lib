package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class ItemHandlerGraphDataType implements GraphDataType<ItemHandlerAccess> {
    @Override
    public Class<ItemHandlerAccess> dataType() {
        return ItemHandlerAccess.class;
    }

    @Override
    public Codec<ItemHandlerAccess> codec() {
        return ItemHandlerAccess.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ItemHandlerAccess> streamCodec() {
        return ItemHandlerAccess.STREAM_CODEC;
    }
}

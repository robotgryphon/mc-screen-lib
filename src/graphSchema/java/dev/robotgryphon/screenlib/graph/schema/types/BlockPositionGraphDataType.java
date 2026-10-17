package dev.robotgryphon.screenlib.graph.schema.types;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.BlockPos;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;

public class BlockPositionGraphDataType implements GraphDataType<BlockPos> {
    @Override
    public Class<BlockPos> dataType() {
        return BlockPos.class;
    }

    @Override
    public Codec<BlockPos> codec() {
        return BlockPos.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, BlockPos> streamCodec() {
        return BlockPos.STREAM_CODEC;
    }
}

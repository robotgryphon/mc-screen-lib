package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockStateGraphDataType implements GraphDataType<BlockState> {
    @Override
    public Class<BlockState> dataType() {
        return BlockState.class;
    }

    @Override
    public Codec<BlockState> codec() {
        return BlockState.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, BlockState> streamCodec() {
        return ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY);
    }
}

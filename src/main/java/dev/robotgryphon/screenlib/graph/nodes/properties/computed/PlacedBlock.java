package dev.robotgryphon.screenlib.graph.nodes.properties.computed;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record PlacedBlock(ResourceKey<Level> level, BlockPos pos, BlockState state) {
    public static final StreamCodec<ByteBuf, PlacedBlock> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), PlacedBlock::level,
            BlockPos.STREAM_CODEC, PlacedBlock::pos,
            ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), PlacedBlock::state,
            PlacedBlock::new
    );
}

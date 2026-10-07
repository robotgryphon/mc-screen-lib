package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;

public class BlockPositionGraphDataType implements GraphDataType<BlockPos> {
    @Override
    public Class<BlockPos> dataType() {
        return BlockPos.class;
    }

    @Override
    public Codec<BlockPos> codec() {
        return BlockPos.CODEC;
    }
}

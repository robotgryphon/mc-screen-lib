package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.graph.GraphTheme;
import net.minecraft.core.BlockPos;

public class BlockPositionGraphDataType implements GraphDataType<BlockPos> {
    @Override
    public Class<BlockPos> dataType() {
        return BlockPos.class;
    }

    @Override
    public int color() {
        return GraphTheme.COLOR_POSITION;
    }

    @Override
    public Codec<BlockPos> codec() {
        return BlockPos.CODEC;
    }
}

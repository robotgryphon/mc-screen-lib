package dev.robotgryphon.screenlib.graph.computed;

import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class BlockStateAt implements ComputedProperty<BlockState> {
    @Override
    public Optional<BlockState> compute(ComputeContext context) {
        var server = context.server();
        var position = context.get("Position", ScreenLib.BLOCK_POS_DATA_TYPE);
        var levelKey = context.get("Level", ScreenLib.LEVEL_DATA_TYPE);

        if (server == null || position.isEmpty() || levelKey.isEmpty())
            return Optional.empty();

        var level = server.getLevel(levelKey.get());
        return level == null ? Optional.empty() : Optional.of(level.getBlockState(position.get()));
    }
}

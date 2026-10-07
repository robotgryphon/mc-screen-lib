package dev.robotgryphon.screenlib.graph.computed;

import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.BlockPos;

import java.util.Optional;

public class BlockPositionFromCoordinates implements ComputedProperty<BlockPos> {
    @Override
    public Optional<BlockPos> compute(ComputeContext context) {
        Optional<Integer> x = context.get("x", ScreenLib.INTEGER_DATA_TYPE.value());
        Optional<Integer> y = context.get("y", ScreenLib.INTEGER_DATA_TYPE.value());
        Optional<Integer> z = context.get("z", ScreenLib.INTEGER_DATA_TYPE.value());
        if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BlockPos(x.get(), y.get(), z.get()));
    }
}

package dev.robotgryphon.screenlib.graph.nodes.properties.computed;

import dev.robotgryphon.screenlib.graph.nodes.properties.DataKey;

import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;

public class BlockPositionFromCoordinates implements ComputedProperty<BlockPos> {
    public static final DataKey<Integer> X = DataKey.required("x", ScreenLib.INTEGER_DATA_TYPE);
    public static final DataKey<Integer> Y = DataKey.required("y", ScreenLib.INTEGER_DATA_TYPE);
    public static final DataKey<Integer> Z = DataKey.required("z", ScreenLib.INTEGER_DATA_TYPE);

    @Override
    public List<DataKey<?>> requires() {
        return List.of(X, Y, Z);
    }

    @Override
    public Optional<BlockPos> compute(ComputeContext context) {
        Optional<Integer> x = context.get(X);
        Optional<Integer> y = context.get(Y);
        Optional<Integer> z = context.get(Z);
        if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BlockPos(x.get(), y.get(), z.get()));
    }
}

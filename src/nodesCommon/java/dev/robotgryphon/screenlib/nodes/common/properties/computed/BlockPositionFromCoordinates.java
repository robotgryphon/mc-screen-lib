package dev.robotgryphon.screenlib.nodes.common.properties.computed;

import dev.robotgryphon.screenlib.nodes.api.properties.computed.ComputedProperties;
import net.neoforged.neoforge.registries.DeferredHolder;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import dev.robotgryphon.screenlib.graph.schema.properties.DataKey;
import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.nodes.common.ScreenLibNodes;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;
import dev.robotgryphon.screenlib.graph.schema.properties.computed.ComputeContext;
import dev.robotgryphon.screenlib.graph.schema.properties.computed.ComputedProperty;

public class BlockPositionFromCoordinates implements ComputedProperty<BlockPos> {
    private static final DataKey<Integer> X = ComputedProperties.BlockPositionFromCoordinates.X;
    private static final DataKey<Integer> Y = ComputedProperties.BlockPositionFromCoordinates.Y;
    private static final DataKey<Integer> Z = ComputedProperties.BlockPositionFromCoordinates.Z;

    @Override
    public DeferredHolder<GraphDataType<?>, ? extends GraphDataType<BlockPos>> type() {
        return ScreenLibNodes.BLOCK_POS_DATA_TYPE;
    }

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

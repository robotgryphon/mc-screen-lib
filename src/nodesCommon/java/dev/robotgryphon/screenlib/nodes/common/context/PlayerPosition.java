package dev.robotgryphon.screenlib.nodes.common.context;

import net.minecraft.core.BlockPos;

import java.util.Map;
import dev.robotgryphon.screenlib.graph.schema.context.NodeCreationContext;
import dev.robotgryphon.screenlib.graph.schema.context.NodeContextSupplier;

public class PlayerPosition implements NodeContextSupplier {
    @Override
    public Map<String, Object> supply(NodeCreationContext context) {
        BlockPos pos = context.player().blockPosition();
        return Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ());
    }
}

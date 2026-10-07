package dev.robotgryphon.screenlib.graph.context;

import net.minecraft.core.BlockPos;

import java.util.Map;

public class PlayerPosition implements NodeContextSupplier {
    @Override
    public Map<String, Object> supply(NodeCreationContext context) {
        BlockPos pos = context.player().blockPosition();
        return Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ());
    }
}

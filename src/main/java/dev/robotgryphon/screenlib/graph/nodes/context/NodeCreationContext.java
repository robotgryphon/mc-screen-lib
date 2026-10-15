package dev.robotgryphon.screenlib.graph.nodes.context;

import dev.robotgryphon.screenlib.graph.canvas.CanvasSupplier;
import net.minecraft.server.level.ServerPlayer;

public record NodeCreationContext(ServerPlayer player, CanvasSupplier source) {
}

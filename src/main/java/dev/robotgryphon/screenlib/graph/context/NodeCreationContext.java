package dev.robotgryphon.screenlib.graph.context;

import dev.robotgryphon.screenlib.menu.CanvasSource;
import net.minecraft.server.level.ServerPlayer;

public record NodeCreationContext(ServerPlayer player, CanvasSource source) {
}

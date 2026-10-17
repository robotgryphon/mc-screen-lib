package dev.robotgryphon.screenlib.graph.schema.properties.computed;

import dev.robotgryphon.screenlib.graph.schema.properties.DataKey;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;

public interface ComputeContext {
    MinecraftServer server();

    <T> Optional<T> get(DataKey<T> key);
}

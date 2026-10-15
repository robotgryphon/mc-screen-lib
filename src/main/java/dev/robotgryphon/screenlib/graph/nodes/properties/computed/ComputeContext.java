package dev.robotgryphon.screenlib.graph.nodes.properties.computed;

import dev.robotgryphon.screenlib.graph.nodes.properties.DataKey;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.Optional;

public final class ComputeContext {
    private final Node node;
    private final Map<String, String> data;
    private final MinecraftServer server;

    public ComputeContext(Node node, Map<String, String> data, MinecraftServer server) {
        this.node = node;
        this.data = data;
        this.server = server;
    }

    public MinecraftServer server() {
        return this.server;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(DataKey<T> key) {
        String property = this.data.get(key.name());
        if (property == null) {
            return Optional.empty();
        }
        return this.node.propertyManager().resolve(property)
                .filter(value -> key.type().get().dataType().isInstance(value))
                .map(value -> (T) value);
    }
}

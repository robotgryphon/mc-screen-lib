package dev.robotgryphon.screenlib.graph.computed;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public final class ComputeContext {
    private final Node node;
    private final Map<String, String> data;

    public ComputeContext(Node node, Map<String, String> data) {
        this.node = node;
        this.data = data;
    }

    public @Nullable MinecraftServer server() {
        return this.node.canvas.server();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(DataKey<T> key) {
        String property = this.data.get(key.name());
        if (property == null) {
            return Optional.empty();
        }
        return this.node.resolvedValue(property)
                .filter(value -> key.type().get().dataType().isInstance(value))
                .map(value -> (T) value);
    }
}

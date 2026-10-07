package dev.robotgryphon.screenlib.graph.computed;

import dev.robotgryphon.screenlib.graph.Connection;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.ports.PropertyInputPort;
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
        return this.value(property)
                .filter(value -> key.type().get().dataType().isInstance(value))
                .map(value -> (T) value);
    }

    private Optional<Object> value(String property) {
        PropertyInputPort propertyPort = this.node.portManager().propertyPort(property);
        if (propertyPort != null) {
            Connection inbound = this.node.portManager().inbound(propertyPort);
            if (inbound != null) {
                return Optional.ofNullable(this.node.canvas.resolveUpstreamValue(inbound));
            }
        }
        return Optional.ofNullable(this.node.propertyManager().value(property));
    }
}

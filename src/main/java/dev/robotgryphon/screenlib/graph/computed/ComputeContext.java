package dev.robotgryphon.screenlib.graph.computed;

import dev.robotgryphon.screenlib.graph.Connection;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.ports.Port;
import dev.robotgryphon.screenlib.graph.ports.PropertyInputPort;
import dev.robotgryphon.screenlib.graph.ports.StandardInputPort;
import dev.robotgryphon.screenlib.graph.types.BlockPositionGraphDataType;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

public final class ComputeContext {
    private final Node node;

    public ComputeContext(Node node) {
        this.node = node;
    }

    public @Nullable MinecraftServer server() {
        return this.node.canvas.server();
    }

    public Optional<Object> value(String name) {
        for (Port port : this.node.portManager().ports()) {
            if (port instanceof StandardInputPort input && input.title().getString().equals(name)) {
                Connection inbound = this.node.portManager().inbound(port);
                return inbound == null
                        ? Optional.empty()
                        : Optional.ofNullable(this.node.canvas.resolveUpstreamValue(inbound));
            }
        }
        PropertyInputPort propertyPort = this.node.portManager().propertyPort(name);
        if (propertyPort != null) {
            Connection inbound = this.node.portManager().inbound(propertyPort);
            if (inbound != null) {
                return Optional.ofNullable(this.node.canvas.resolveUpstreamValue(inbound));
            }
        }
        return Optional.ofNullable(this.node.propertyManager().value(name));
    }

    @SuppressWarnings("unchecked")
    public <T, V extends GraphDataType<T>> Optional<T> get(String name, Supplier<V> type) {
        return this.value(name)
                .filter(value -> type.get().dataType().isInstance(value))
                .map(value -> (T) value);
    }

    @SuppressWarnings("unchecked")
    public <V> Optional<V> get(String name, GraphDataType<V> type) {
        return this.value(name)
                .filter(value -> type.dataType().isInstance(value))
                .map(value -> (V) value);
    }
}

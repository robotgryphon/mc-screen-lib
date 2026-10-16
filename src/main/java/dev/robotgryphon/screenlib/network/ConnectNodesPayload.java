package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.PropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import dev.robotgryphon.screenlib.graph.state.CanvasState;
import dev.robotgryphon.screenlib.graph.state.ConnectionState;
import dev.robotgryphon.screenlib.graph.state.NodeState;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record ConnectNodesPayload(ConnectionState connection) implements CanvasEditPayload {
    public static final Type<ConnectNodesPayload> TYPE = new Type<>(ScreenLib.id("connect_nodes"));

    public static final StreamCodec<ByteBuf, ConnectNodesPayload> STREAM_CODEC = StreamCodec.composite(
            ConnectionState.STREAM_CODEC, ConnectNodesPayload::connection,
            ConnectNodesPayload::new
    );

    @Override
    public Optional<CanvasState> apply(CanvasState state, RegistryAccess registries, Set<UUID> required) {
        if (this.connection.sourceNode().equals(this.connection.targetNode())) {
            return Optional.empty();
        }
        Optional<PropertyDefinition<?>> source = state.node(this.connection.sourceNode())
                .map(NodeState::definition)
                .flatMap(definition -> output(definition.value(), this.connection.sourcePort()));
        Optional<PropertyDefinition<?>> target = state.node(this.connection.targetNode())
                .map(NodeState::definition)
                .flatMap(definition -> input(definition.value(), this.connection.targetPort()));
        if (source.isEmpty() || target.isEmpty() || source.get().anyType().value() != target.get().anyType().value()) {
            return Optional.empty();
        }
        List<ConnectionState> connections = new ArrayList<>();
        for (ConnectionState existing : state.connections()) {
            if (!existing.targetNode().equals(this.connection.targetNode())
                    || !existing.targetPort().equals(this.connection.targetPort())) {
                connections.add(existing);
            }
        }
        connections.add(this.connection);
        return Optional.of(state.withConnections(List.copyOf(connections)));
    }

    private static Optional<PropertyDefinition<?>> output(NodeDefinition definition, String property) {
        return definition.outputs().stream().anyMatch(output -> output.property().equals(property))
                ? definition.property(property)
                : Optional.empty();
    }

    private static Optional<PropertyDefinition<?>> input(NodeDefinition definition, String property) {
        return definition.inputs().stream().anyMatch(input -> input.property().equals(property))
                ? definition.property(property)
                : Optional.empty();
    }

    @Override
    public Type<ConnectNodesPayload> type() {
        return TYPE;
    }
}

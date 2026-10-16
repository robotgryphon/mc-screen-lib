package dev.robotgryphon.screenlib.graph.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record CanvasState(UUID id, List<NodeState> nodes, List<ConnectionState> connections) {
    public static final MapCodec<CanvasState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("id")
                    .forGetter(state -> Optional.of(state.id())),
            NodeState.CODEC.listOf()
                    .optionalFieldOf("nodes", List.of())
                    .forGetter(CanvasState::nodes),
            ConnectionState.CODEC.listOf()
                    .optionalFieldOf("connections", List.of())
                    .forGetter(CanvasState::connections)
    ).apply(i, (id, nodes, connections) -> new CanvasState(id.orElseGet(UUID::randomUUID), nodes, connections)));

    public static final Codec<CanvasState> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasState> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CanvasState::id,
            NodeState.STREAM_CODEC.apply(ByteBufCodecs.list()), CanvasState::nodes,
            ConnectionState.STREAM_CODEC.apply(ByteBufCodecs.list()), CanvasState::connections,
            CanvasState::new
    );

    public static CanvasState empty() {
        return new CanvasState(UUID.randomUUID(), List.of(), List.of());
    }

    public CanvasState withContents(List<NodeState> nodes, List<ConnectionState> connections) {
        return new CanvasState(this.id, nodes, connections);
    }

    public Optional<NodeState> node(UUID id) {
        return this.nodes.stream().filter(node -> node.id().equals(id)).findFirst();
    }

    public CanvasState withNode(NodeState replacement) {
        return this.withContents(this.nodes.stream()
                .map(node -> node.id().equals(replacement.id()) ? replacement : node)
                .toList(), this.connections);
    }

    public CanvasState withoutNode(UUID id) {
        return this.withContents(
                this.nodes.stream().filter(node -> !node.id().equals(id)).toList(),
                this.connections.stream()
                        .filter(connection -> !connection.sourceNode().equals(id) && !connection.targetNode().equals(id))
                        .toList());
    }

    public CanvasState withConnections(List<ConnectionState> connections) {
        return this.withContents(this.nodes, connections);
    }
}

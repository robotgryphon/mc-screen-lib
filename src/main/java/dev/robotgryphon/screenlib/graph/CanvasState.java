package dev.robotgryphon.screenlib.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.nodes.NodeState;

import java.util.List;

public record CanvasState(List<NodeState> nodes, List<ConnectionState> connections) {
    public static final CanvasState EMPTY = new CanvasState(List.of(), List.of());

    public static final MapCodec<CanvasState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NodeState.CODEC.listOf()
                    .optionalFieldOf("nodes", List.of())
                    .forGetter(CanvasState::nodes),
            ConnectionState.CODEC.listOf()
                    .optionalFieldOf("connections", List.of())
                    .forGetter(CanvasState::connections)
    ).apply(i, CanvasState::new));

    public static final Codec<CanvasState> CODEC = MAP_CODEC.codec();
}

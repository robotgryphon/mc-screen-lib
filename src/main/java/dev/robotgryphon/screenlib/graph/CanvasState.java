package dev.robotgryphon.screenlib.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.nodes.NodeState;

import java.util.List;
import java.util.Optional;

public record CanvasState(List<NodeState> nodes, List<ConnectionState> connections,
                          Optional<ViewportState> viewport) {
    public static final CanvasState EMPTY = new CanvasState(List.of(), List.of(), Optional.empty());

    public static final MapCodec<CanvasState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NodeState.CODEC.listOf()
                    .optionalFieldOf("nodes", List.of())
                    .forGetter(CanvasState::nodes),
            ConnectionState.CODEC.listOf()
                    .optionalFieldOf("connections", List.of())
                    .forGetter(CanvasState::connections),
            ViewportState.CODEC
                    .lenientOptionalFieldOf("viewport")
                    .forGetter(CanvasState::viewport)
    ).apply(i, CanvasState::new));

    public static final Codec<CanvasState> CODEC = MAP_CODEC.codec();
}

package dev.robotgryphon.screenlib.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Serializable snapshot of one connection between two nodes' ports.
 *
 * <p>Nodes are referenced by their <em>index</em> in the host
 * {@link CanvasState#nodes()} list — load preserves the order, so the
 * index resolves back to the same {@link Node} after a round-trip. Ports
 * are referenced by their index in {@link Node#ports()} (which is the
 * declaration order from the {@link dev.robotgryphon.screenlib.types.NodeDefinition}:
 * all inputs first, then all outputs).
 *
 * <p>The convention is the same as a live {@link Connection}: source side
 * is always {@link PortSide#RIGHT} (an output), target side is always
 * {@link PortSide#LEFT} (an input). Loading a state that violates this is
 * silently dropped by {@link Canvas#loadState}.
 *
 * @param sourceNodeIndex  index of the source node within {@code CanvasState.nodes}
 * @param sourcePortIndex  index of the source port within the source node's {@code ports()}
 * @param targetNodeIndex  index of the target node within {@code CanvasState.nodes}
 * @param targetPortIndex  index of the target port within the target node's {@code ports()}
 * @param color            ARGB color the line should render with
 */
public record ConnectionState(int sourceNodeIndex,
                              int sourcePortIndex,
                              int targetNodeIndex,
                              int targetPortIndex,
                              int color) {

    public static final Codec<ConnectionState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("source_node").forGetter(ConnectionState::sourceNodeIndex),
            Codec.INT.fieldOf("source_port").forGetter(ConnectionState::sourcePortIndex),
            Codec.INT.fieldOf("target_node").forGetter(ConnectionState::targetNodeIndex),
            Codec.INT.fieldOf("target_port").forGetter(ConnectionState::targetPortIndex),
            Codec.INT.fieldOf("color").forGetter(ConnectionState::color)
    ).apply(i, ConnectionState::new));
}

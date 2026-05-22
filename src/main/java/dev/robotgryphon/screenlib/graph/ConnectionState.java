package dev.robotgryphon.screenlib.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * Serializable snapshot of one connection between two nodes' ports.
 *
 * <p>Nodes are referenced by their <em>index</em> in the host
 * {@link CanvasState#nodes()} list — load preserves the order, so the
 * index resolves back to the same {@link Node} after a round-trip.
 *
 * <p>Ports are referenced primarily by <em>name</em>: the source / target
 * port name fields carry the port's local identifier (its title for a
 * regular input / output port, the property name for a property port).
 * The legacy port-index fields are still written and read so older
 * saves (pre-name) decode cleanly, and so do canvases edited against a
 * node definition that has since had its ports added / removed /
 * reordered — the name lookup is the authoritative one, indices are a
 * fallback used only when the name doesn't resolve.
 *
 * <p>The convention is the same as a live {@link Connection}: source side
 * is always {@link PortSide#RIGHT} (an output), target side is always
 * {@link PortSide#LEFT} (an input). {@link CanvasStateManager#loadState}
 * re-validates the side + the port types on every connection it
 * rehydrates and drops any wire that fails — same checks
 * {@link Canvas#connect} enforces at edit time, so a saved canvas
 * survives a node-definition change without the graph carrying
 * invalid wires after load.
 *
 * @param sourceNodeIndex  index of the source node within {@code CanvasState.nodes}
 * @param sourcePortIndex  index of the source port within the source node's {@code ports()};
 *                         fallback identifier used when {@code sourcePortName} doesn't resolve
 * @param sourcePortName   local name of the source port; takes precedence over
 *                         {@code sourcePortIndex} when present in the saved record.
 *                         Empty for legacy saves written before this field landed
 * @param targetNodeIndex  index of the target node within {@code CanvasState.nodes}
 * @param targetPortIndex  index of the target port within the target node's {@code ports()};
 *                         fallback identifier used when {@code targetPortName} doesn't resolve
 * @param targetPortName   local name of the target port; takes precedence over
 *                         {@code targetPortIndex} when present
 * @param color            ARGB color the line should render with
 */
public record ConnectionState(int sourceNodeIndex,
                              int sourcePortIndex,
                              Optional<String> sourcePortName,
                              int targetNodeIndex,
                              int targetPortIndex,
                              Optional<String> targetPortName,
                              int color) {

    /**
     * Five-arg convenience — keeps test-only call sites that build a
     * connection state by hand source-compatible, defaulting both
     * names to absent (which forces index-based lookup on load).
     */
    public ConnectionState(int sourceNodeIndex, int sourcePortIndex,
                           int targetNodeIndex, int targetPortIndex,
                           int color) {
        this(sourceNodeIndex, sourcePortIndex, Optional.empty(),
                targetNodeIndex, targetPortIndex, Optional.empty(),
                color);
    }

    public static final Codec<ConnectionState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("source_node").forGetter(ConnectionState::sourceNodeIndex),
            Codec.INT.fieldOf("source_port").forGetter(ConnectionState::sourcePortIndex),
            // Name fields are optional so legacy saves (which only wrote
            // indices) decode cleanly; the empty Optional in that case
            // falls through to the index-based lookup on load.
            Codec.STRING.optionalFieldOf("source_port_name").forGetter(ConnectionState::sourcePortName),
            Codec.INT.fieldOf("target_node").forGetter(ConnectionState::targetNodeIndex),
            Codec.INT.fieldOf("target_port").forGetter(ConnectionState::targetPortIndex),
            Codec.STRING.optionalFieldOf("target_port_name").forGetter(ConnectionState::targetPortName),
            Codec.INT.fieldOf("color").forGetter(ConnectionState::color)
    ).apply(i, ConnectionState::new));
}

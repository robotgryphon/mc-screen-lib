package dev.robotgryphon.screenlib.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import dev.robotgryphon.screenlib.client.ui.widget.Connection;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.types.PortDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Stateless façade for moving a {@link Canvas} in and out of its
 * serializable {@link CanvasState} form. The translation logic used to
 * live on {@code Canvas} itself, but it had grown a private-helper
 * sprawl ({@code toState}, {@code loadState}, {@code encodePropertyValues},
 * {@code applyPropertyValues}, an {@code isValidIndex} predicate) that
 * had nothing to do with the canvas's "graph + view-transform" job.
 * Lifting all of it to a separate manager keeps {@code Canvas} focused
 * on holding live state and gives the persistence path one obvious entry
 * point.
 *
 * <p>The class is intentionally a holder of static methods only — there's
 * no instance state to carry between {@code toState} and {@code loadState}
 * calls, and threading an extra "manager" object around would just add
 * noise. Callers pass in the {@link Canvas} (and {@link CanvasState} when
 * loading); everything else (node lookups, codec dispatch, index
 * validation) is derived from those.
 *
 * <p>Property-value translation rides the property's own codec via
 * {@link NbtOps#INSTANCE}; the values land in {@link NodeState} as
 * {@code Map<String, Dynamic<?>>} so they round-trip through whichever
 * ops the outer canvas codec is currently running on. See
 * {@link NodeState} for why the codec-agnostic {@code Dynamic} form
 * matters at the serialization layer.
 */
public final class CanvasStateManager {

    private CanvasStateManager() {}

    /**
     * Captures a serializable snapshot of {@code canvas}'s current nodes
     * and connections. The snapshot is a plain {@link CanvasState}
     * record — encode it with {@link CanvasState#CODEC} against whatever
     * storage format the caller wants (JSON via {@code JsonOps}, NBT via
     * {@code NbtOps}, etc.).
     *
     * <p>Pan / zoom and any other view-only state are intentionally
     * excluded; only the document — nodes + wires + per-property current
     * values — is captured.
     */
    public static CanvasState toState(Canvas canvas) {
        List<NodeWidget> widgets = canvas.nodes();
        List<NodeState> nodeStates = new ArrayList<>(widgets.size());
        // IdentityHashMap so node ref-equality (not value-equality) drives
        // the index lookup — two structurally identical nodes are still
        // distinct entries on the canvas.
        Map<Node, Integer> indexOf = new IdentityHashMap<>(widgets.size());
        for (int i = 0; i < widgets.size(); i++) {
            Node node = widgets.get(i).node();
            indexOf.put(node, i);
            nodeStates.add(new NodeState(
                    node.definitionHolder(), node.x(), node.y(),
                    encodePropertyValues(node)));
        }

        List<Connection> connections = canvas.connections();
        List<ConnectionState> connectionStates = new ArrayList<>(connections.size());
        for (Connection c : connections) {
            Integer srcIdx = indexOf.get(c.source());
            Integer tgtIdx = indexOf.get(c.target());
            if (srcIdx == null || tgtIdx == null) {
                // Connection refers to a node not in our list — shouldn't
                // happen with the normal API but skip rather than serialize
                // a dangling reference.
                continue;
            }
            int srcPortIdx = c.source().ports().indexOf(c.sourcePort());
            int tgtPortIdx = c.target().ports().indexOf(c.targetPort());
            if (srcPortIdx < 0 || tgtPortIdx < 0) {
                continue;
            }
            connectionStates.add(new ConnectionState(
                    srcIdx, srcPortIdx, tgtIdx, tgtPortIdx, c.color()));
        }

        return new CanvasState(nodeStates, connectionStates);
    }

    /**
     * Restores {@code state} into {@code canvas}, replacing whatever
     * was there. The flow:
     * <ol>
     *   <li>Wipe the canvas via {@link Canvas#clear} so any back-references
     *       on the old widgets are cleared before they go out of scope.</li>
     *   <li>For each {@link NodeState}, run {@code nodeBuilder} to produce
     *       a {@link NodeWidget} (the host decides title text, custom
     *       subclasses, etc.), overlay persisted property values on top
     *       of whatever defaults the node constructor seeded, and add it
     *       to the canvas. Adding in order preserves the indices that
     *       {@link ConnectionState} entries reference.</li>
     *   <li>For each {@link ConnectionState}, resolve the indexed
     *       nodes / ports against the rebuilt graph. References that
     *       fall outside (a missing node, a port index that doesn't
     *       exist on the definition anymore) are dropped silently rather
     *       than crashing the load.</li>
     * </ol>
     */
    public static void loadState(Canvas canvas, CanvasState state,
                                 Function<NodeState, NodeWidget> nodeBuilder) {
        canvas.clear();

        for (NodeState ns : state.nodes()) {
            NodeWidget widget = nodeBuilder.apply(ns);
            // Apply persisted property values on top of the defaults the
            // Node constructor already seeded — saved values win, missing
            // entries fall through to whatever the schema default is.
            applyPropertyValues(widget.node(), ns.propertyValues());
            // Go through addNode so the widget's canvas back-reference is
            // set the same way as for runtime-added nodes — otherwise
            // property ports on loaded nodes would never know they're
            // connected and would stay hidden.
            canvas.addNode(widget);
        }

        List<NodeWidget> rebuilt = canvas.nodes();
        for (ConnectionState cs : state.connections()) {
            if (!isValidIndex(cs.sourceNodeIndex(), rebuilt.size())) continue;
            if (!isValidIndex(cs.targetNodeIndex(), rebuilt.size())) continue;
            Node srcNode = rebuilt.get(cs.sourceNodeIndex()).node();
            Node tgtNode = rebuilt.get(cs.targetNodeIndex()).node();
            if (!isValidIndex(cs.sourcePortIndex(), srcNode.ports().size())) continue;
            if (!isValidIndex(cs.targetPortIndex(), tgtNode.ports().size())) continue;
            Port srcPort = srcNode.ports().get(cs.sourcePortIndex());
            Port tgtPort = tgtNode.ports().get(cs.targetPortIndex());
            canvas.addConnection(new Connection(srcNode, srcPort, tgtNode, tgtPort, cs.color()));
        }
    }

    /**
     * Walks {@code node}'s declared properties and encodes each
     * currently-set value via the property type's codec, packaged as a
     * {@link Dynamic} so {@link NodeState}'s {@code Codec.PASSTHROUGH}
     * field can carry it through whichever ops the canvas codec is
     * later run over.
     *
     * <p>{@link NbtOps#INSTANCE} is used as the encoding ops here because
     * every currently-registered property type round-trips through NBT
     * cleanly without needing a registry-aware ops. If a future property
     * type carries registry references in its codec, this site is where
     * we'd thread a {@code RegistryOps<Tag>} through instead.
     */
    private static Map<String, Dynamic<?>> encodePropertyValues(Node node) {
        Map<String, Dynamic<?>> encoded = new HashMap<>();
        for (PortDefinition prop : node.definition().properties()) {
            Object value = node.propertyValue(prop.name());
            if (value == null) continue;
            // Type-erased encode: the property definition's codec is
            // Codec<?>, but Java's wildcard can't be captured here. The
            // cast is safe so long as the caller put a value of the right
            // runtime type into the property map — same trust contract as
            // the registered PropertyDefinition's default.
            @SuppressWarnings({"unchecked", "rawtypes"})
            DataResult<Tag> result = ((Codec) prop.type().value().codec())
                    .encodeStart(NbtOps.INSTANCE, value);
            result.result().ifPresent(tag -> encoded.put(prop.name(), new Dynamic<>(NbtOps.INSTANCE, tag)));
        }
        return encoded;
    }

    /**
     * Inverse of {@link #encodePropertyValues}. For each entry in the
     * saved-state map, look up the matching property definition, decode
     * the {@link Dynamic} via the property type's codec, and write the
     * typed result through {@link Node#setPropertyValue}. Entries whose
     * keys don't match any property in the current definition, or whose
     * values fail to decode, are skipped silently — schema evolution
     * shouldn't crash a load.
     */
    private static void applyPropertyValues(Node node, Map<String, Dynamic<?>> stored) {
        if (stored.isEmpty()) return;
        Map<String, PortDefinition> byName = new HashMap<>();
        for (PortDefinition prop : node.definition().properties()) {
            byName.put(prop.name(), prop);
        }
        for (Map.Entry<String, Dynamic<?>> entry : stored.entrySet()) {
            PortDefinition prop = byName.get(entry.getKey());
            if (prop == null) continue;
            @SuppressWarnings({"unchecked", "rawtypes"})
            DataResult<?> decoded = ((Codec) prop.type().value().codec())
                    .parse(entry.getValue());
            decoded.result().ifPresent(value -> node.setPropertyValue(prop.name(), value));
        }
    }

    private static boolean isValidIndex(int idx, int size) {
        return idx >= 0 && idx < size;
    }
}

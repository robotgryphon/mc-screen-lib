package dev.robotgryphon.screenlib.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import dev.robotgryphon.screenlib.client.ui.widget.Connection;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.types.PortDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                    encodePropertyValues(node),
                    Optional.ofNullable(node.tintColor())));
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
            // Names are written alongside indices so a future load can
            // re-resolve the connection even if the node's port order
            // has changed (e.g., a property added / removed between
            // save and load shifted "Storage" from index 2 to index 1).
            // Indices remain as a fallback for ports whose names happen
            // to collide or have shifted, but the name lookup wins.
            connectionStates.add(new ConnectionState(
                    srcIdx, srcPortIdx, Optional.of(portLocalName(c.sourcePort())),
                    tgtIdx, tgtPortIdx, Optional.of(portLocalName(c.targetPort())),
                    c.color()));
        }

        return new CanvasState(nodeStates, connectionStates);
    }

    /**
     * Local identifier for a port — the string the
     * {@link ConnectionState} serializes so load can re-resolve the
     * port even if its index in {@link Node#ports()} has shifted.
     * Regular ports use their visible title (the same string the
     * datapack's {@code "name"} JSON field carries — declared inputs
     * have their port {@code title} built from that name verbatim);
     * property ports use their bound property name. The two namespaces
     * don't collide in practice — schemas use capitalized labels for
     * regular ports ("Position", "Storage") and lower-case identifiers
     * for property names ("value", "seed") — and even if they did, the
     * side check on load disambiguates.
     */
    private static String portLocalName(Port port) {
        if (port.isProperty()) {
            return port.propertyName();
        }
        return port.title().getString();
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
     *   <li>For each {@link ConnectionState}, resolve the source / target
     *       ports against the rebuilt graph — preferring the saved port
     *       names (so a node-definition refactor that reordered or
     *       added / removed ports doesn't relocate the wire to a
     *       different port at the same index), falling back to the
     *       saved indices for legacy saves that pre-date the name
     *       fields. Each rehydrated connection is then re-validated
     *       (source must be RIGHT, target must be LEFT, port types
     *       must match) the same way {@link Canvas#connect} validates
     *       at edit time; failures are dropped silently so a stale
     *       wire from a previous schema doesn't poison the loaded
     *       graph.</li>
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
            // Restore any persisted tint color. {@link Optional#empty()}
            // leaves the node at the default coloring — the absent
            // {@code tint} field on legacy data lands here unchanged.
            ns.tintColor().ifPresent(c -> widget.node().setTintColor(c));
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

            Port srcPort = resolvePort(srcNode, cs.sourcePortName(), cs.sourcePortIndex());
            Port tgtPort = resolvePort(tgtNode, cs.targetPortName(), cs.targetPortIndex());
            if (srcPort == null || tgtPort == null) continue;

            // Same validation as {@link Canvas#connect}: source must be
            // a RIGHT-side output, target must be a LEFT-side input,
            // and both ports must agree on the registry-resolved type.
            // A schema change between save and load that breaks any of
            // these (e.g., a port flipped from input to property) gets
            // the wire silently dropped here, so the loaded graph
            // never carries an invalid connection.
            if (srcPort.side() != PortSide.RIGHT) continue;
            if (tgtPort.side() != PortSide.LEFT) continue;
            if (srcPort.type().value() != tgtPort.type().value()) continue;

            canvas.addConnection(new Connection(srcNode, srcPort, tgtNode, tgtPort, cs.color()));
        }
    }

    /**
     * Resolves a saved port reference to a live {@link Port} on
     * {@code node}. Prefers name lookup — the durable identifier
     * across node-definition changes — and falls back to the saved
     * index when the name is absent (legacy save) or no port on the
     * current node carries that name. Returns {@code null} when both
     * lookups fail, signaling the connection should be dropped.
     *
     * <p>Name lookup keys off {@link Port#title()} for regular ports
     * and {@link Port#propertyName()} for property ports, matching the
     * encoding {@link #portLocalName} uses on save.
     */
    private static @Nullable Port resolvePort(Node node,
                                              Optional<String> savedName,
                                              int savedIndex) {
        if (savedName.isPresent()) {
            String name = savedName.get();
            for (Port port : node.ports()) {
                String portName = port.isProperty()
                        ? port.propertyName()
                        : port.title().getString();
                if (name.equals(portName)) {
                    return port;
                }
            }
            // Saved name doesn't match anything on the current
            // definition — fall through to the index lookup. Covers
            // the case where a port was renamed but kept structurally
            // the same; if THAT also fails the connection is dropped.
        }
        if (isValidIndex(savedIndex, node.ports().size())) {
            return node.ports().get(savedIndex);
        }
        return null;
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

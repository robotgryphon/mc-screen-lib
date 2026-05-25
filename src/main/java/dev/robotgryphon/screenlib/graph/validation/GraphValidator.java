package dev.robotgryphon.screenlib.graph.validation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.robotgryphon.screenlib.client.ui.widget.Connection;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.Node;
import dev.robotgryphon.screenlib.graph.Port;
import dev.robotgryphon.screenlib.graph.PortSide;
import dev.robotgryphon.screenlib.types.PortDefinition;
import dev.robotgryphon.screenlib.types.PropertyDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class GraphValidator {

    private GraphValidator() {}

    public static DataResult<Node> validateNode(Node node) {
        List<String> errors = new ArrayList<>();
        collectNodeErrors(node, errors);
        return buildResult(node, errors, "Node");
    }

    /**
     * Per-node validation that also takes the surrounding graph into
     * account — runs the same property checks as {@link #validateNode}
     * and additionally flags any required (non-optional, non-property)
     * input port on {@code node} that has no inbound connection on
     * {@code canvas}. Cross-node connection structure (side, type
     * match, single-inbound) isn't re-checked here because those are
     * graph-wide invariants the canvas's mutation path already
     * enforces; this method exists for the "is THIS node currently
     * usable" query that {@link Node#validate()} pushes onto the
     * canvas after every relevant change.
     */
    public static DataResult<Node> validateNodeInGraph(Node node, Canvas canvas) {
        List<String> errors = new ArrayList<>();
        collectNodeErrors(node, errors);
        collectRequiredInputErrors(node, canvas.connections(), errors);
        collectRequiredPropertyErrors(node, canvas.connections(), errors);
        collectIncomingWireValueErrors(node, canvas, errors);
        return buildResult(node, errors, "Node");
    }

    public static DataResult<Canvas> validateCanvas(Canvas canvas) {
        List<String> errors = new ArrayList<>();

        Set<Node> nodesOnCanvas = Collections.newSetFromMap(new IdentityHashMap<>());
        for (NodeWidget widget : canvas.nodes()) {
            nodesOnCanvas.add(widget.node());
        }

        for (NodeWidget widget : canvas.nodes()) {
            collectNodeErrors(widget.node(), errors);
        }

        Set<Port> seenTargetPorts = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Connection connection : canvas.connections()) {
            collectConnectionErrors(connection, nodesOnCanvas, seenTargetPorts, errors);
        }

        for (NodeWidget widget : canvas.nodes()) {
            collectRequiredInputErrors(widget.node(), canvas.connections(), errors);
            collectRequiredPropertyErrors(widget.node(), canvas.connections(), errors);
            collectIncomingWireValueErrors(widget.node(), canvas, errors);
        }

        return buildResult(canvas, errors, "Canvas");
    }

    private static void collectNodeErrors(Node node, List<String> errors) {
        String nodeTag = nodeTag(node);
        for (PortDefinition prop : node.definition().properties()) {
            PropertyDefinition<?> def = prop.type().value();
            Object value = node.propertyValue(prop.name());
            if (value == null) continue;

            DataResult<Tag> encoded = encodeProperty(def, value);
            if (encoded.error().isPresent()) {
                errors.add(nodeTag + " property '" + prop.name()
                        + "' value is not assignable to its declared type ("
                        + encoded.error().get().message() + ")");
                continue;
            }

            if (def.allowedValues().isPresent()
                    && !def.allowedValues().get().contains(value)) {
                errors.add(nodeTag + " property '" + prop.name()
                        + "' value " + value
                        + " is not one of the declared allowed values "
                        + def.allowedValues().get());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DataResult<Tag> encodeProperty(PropertyDefinition<?> def, Object value) {
        Codec codec = def.codec();
        try {
            return codec.encodeStart(NbtOps.INSTANCE, value);
        } catch (ClassCastException ex) {
            return DataResult.error(ex::getMessage);
        }
    }

    private static void collectConnectionErrors(Connection connection,
                                                Set<Node> nodesOnCanvas,
                                                Set<Port> seenTargetPorts,
                                                List<String> errors) {
        Node source = connection.source();
        Node target = connection.target();
        Port sourcePort = connection.sourcePort();
        Port targetPort = connection.targetPort();
        String tag = connectionTag(connection);

        if (!nodesOnCanvas.contains(source)) {
            errors.add(tag + " source node is not present on the canvas");
            return;
        }
        if (!nodesOnCanvas.contains(target)) {
            errors.add(tag + " target node is not present on the canvas");
            return;
        }
        if (!containsByIdentity(source.ports(), sourcePort)) {
            errors.add(tag + " source port does not belong to the declared source node");
            return;
        }
        if (!containsByIdentity(target.ports(), targetPort)) {
            errors.add(tag + " target port does not belong to the declared target node");
            return;
        }

        if (sourcePort.side() != PortSide.RIGHT) {
            errors.add(tag + " source port is not on the RIGHT side");
        }
        if (targetPort.side() != PortSide.LEFT) {
            errors.add(tag + " target port is not on the LEFT side");
        }
        if (sourcePort.type().value() != targetPort.type().value()) {
            errors.add(tag + " port types do not match");
        }
        if (!seenTargetPorts.add(targetPort)) {
            errors.add(tag + " target input has more than one inbound connection");
        }
    }

    private static void collectRequiredInputErrors(Node node,
                                                   List<Connection> connections,
                                                   List<String> errors) {
        String nodeTag = nodeTag(node);
        for (Port port : node.ports()) {
            if (port.side() != PortSide.LEFT) continue;
            if (port.isProperty()) continue;
            if (port.optional()) continue;

            boolean wired = false;
            for (Connection connection : connections) {
                if (connection.targetPort() == port) {
                    wired = true;
                    break;
                }
            }
            if (!wired) {
                errors.add(nodeTag + " required input '"
                        + port.title().getString() + "' has no inbound connection");
            }
        }
    }

    /**
     * Flags non-optional properties on {@code node} that don't have a
     * value sourced for them — meaning the local property slot is
     * empty AND no wire targets that property's input port. A
     * property whose {@link
     * dev.robotgryphon.screenlib.types.PropertyDefinition} carries a
     * default ends up with a non-null value during node construction
     * (see {@code Node.seedDefaultPropertyValues}), so most properties
     * naturally satisfy this check; the rule mainly catches properties
     * whose type registers no default and which the user hasn't filled
     * in yet. Marking a property optional via {@link
     * dev.robotgryphon.screenlib.types.PortDefinition#optional}
     * exempts it.
     */
    /**
     * Flags properties on {@code node} whose inbound wire is delivering
     * a value that lies outside the property's declared
     * {@code allowedValues} set. The wire's type identity check at
     * connect time guarantees source and target share a
     * {@link PropertyDefinition}, so the allowed-values list is the
     * same on both sides — but the upstream node's <em>stored</em>
     * property value can still drift outside that list (a stale
     * persisted value from before the schema added the constraint, a
     * setter that didn't enforce membership, etc.). When the wire
     * resolves to a value the receiving end's PropertyDefinition
     * rejects, the receiving node is flagged so the failure surfaces
     * at the point the bad value is being consumed, not just at the
     * upstream where it's stored.
     *
     * <p>{@link Canvas#resolveUpstreamValue} performs the chain walk
     * (recursing through linked-property outputs whose own property
     * may itself be driven by yet another wire), so this check sees
     * the same value the rest of the runtime would deliver. A null
     * resolve result is treated as "no value to check" — that's a
     * "no value" condition, not a "wrong value" one, and the required-
     * property check covers it separately.
     */
    private static void collectIncomingWireValueErrors(Node node,
                                                       Canvas canvas,
                                                       List<String> errors) {
        String nodeTag = nodeTag(node);
        for (PortDefinition prop : node.definition().properties()) {
            PropertyDefinition<?> def = prop.type().value();
            if (def.allowedValues().isEmpty()) continue;

            Connection inbound = null;
            for (Connection connection : canvas.connections()) {
                Port target = connection.targetPort();
                if (target.node() != node) continue;
                if (!target.isProperty()) continue;
                if (prop.name().equals(target.propertyName())) {
                    inbound = connection;
                    break;
                }
            }
            if (inbound == null) continue;

            Object delivered = canvas.resolveUpstreamValue(inbound);
            if (delivered == null) continue;

            if (!def.allowedValues().get().contains(delivered)) {
                errors.add(nodeTag + " property '" + prop.name()
                        + "' is driven by a connection delivering "
                        + delivered + ", which is not one of the declared allowed values "
                        + def.allowedValues().get());
            }
        }
    }

    private static void collectRequiredPropertyErrors(Node node,
                                                      List<Connection> connections,
                                                      List<String> errors) {
        String nodeTag = nodeTag(node);
        for (PortDefinition prop : node.definition().properties()) {
            if (prop.optional()) continue;
            if (node.propertyValue(prop.name()) != null) continue;

            boolean wired = false;
            for (Connection connection : connections) {
                Port target = connection.targetPort();
                if (target.node() != node) continue;
                if (!target.isProperty()) continue;
                if (prop.name().equals(target.propertyName())) {
                    wired = true;
                    break;
                }
            }
            if (!wired) {
                errors.add(nodeTag + " required property '"
                        + prop.name() + "' has no value and no inbound wire");
            }
        }
    }

    private static boolean containsByIdentity(List<Port> ports, Port target) {
        for (Port p : ports) {
            if (p == target) return true;
        }
        return false;
    }

    private static String nodeTag(Node node) {
        return "node '" + node.title().getString() + "'";
    }

    private static String connectionTag(Connection connection) {
        String src = connection.source().title().getString()
                + "." + connection.sourcePort().title().getString();
        String tgt = connection.target().title().getString()
                + "." + connection.targetPort().title().getString();
        return "connection " + src + " -> " + tgt;
    }

    private static <T> DataResult<T> buildResult(T value, List<String> errors, String label) {
        if (errors.isEmpty()) {
            return DataResult.success(value);
        }
        String joined = String.join("; ", errors);
        return DataResult.error(() -> label + " validation failed: " + joined, value);
    }
}

package dev.robotgryphon.screenlib.graph.validation;

import dev.robotgryphon.screenlib.graph.ports.PropertyInputPort;
import dev.robotgryphon.screenlib.graph.ports.StandardInputPort;
import dev.robotgryphon.screenlib.graph.properties.NodeProperty;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.robotgryphon.screenlib.client.ui.widget.Connection;
import dev.robotgryphon.screenlib.graph.Canvas;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.ports.Port;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public final class GraphValidator {
    private GraphValidator() {}

    public static DataResult<Node> validateNodeInGraph(Node node, Canvas canvas) {
        List<String> errors = new ArrayList<>();
        collectNodeErrors(node, errors);
        collectRequiredInputErrors(node, canvas.connections(), errors);
        collectRequiredPropertyErrors(node, canvas.connections(), errors);
        collectIncomingWireValueErrors(node, canvas, errors);
        return buildResult(node, errors, "Node");
    }

    private static void collectNodeErrors(Node node, List<String> errors) {
        String nodeTag = nodeTag(node);
        for (NodeProperty<?> prop : node.propertyManager().properties()) {
            GraphDataType<?> def = prop.type().value();
            Object value = node.propertyManager().value(prop.name());
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
    private static DataResult<Tag> encodeProperty(GraphDataType<?> def, Object value) {
        Codec codec = def.codec();
        try {
            return codec.encodeStart(NbtOps.INSTANCE, value);
        } catch (ClassCastException ex) {
            return DataResult.error(ex::getMessage);
        }
    }

    private static void collectRequiredInputErrors(Node node,
                                                   List<Connection> connections,
                                                   List<String> errors) {
        String nodeTag = nodeTag(node);
        for (Port port : node.portManager().ports()) {
            if (!(port instanceof StandardInputPort input) || input.optional()) continue;

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

    private static void collectIncomingWireValueErrors(Node node,
                                                       Canvas canvas,
                                                       List<String> errors) {
        String nodeTag = nodeTag(node);
        for (NodeProperty<?> prop : node.propertyManager().properties()) {
            GraphDataType<?> def = prop.type().value();
            if (def.allowedValues().isEmpty()) continue;

            Connection inbound = null;
            for (Connection connection : canvas.connections()) {
                Port target = connection.targetPort();
                if (target.node() != node) continue;
                if (!(target instanceof PropertyInputPort propertyPort)) continue;
                if (prop.name().equals(propertyPort.propertyName())) {
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
        for (NodeProperty<?> prop : node.propertyManager().properties()) {
            if (node.propertyManager().value(prop.name()) != null) continue;

            boolean wired = false;
            for (Connection connection : connections) {
                Port target = connection.targetPort();
                if (target.node() != node) continue;
                if (!(target instanceof PropertyInputPort propertyPort)) continue;
                if (prop.name().equals(propertyPort.propertyName())) {
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

    private static String nodeTag(Node node) {
        return "node '" + node.title.getString() + "'";
    }

    private static <T> DataResult<T> buildResult(T value, List<String> errors, String label) {
        if (errors.isEmpty()) {
            return DataResult.success(value);
        }
        String joined = String.join("; ", errors);
        return DataResult.error(() -> label + " validation failed: " + joined, value);
    }
}

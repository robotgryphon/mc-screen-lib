package dev.robotgryphon.screenlib.graph.validation;

import dev.robotgryphon.screenlib.graph.ports.PropertyInputPort;
import dev.robotgryphon.screenlib.graph.schema.node.StoredPropertyDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.robotgryphon.screenlib.graph.Connection;
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

    public static List<NodeValidity> validate(Canvas canvas) {
        List<NodeValidity> results = new ArrayList<>();
        List<Node> nodes = canvas.nodes();
        for (int i = 0; i < nodes.size(); i++) {
            List<String> errors = errors(nodes.get(i));
            if (!errors.isEmpty()) {
                results.add(new NodeValidity(i, errors));
            }
        }
        return results;
    }

    public static List<String> errors(Node node) {
        Canvas canvas = node.canvas;
        List<String> errors = new ArrayList<>();
        collectNodeErrors(node, errors);
        collectRequiredPropertyErrors(node, canvas.connections(), errors);
        collectIncomingWireValueErrors(node, canvas, errors);
        return errors;
    }

    private static void collectNodeErrors(Node node, List<String> errors) {
        String nodeTag = nodeTag(node);
        for (StoredPropertyDefinition<?> prop : node.propertyManager().stored()) {
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

            var allowed = node.canvas.allowedValues(prop.anyType());
            if (allowed.isPresent() && !allowed.get().contains(value)) {
                errors.add(nodeTag + " property '" + prop.name()
                        + "' value " + value
                        + " is not one of the declared allowed values "
                        + allowed.get());
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

    private static void collectIncomingWireValueErrors(Node node,
                                                       Canvas canvas,
                                                       List<String> errors) {
        String nodeTag = nodeTag(node);
        for (StoredPropertyDefinition<?> prop : node.propertyManager().stored()) {
            GraphDataType<?> def = prop.type().value();
            var allowed = canvas.allowedValues(prop.anyType());
            if (allowed.isEmpty()) continue;

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

            if (!allowed.get().contains(delivered)) {
                errors.add(nodeTag + " property '" + prop.name()
                        + "' is driven by a connection delivering "
                        + delivered + ", which is not one of the declared allowed values "
                        + allowed.get());
            }
        }
    }

    private static void collectRequiredPropertyErrors(Node node,
                                                      List<Connection> connections,
                                                      List<String> errors) {
        String nodeTag = nodeTag(node);
        for (StoredPropertyDefinition<?> prop : node.propertyManager().stored()) {
            if (prop.optional()) continue;
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
}

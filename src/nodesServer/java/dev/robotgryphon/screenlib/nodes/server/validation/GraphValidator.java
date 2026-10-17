package dev.robotgryphon.screenlib.nodes.server.validation;

import dev.robotgryphon.screenlib.nodes.common.sync.NodeValidity;
import dev.robotgryphon.screenlib.graph.schema.properties.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.nodes.common.canvas.CanvasGraph;
import dev.robotgryphon.screenlib.nodes.common.Node;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;

import java.util.ArrayList;
import java.util.List;

public final class GraphValidator {
    private GraphValidator() {}

    public static List<NodeValidity> validate(CanvasGraph canvas) {
        List<NodeValidity> results = new ArrayList<>();
        for (Node node : canvas.nodes()) {
            List<String> errors = errors(node);
            if (!errors.isEmpty()) {
                results.add(new NodeValidity(node.id(), errors));
            }
        }
        return results;
    }

    public static List<String> errors(Node node) {
        CanvasGraph canvas = node.canvas;
        List<String> errors = new ArrayList<>();
        collectNodeErrors(node, errors);
        collectRequiredPropertyErrors(node, errors);
        collectIncomingWireValueErrors(node, canvas, errors);
        return errors;
    }

    private static void collectNodeErrors(Node node, List<String> errors) {
        String nodeTag = nodeTag(node);
        for (StoredPropertyDefinition<?> prop : node.propertyManager().stored()) {
            GraphDataType<?> def = prop.type();
            Object value = node.propertyManager().value(prop.name());
            if (value == null) continue;

            if (!def.dataType().isInstance(value)) {
                errors.add(nodeTag + " property '" + prop.name()
                        + "' value is not assignable to its declared type");
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

    private static void collectIncomingWireValueErrors(Node node,
                                                       CanvasGraph canvas,
                                                       List<String> errors) {
        String nodeTag = nodeTag(node);
        for (StoredPropertyDefinition<?> prop : node.propertyManager().stored()) {
            GraphDataType<?> def = prop.type();
            var allowed = canvas.allowedValues(prop.anyType());
            if (allowed.isEmpty()) continue;

            if (!node.propertyManager().isDriven(prop)) continue;

            Object delivered = node.propertyManager().resolve(prop).orElse(null);
            if (delivered == null) continue;

            if (!allowed.get().contains(delivered)) {
                errors.add(nodeTag + " property '" + prop.name()
                        + "' is driven by a connection delivering "
                        + delivered + ", which is not one of the declared allowed values "
                        + allowed.get());
            }
        }
    }

    private static void collectRequiredPropertyErrors(Node node, List<String> errors) {
        String nodeTag = nodeTag(node);
        for (StoredPropertyDefinition<?> prop : node.propertyManager().stored()) {
            if (prop.optional()) continue;
            if (node.propertyManager().value(prop.name()) != null) continue;

            if (!node.propertyManager().isDriven(prop)) {
                errors.add(nodeTag + " required property '"
                        + prop.name() + "' has no value and no inbound wire");
            }
        }
    }

    private static String nodeTag(Node node) {
        return "node '" + node.title.getString() + "'";
    }
}

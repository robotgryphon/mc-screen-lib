package dev.robotgryphon.screenlib.graph.validation;

import dev.robotgryphon.screenlib.graph.sync.NodeValidity;

import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.robotgryphon.screenlib.graph.canvas.CanvasGraph;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public final class GraphValidator {
    private GraphValidator() {}

    public static List<NodeValidity> validate(CanvasGraph canvas) {
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
                                                       CanvasGraph canvas,
                                                       List<String> errors) {
        String nodeTag = nodeTag(node);
        for (StoredPropertyDefinition<?> prop : node.propertyManager().stored()) {
            GraphDataType<?> def = prop.type().value();
            var allowed = canvas.allowedValues(prop.anyType());
            if (allowed.isEmpty()) continue;

            if (!node.propertyManager().isDriven(prop.name())) continue;

            Object delivered = node.propertyManager().resolve(prop.name()).orElse(null);
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

            if (!node.propertyManager().isDriven(prop.name())) {
                errors.add(nodeTag + " required property '"
                        + prop.name() + "' has no value and no inbound wire");
            }
        }
    }

    private static String nodeTag(Node node) {
        return "node '" + node.title.getString() + "'";
    }
}

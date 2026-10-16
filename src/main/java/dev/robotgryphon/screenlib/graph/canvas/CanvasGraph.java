package dev.robotgryphon.screenlib.graph.canvas;

import dev.robotgryphon.screenlib.graph.nodes.properties.schema.PropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.graph.Connection;
import dev.robotgryphon.screenlib.graph.nodes.ports.PortSide;
import dev.robotgryphon.screenlib.graph.state.CanvasState;
import dev.robotgryphon.screenlib.graph.state.ConnectionState;
import dev.robotgryphon.screenlib.graph.state.NodeState;
import dev.robotgryphon.screenlib.graph.ConnectionManager;
import dev.robotgryphon.screenlib.graph.nodes.NodeReference;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.properties.AllowedValues;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CanvasGraph {
    private final List<Node> nodes = new ArrayList<>();

    private final AllowedValues allowedValues;

    private final ConnectionManager connections;

    private PropertyListener propertyListener = CanvasGraph::ignoreProperty;

    public CanvasGraph(AllowedValues allowedValues) {
        this.allowedValues = allowedValues;
        this.connections = new ConnectionManager(allowedValues);
    }

    public void onPropertyChanged(PropertyListener listener) {
        this.propertyListener = listener;
    }

    public <T> void propertyChanged(Node node, StoredPropertyDefinition<T> property, Optional<T> value) {
        this.propertyListener.changed(node, property, value);
    }

    private static <T> void ignoreProperty(Node node, StoredPropertyDefinition<T> property, Optional<T> value) {
    }

    public Optional<Collection<Object>> allowedValues(Holder<GraphDataType<?>> type) {
        return this.allowedValues.of(type);
    }

    public <T> Optional<List<T>> allowedValues(PropertyDefinition<T> property) {
        Class<T> type = property.type().dataType();
        return this.allowedValues.of(property.anyType())
                .map(values -> values.stream().filter(type::isInstance).map(type::cast).toList());
    }

    public Node addNode(Node node) {
        if (node.canvas != this) {
            throw new IllegalArgumentException("Node belongs to a different canvas: " + node);
        }
        this.nodes.add(node);
        return node;
    }

    public List<Node> nodes() {
        return Collections.unmodifiableList(this.nodes);
    }

    public Optional<Node> node(NodeReference reference) {
        for (Node node : this.nodes) {
            if (node.id().equals(reference.id()) && node.type().equals(reference.type())) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    public ConnectionManager connections() {
        return this.connections;
    }

    public void removeNode(Node removed) {
        if (!this.nodes.remove(removed)) {
            return;
        }
        this.connections.disconnect(removed);
    }

    public void load(CanvasState state) {
        this.clear();
        Map<UUID, Node> byId = new HashMap<>();
        for (NodeState saved : state.nodes()) {
            if (byId.containsKey(saved.id())) {
                continue;
            }
            Node node = new Node(this, saved.id(), saved.definition(), Node.defaultTitle(saved.definition().key()));
            node.propertyManager().load(saved.propertyValues());
            byId.put(saved.id(), node);
            this.addNode(node);
        }
        for (ConnectionState saved : state.connections()) {
            Node source = byId.get(saved.sourceNode());
            Node target = byId.get(saved.targetNode());
            if (source == null || target == null) {
                continue;
            }
            var sourcePort = source.portManager().port(PortSide.RIGHT, saved.sourcePort());
            var targetPort = target.portManager().port(PortSide.LEFT, saved.targetPort());
            if (sourcePort.isEmpty() || targetPort.isEmpty()
                    || sourcePort.get().type().value() != targetPort.get().type().value()) {
                continue;
            }
            this.connections.add(new Connection(source, sourcePort.get(), target, targetPort.get()));
        }
    }

    public void clear() {
        this.nodes.clear();
        this.connections.clear();
    }

    @FunctionalInterface
    public interface PropertyListener {
        <T> void changed(Node node, StoredPropertyDefinition<T> property, Optional<T> value);
    }
}

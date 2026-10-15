package dev.robotgryphon.screenlib.graph.canvas;

import dev.robotgryphon.screenlib.graph.ConnectionManager;
import dev.robotgryphon.screenlib.graph.nodes.NodeReference;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.properties.AllowedValues;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CanvasGraph {
    private final List<Node> nodes = new ArrayList<>();

    private final AllowedValues allowedValues;

    private final ConnectionManager connections;

    private int revision;

    public CanvasGraph(AllowedValues allowedValues) {
        this.allowedValues = allowedValues;
        this.connections = new ConnectionManager(allowedValues, this::touch);
    }

    public Optional<Collection<Object>> allowedValues(Holder<GraphDataType<?>> type) {
        return this.allowedValues.of(type);
    }

    public Node addNode(Node node) {
        if (node.canvas != this) {
            throw new IllegalArgumentException("Node belongs to a different canvas: " + node);
        }
        this.nodes.add(node);
        this.touch();
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

    public int revision() {
        return this.revision;
    }

    public void touch() {
        this.revision++;
    }

    public void removeNode(Node removed) {
        if (!this.nodes.remove(removed)) {
            return;
        }
        this.connections.disconnect(removed);
        this.touch();
    }

    public void clear() {
        this.nodes.clear();
        this.connections.clear();
        this.touch();
    }
}

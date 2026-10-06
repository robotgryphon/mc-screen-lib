package dev.robotgryphon.screenlib.graph;

import com.mojang.serialization.DataResult;
import dev.robotgryphon.screenlib.client.ui.widget.Connection;
import dev.robotgryphon.screenlib.client.ui.widget.NodeWidget;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.NodeDefinition;
import dev.robotgryphon.screenlib.graph.ports.*;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.joml.Vector2dc;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class Canvas {
    public static final int IN_FLIGHT_COLOR = 0xFFFFD24A;

    private final List<NodeWidget> nodes = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();

    private final Map<Node, Vector2i> positions = new IdentityHashMap<>();
    private final Map<Node, DataResult<Node>> validationResults = new IdentityHashMap<>();
    private final Map<ResourceKey<NodeDefinition>, Holder<NodeDefinition>> definitions = new IdentityHashMap<>();

    private @Nullable Holder<GraphDataType<?>> activeDragType;

    public void place(Node node, int x, int y) {
        this.positions.put(node, new Vector2i(x, y));
    }

    public int x(Node node) {
        return this.positions.get(node).x;
    }

    public int y(Node node) {
        return this.positions.get(node).y;
    }

    public void register(Holder<NodeDefinition> definition) {
        this.definitions.put(definition.key(), definition);
    }

    public Holder<NodeDefinition> definitionHolder(Node node) {
        Holder<NodeDefinition> holder = this.definitions.get(node);
        if (holder == null) {
            throw new IllegalArgumentException("Node is not registered on this canvas: " + node);
        }
        return holder;
    }

    public NodeDefinition definition(Node node) {
        return this.definitionHolder(node).value();
    }

    public NodeWidget addNode(NodeWidget widget) {
        if (widget.node().canvas != this) {
            throw new IllegalArgumentException("Node belongs to a different canvas: " + widget.node());
        }
        this.nodes.add(widget);
        return widget;
    }

    public NodeWidget widgetFor(Node node) {
        for (NodeWidget widget : this.nodes) {
            if (widget.node() == node) {
                return widget;
            }
        }
        throw new IllegalArgumentException("Node is not on this canvas: " + node);
    }

    public List<NodeWidget> nodes() {
        return Collections.unmodifiableList(this.nodes);
    }

    public List<Connection> connections() {
        return Collections.unmodifiableList(this.connections);
    }

    public void submitValidity(Node node, DataResult<Node> result) {
        if (result.error().isPresent()) {
            this.validationResults.put(node, result);
        } else {
            this.validationResults.remove(node);
        }
    }

    public boolean isInvalid(Node node) {
        return this.validationResults.containsKey(node);
    }

    public void revalidateDownstreamOf(Node source) {
        Set<Node> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        visited.add(source);
        revalidateDownstreamOfImpl(source, visited);
    }

    private void revalidateDownstreamOfImpl(Node source, Set<Node> visited) {
        for (Connection c : this.connections) {
            if (c.source() != source) continue;
            Node target = c.target();
            if (!visited.add(target)) continue;
            target.validate();
            revalidateDownstreamOfImpl(target, visited);
        }
    }

    public @Nullable Holder<GraphDataType<?>> activeDragType() {
        return this.activeDragType;
    }

    public void setActiveDragType(@Nullable Holder<GraphDataType<?>> type) {
        this.activeDragType = type;
    }

    public Stream<Port> findPortsNear(Vector2dc point) {
        var s = Stream.<Port>builder();
        for (int i = this.nodes.size() - 1; i >= 0; i--) {
            NodeWidget node = this.nodes.get(i);
            Port port = node.portAt(point.x(), point.y());
            if (port != null)
                s.add(port);
        }

        return s.build();
    }

    public Stream<NodeWidget> findNodesNear(Vector2dc point) {
        var s = Stream.<NodeWidget>builder();
        for (int i = this.nodes.size() - 1; i >= 0; i--) {
            NodeWidget node = this.nodes.get(i);
            if (node.isMouseOver(point.x(), point.y())) {
                final var maybePort = node.portAt(point.x(), point.y());
                if(maybePort == null)
                    s.add(node);
            }
        }

        return s.build();
    }

    public boolean connect(Port from, Port to, int color) {
        if (from.side() != PortSide.RIGHT || to.side() != PortSide.LEFT) {
            return false;
        }
        if (from.type().value() != to.type().value()) {
            return false;
        }
        for (Connection existing : List.copyOf(this.connections)) {
            if (existing.targetPort().equals(to)) {
                detach(existing);
            }
        }
        attach(new Connection(from.node(), from, to.node(), to, color));
        to.node().validate();
        revalidateDownstreamOf(to.node());
        return true;
    }

    public void removeConnection(Connection connection) {
        captureUpstreamIntoProperty(connection);
        detach(connection);
        connection.target().validate();
        revalidateDownstreamOf(connection.target());
    }

    private void captureUpstreamIntoProperty(Connection connection) {
        if (!(connection.targetPort() instanceof PropertyInputPort targetPort)) return;
        String propName = targetPort.propertyName();

        Object value = resolveUpstreamValue(connection);
        if (value == null) return;

        GraphDataType<?> def = targetPort.type().value();
        if (def.allowedValues().isPresent()
                && !def.allowedValues().get().contains(value)) {
            return;
        }
        connection.target().propertyManager().set(propName, value);
    }

    public @Nullable Object resolveUpstreamValue(Connection connection) {
        return resolveUpstreamValue(connection, new java.util.HashSet<>());
    }

    private @Nullable Object resolveUpstreamValue(Connection connection,
                                                  java.util.Set<Port> visited) {
        Port source = connection.sourcePort();
        if (!visited.add(source)) {
            return null;
        }
        return switch (source) {
            case PropertyOutputPort relay -> {
                Node sourceNode = relay.node();
                Connection upstream = findInboundPropertyConnection(sourceNode, relay.propertyName());
                yield upstream != null
                        ? resolveUpstreamValue(upstream, visited)
                        : sourceNode.propertyManager().value(relay.propertyName());
            }
            case ConstantOutputPort constant -> constant.value();
            case StandardOutputPort ignored -> null;
            case InputPort ignored -> null;
        };
    }

    private @Nullable Connection findInboundPropertyConnection(Node node, String propertyName) {
        return node.portManager().inboundForProperty(propertyName);
    }

    private void attach(Connection connection) {
        this.connections.add(connection);
        connection.source().portManager().attach(connection);
        connection.target().portManager().attach(connection);
    }

    private void detach(Connection connection) {
        this.connections.remove(connection);
        unregisterWithPorts(connection);
    }

    private static void unregisterWithPorts(Connection connection) {
        connection.source().portManager().detach(connection);
        connection.target().portManager().detach(connection);
    }

    public void removeNode(NodeWidget widget) {
        if (!this.nodes.remove(widget)) {
            return;
        }
        Node removed = widget.node();
        java.util.Set<Node> neighborsToRevalidate =
                Collections.newSetFromMap(new IdentityHashMap<>());
        var it = this.connections.iterator();
        while (it.hasNext()) {
            Connection c = it.next();
            boolean sourceMatch = c.source() == removed;
            boolean targetMatch = c.target() == removed;
            if (!sourceMatch && !targetMatch) continue;
            if (sourceMatch && !targetMatch) {
                captureUpstreamIntoProperty(c);
                neighborsToRevalidate.add(c.target());
            } else if (targetMatch && !sourceMatch) {
                neighborsToRevalidate.add(c.source());
            }
            it.remove();
            unregisterWithPorts(c);
        }
        this.validationResults.remove(removed);
        this.definitions.remove(removed);
        this.positions.remove(removed);
        for (Node neighbor : neighborsToRevalidate) {
            neighbor.validate();
            revalidateDownstreamOf(neighbor);
        }
    }

    public void clear() {
        this.nodes.clear();
        this.definitions.clear();
        this.positions.clear();
        for (Connection c : this.connections) {
            unregisterWithPorts(c);
        }
        this.connections.clear();
        this.validationResults.clear();
    }

    public void addConnection(Connection connection) {
        attach(connection);
        connection.target().validate();
        revalidateDownstreamOf(connection.target());
    }
}

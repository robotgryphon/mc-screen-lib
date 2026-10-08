package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.ports.*;
import dev.robotgryphon.screenlib.graph.schema.AllowedValues;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Canvas {
    private final List<Node> nodes = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();

    private final Map<Node, Vector2i> positions = new IdentityHashMap<>();
    private final Map<Node, List<String>> validity = new IdentityHashMap<>();
    private final Map<Node, Integer> tints = new IdentityHashMap<>();

    private final AllowedValues allowedValues;

    private int revision;
    private @Nullable MinecraftServer server;

    public Canvas(AllowedValues allowedValues) {
        this.allowedValues = allowedValues;
    }

    public Optional<Collection<Object>> allowedValues(Holder<GraphDataType<?>> type) {
        return this.allowedValues.of(type);
    }

    public void place(Node node, int x, int y) {
        this.positions.put(node, new Vector2i(x, y));
    }

    public int x(Node node) {
        return this.positions.get(node).x;
    }

    public int y(Node node) {
        return this.positions.get(node).y;
    }

    public @Nullable Integer tint(Node node) {
        return this.tints.get(node);
    }

    public void setTint(Node node, @Nullable Integer tint) {
        if (tint == null) {
            this.tints.remove(node);
        } else {
            this.tints.put(node, tint);
        }
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

    public List<Connection> connections() {
        return Collections.unmodifiableList(this.connections);
    }

    public @Nullable MinecraftServer server() {
        return this.server;
    }

    public void attachServer(MinecraftServer server) {
        this.server = server;
    }

    public int revision() {
        return this.revision;
    }

    public void touch() {
        this.revision++;
    }

    public boolean isInvalid(Node node) {
        return this.validity.containsKey(node);
    }

    public List<String> errors(Node node) {
        return this.validity.getOrDefault(node, List.of());
    }

    public void setValidity(Map<Node, List<String>> results) {
        this.validity.clear();
        this.validity.putAll(results);
    }

    public boolean connect(Port from, Port to) {
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
        attach(new Connection(from.node(), from, to.node(), to));
        this.touch();
        return true;
    }

    public void removeConnection(Connection connection) {
        captureUpstreamIntoProperty(connection);
        detach(connection);
        this.touch();
    }

    private void captureUpstreamIntoProperty(Connection connection) {
        if (!(connection.targetPort() instanceof PropertyInputPort targetPort)) return;
        String propName = targetPort.propertyName();

        Object value = resolveUpstreamValue(connection);
        if (value == null) return;

        var allowed = this.allowedValues(targetPort.type());
        if (allowed.isPresent() && !allowed.get().contains(value)) {
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

    public void removeNode(Node removed) {
        if (removed.isHosted() || !this.nodes.remove(removed)) {
            return;
        }
        var it = this.connections.iterator();
        while (it.hasNext()) {
            Connection c = it.next();
            boolean sourceMatch = c.source() == removed;
            boolean targetMatch = c.target() == removed;
            if (!sourceMatch && !targetMatch) continue;
            if (sourceMatch && !targetMatch) {
                captureUpstreamIntoProperty(c);
            }
            it.remove();
            unregisterWithPorts(c);
        }
        this.validity.remove(removed);
        this.positions.remove(removed);
        this.tints.remove(removed);
        this.touch();
    }

    public void clear() {
        this.nodes.clear();
        this.positions.clear();
        this.tints.clear();
        for (Connection c : this.connections) {
            unregisterWithPorts(c);
        }
        this.connections.clear();
        this.validity.clear();
        this.touch();
    }

    public void addConnection(Connection connection) {
        attach(connection);
        this.touch();
    }
}

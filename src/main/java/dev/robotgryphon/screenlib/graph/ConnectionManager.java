package dev.robotgryphon.screenlib.graph;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.ports.Port;
import dev.robotgryphon.screenlib.graph.nodes.ports.PortSide;
import dev.robotgryphon.screenlib.graph.nodes.ports.PropertyInputPort;
import dev.robotgryphon.screenlib.graph.nodes.ports.PropertyOutputPort;
import dev.robotgryphon.screenlib.graph.nodes.properties.AllowedValues;
import dev.robotgryphon.screenlib.graph.nodes.properties.NodeProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class ConnectionManager {
    private final List<Connection> connections = new ArrayList<>();
    private final AllowedValues allowedValues;
    private final Runnable onChanged;

    public ConnectionManager(AllowedValues allowedValues, Runnable onChanged) {
        this.allowedValues = allowedValues;
        this.onChanged = onChanged;
    }

    public List<Connection> all() {
        return Collections.unmodifiableList(this.connections);
    }

    public boolean isConnected(Port port) {
        for (Connection connection : this.connections) {
            if (connection.sourcePort() == port || connection.targetPort() == port) {
                return true;
            }
        }
        return false;
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
                this.detach(existing);
            }
        }
        this.attach(new Connection(from.node(), from, to.node(), to));
        this.onChanged.run();
        return true;
    }

    public void add(Connection connection) {
        this.attach(connection);
        this.onChanged.run();
    }

    public void remove(Connection connection) {
        this.captureUpstreamIntoProperty(connection);
        this.detach(connection);
        this.onChanged.run();
    }

    public void disconnect(Node node) {
        Iterator<Connection> it = this.connections.iterator();
        while (it.hasNext()) {
            Connection connection = it.next();
            boolean sourceMatch = connection.source() == node;
            boolean targetMatch = connection.target() == node;
            if (!sourceMatch && !targetMatch) continue;
            if (sourceMatch && !targetMatch) {
                this.captureUpstreamIntoProperty(connection);
            }
            it.remove();
            unbind(connection);
        }
        this.onChanged.run();
    }

    public void clear() {
        for (Connection connection : this.connections) {
            unbind(connection);
        }
        this.connections.clear();
        this.onChanged.run();
    }

    private void captureUpstreamIntoProperty(Connection connection) {
        if (!(connection.targetPort() instanceof PropertyInputPort targetPort)) return;
        String propName = targetPort.propertyName();

        Object value = connection.target().propertyManager().resolve(propName).orElse(null);
        if (value == null) return;

        var allowed = this.allowedValues.of(targetPort.type());
        if (allowed.isPresent() && !allowed.get().contains(value)) {
            return;
        }
        connection.target().propertyManager().set(propName, value);
    }

    private void attach(Connection connection) {
        this.connections.add(connection);
        if (connection.sourcePort() instanceof PropertyOutputPort source
                && connection.targetPort() instanceof PropertyInputPort target) {
            connection.target().propertyManager().bind(target.propertyName(),
                    new NodeProperty(connection.source(), source.propertyName()));
        }
    }

    private void detach(Connection connection) {
        this.connections.remove(connection);
        unbind(connection);
    }

    private static void unbind(Connection connection) {
        if (connection.targetPort() instanceof PropertyInputPort target) {
            connection.target().propertyManager().unbind(target.propertyName());
        }
    }
}

package dev.robotgryphon.screenlib.nodes.common;

import dev.robotgryphon.screenlib.nodes.common.properties.PropertyDataSource;
import dev.robotgryphon.screenlib.graph.schema.properties.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.nodes.common.ports.Port;
import dev.robotgryphon.screenlib.nodes.common.ports.PropertyInputPort;
import dev.robotgryphon.screenlib.nodes.common.ports.PropertyOutputPort;
import dev.robotgryphon.screenlib.nodes.common.properties.AllowedValues;
import dev.robotgryphon.screenlib.nodes.common.properties.NodeProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public final class ConnectionManager {
    private final List<Connection> connections = new ArrayList<>();
    private final AllowedValues allowedValues;

    public ConnectionManager(AllowedValues allowedValues) {
        this.allowedValues = allowedValues;
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
        if (!(from instanceof PropertyOutputPort source) || !(to instanceof PropertyInputPort target)) {
            return false;
        }
        if (source.type().value() != target.type().value()) {
            return false;
        }
        for (Connection existing : List.copyOf(this.connections)) {
            if (existing.targetPort().equals(target)) {
                this.detach(existing);
            }
        }
        this.attach(new Connection(source.node(), source, target.node(), target));
        return true;
    }

    public void add(Connection connection) {
        this.attach(connection);
    }

    public void remove(Connection connection) {
        this.captureUpstreamIntoProperty(connection);
        this.detach(connection);
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
    }

    public void clear() {
        for (Connection connection : this.connections) {
            unbind(connection);
        }
        this.connections.clear();
    }

    private void captureUpstreamIntoProperty(Connection connection) {
        this.capture(connection.target(), connection.targetPort().property());
    }

    private <T> void capture(Node node, StoredPropertyDefinition<T> property) {
        Optional<T> value = node.propertyManager().resolve(property);
        if (value.isEmpty()) {
            return;
        }
        var allowed = this.allowedValues.of(property.anyType());
        if (allowed.isPresent() && !allowed.get().contains(value.get())) {
            return;
        }
        node.propertyManager().set(property, value);
    }

    private void attach(Connection connection) {
        this.connections.add(connection);
        bind(connection.target(), connection.targetPort().property(),
                new NodeProperty<>(connection.source(), connection.sourcePort().property()));
    }

    private static <T> void bind(Node node, StoredPropertyDefinition<T> property, PropertyDataSource<?> source) {
        node.propertyManager().bind(property, PropertyDataSource.as(property.type().dataType(), source));
    }

    private void detach(Connection connection) {
        this.connections.remove(connection);
        unbind(connection);
    }

    private static void unbind(Connection connection) {
        connection.target().propertyManager().unbind(connection.targetPort().property());
    }
}

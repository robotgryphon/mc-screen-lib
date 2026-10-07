package dev.robotgryphon.screenlib.graph.ports;

import dev.robotgryphon.screenlib.graph.schema.port.InputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.port.OutputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.port.StandardOutputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.port.PropertyOutputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.port.ConstantOutputPortDefinition;
import dev.robotgryphon.screenlib.graph.Connection;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import dev.robotgryphon.screenlib.graph.schema.node.NodeProperty;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class PortManager {
    private final Node node;
    private final List<Port> ports;
    private final Map<Port, List<Connection>> connectionsByPort = new IdentityHashMap<>();

    public PortManager(Node node, NodeDefinition def) {
        this.node = node;

        List<Port> built = new ArrayList<>(def.inputs().size() + def.outputs().size() + def.properties().size());
        for (InputPortDefinition input : def.inputs()) {
            built.add(new StandardInputPort(node, Component.literal(input.name()), input.type(), input.optional()));
        }
        for (OutputPortDefinition output : def.outputs()) {
            Component title = Component.literal(output.name());
            built.add(switch (output) {
                case StandardOutputPortDefinition o -> new StandardOutputPort(node, title, o.type());
                case PropertyOutputPortDefinition o -> new PropertyOutputPort(node, title, o.type(), o.linkedProperty());
                case ConstantOutputPortDefinition o -> new ConstantOutputPort(node, title, o.type(), o.value());
            });
        }
        for (NodeProperty<?> property : def.properties()) {
            built.add(new PropertyInputPort(node, property));
        }
        this.ports = List.copyOf(built);
    }

    public List<Port> ports() {
        return this.ports;
    }

    public @Nullable PropertyInputPort propertyPort(String propertyName) {
        for (Port port : this.ports) {
            if (port instanceof PropertyInputPort p && p.propertyName().equals(propertyName)) {
                return p;
            }
        }
        return null;
    }

    public void attach(Connection connection) {
        this.portOn(connection).ifPresent(port ->
                this.connectionsByPort.computeIfAbsent(port, p -> new ArrayList<>()).add(connection));
    }

    public void detach(Connection connection) {
        this.portOn(connection).ifPresent(port -> {
            List<Connection> list = this.connectionsByPort.get(port);
            if (list != null) {
                list.remove(connection);
                if (list.isEmpty()) {
                    this.connectionsByPort.remove(port);
                }
            }
        });
    }

    private java.util.Optional<Port> portOn(Connection connection) {
        if (connection.source() == this.node) {
            return java.util.Optional.of(connection.sourcePort());
        }
        if (connection.target() == this.node) {
            return java.util.Optional.of(connection.targetPort());
        }
        return java.util.Optional.empty();
    }

    public List<Connection> connections(Port port) {
        List<Connection> list = this.connectionsByPort.get(port);
        return list == null ? List.of() : List.copyOf(list);
    }

    public boolean isConnected(Port port) {
        return this.connectionsByPort.containsKey(port);
    }

    public @Nullable Connection inbound(Port port) {
        List<Connection> list = this.connectionsByPort.get(port);
        return list == null || list.isEmpty() ? null : list.getFirst();
    }

    public @Nullable Connection inboundForProperty(String propertyName) {
        PropertyInputPort port = this.propertyPort(propertyName);
        return port == null ? null : this.inbound(port);
    }
}

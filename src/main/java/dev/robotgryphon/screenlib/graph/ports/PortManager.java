package dev.robotgryphon.screenlib.graph.ports;

import dev.robotgryphon.screenlib.client.ui.widget.Connection;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.NodeDefinition;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class PortManager {
    private final Node node;
    private final PortLayout layout;
    private final List<Port> ports;
    private final Map<Port, PortLayout.Slot> slotsByPort = new IdentityHashMap<>();
    private final Map<Port, List<Connection>> connectionsByPort = new IdentityHashMap<>();

    public PortManager(Node node, PortLayout layout, NodeDefinition def) {
        this.node = node;
        this.layout = layout;

        List<Port> built = new ArrayList<>(layout.slots().size());
        for (PortLayout.Slot slot : layout.slots()) {
            Port port = createPort(def, slot);
            built.add(port);
            this.slotsByPort.put(port, slot);
        }
        this.ports = List.copyOf(built);
    }

    private Port createPort(NodeDefinition def, PortLayout.Slot slot) {
        return switch (slot.kind()) {
            case INPUT -> {
                InputPortDefinition input = def.inputs().get(slot.index());
                yield new StandardInputPort(this.node, Component.literal(input.name()),
                        input.type(), input.optional());
            }
            case OUTPUT -> {
                OutputPortDefinition output = def.outputs().get(slot.index());
                Component title = Component.literal(output.name());
                yield switch (output) {
                    case StandardOutputPortDefinition o -> new StandardOutputPort(this.node, title, o.type());
                    case PropertyOutputPortDefinition o ->
                            new PropertyOutputPort(this.node, title, o.type(), o.linkedProperty());
                    case ConstantOutputPortDefinition o ->
                            new ConstantOutputPort(this.node, title, o.type(), o.value());
                };
            }
            case PROPERTY -> new PropertyInputPort(this.node, def.properties().get(slot.index()));
        };
    }

    public PortLayout layout() {
        return this.layout;
    }

    public List<Port> ports() {
        return this.ports;
    }

    public PortLayout.Slot slot(Port port) {
        PortLayout.Slot slot = this.slotsByPort.get(port);
        if (slot == null) {
            throw new IllegalArgumentException("Port not on this node: " + port);
        }
        return slot;
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

package dev.robotgryphon.screenlib.graph.nodes.ports;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PortManager {
    private final List<Port> ports;

    public PortManager(Node node, NodeDefinition def) {
        List<Port> built = new ArrayList<>(def.inputs().size() + def.outputs().size());
        for (InputPortDefinition input : def.inputs()) {
            if (def.property(input.property()).orElse(null) instanceof StoredPropertyDefinition<?> property) {
                built.add(new PropertyInputPort(node, property));
            }
        }
        for (OutputPortDefinition output : def.outputs()) {
            def.property(output.property()).ifPresent(property -> built.add(new PropertyOutputPort(node,
                    Node.propertyLabel(node.type(), property.name()), property)));
        }
        this.ports = List.copyOf(built);
    }

    public List<Port> ports() {
        return this.ports;
    }

    public Optional<Port> port(PortSide side, String property) {
        for (Port port : this.ports) {
            if (port.side() == side && port.propertyName().equals(property)) {
                return Optional.of(port);
            }
        }
        return Optional.empty();
    }
}

package dev.robotgryphon.screenlib.nodes.common.ports;

import dev.robotgryphon.screenlib.nodes.common.Node;
import dev.robotgryphon.screenlib.graph.schema.properties.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import dev.robotgryphon.screenlib.graph.schema.ports.OutputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.ports.InputPortDefinition;

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

    public Optional<PropertyInputPort> input(String property) {
        for (Port port : this.ports) {
            if (port instanceof PropertyInputPort input && input.propertyName().equals(property)) {
                return Optional.of(input);
            }
        }
        return Optional.empty();
    }

    public Optional<PropertyOutputPort> output(String property) {
        for (Port port : this.ports) {
            if (port instanceof PropertyOutputPort output && output.propertyName().equals(property)) {
                return Optional.of(output);
            }
        }
        return Optional.empty();
    }
}

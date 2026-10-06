package dev.robotgryphon.screenlib.graph.nodes;

import dev.robotgryphon.screenlib.graph.ports.InputPortDefinition;
import dev.robotgryphon.screenlib.graph.ports.OutputPortDefinition;
import dev.robotgryphon.screenlib.graph.ports.PropertyOutputPortDefinition;
import dev.robotgryphon.screenlib.graph.ports.StandardOutputPortDefinition;
import dev.robotgryphon.screenlib.graph.properties.NodeProperty;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

import java.util.ArrayList;
import java.util.List;

public class NodeBuilder {
    private final List<InputPortDefinition> inputs = new ArrayList<>();
    private final List<OutputPortDefinition> outputs = new ArrayList<>();
    private final List<NodeProperty<?>> properties = new ArrayList<>();

    public NodeBuilder() {}

    public NodeBuilder addInput(InputPortDefinition def) {
        this.inputs.add(def);
        return this;
    }

    public NodeBuilder addInput(String name, Holder<GraphDataType<?>> type) {
        return this.addInput(new InputPortDefinition(name, type));
    }

    public NodeBuilder addInput(String name, Holder<GraphDataType<?>> type, boolean optional) {
        return this.addInput(new InputPortDefinition(name, type, optional));
    }

    public NodeBuilder addOutput(OutputPortDefinition def) {
        this.outputs.add(def);
        return this;
    }

    public NodeBuilder addOutput(String name, Holder<GraphDataType<?>> type) {
        return this.addOutput(new StandardOutputPortDefinition(name, type));
    }

    public NodeBuilder addOutput(String name, Holder<GraphDataType<?>> type, String linkedProperty) {
        return this.addOutput(new PropertyOutputPortDefinition(name, type, linkedProperty));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public NodeBuilder addProperty(String name, Holder<GraphDataType<?>> type) {
        this.properties.add(new NodeProperty(name, type));
        return this;
    }

    public NodeDefinition build() {
        return new NodeDefinition(
                List.copyOf(this.inputs),
                List.copyOf(this.outputs),
                List.copyOf(this.properties));
    }
}

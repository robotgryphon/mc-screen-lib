package dev.robotgryphon.screenlib.graph.schema.node;

import dev.robotgryphon.screenlib.graph.computed.ComputedProperty;
import dev.robotgryphon.screenlib.graph.render.NodeRenderProvider;
import dev.robotgryphon.screenlib.graph.schema.port.InputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.port.OutputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.port.PropertyOutputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.port.StandardOutputPortDefinition;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

import java.util.ArrayList;
import java.util.List;

public class NodeBuilder {
    private final List<InputPortDefinition> inputs = new ArrayList<>();
    private final List<OutputPortDefinition> outputs = new ArrayList<>();
    private final List<NodeProperty<?>> properties = new ArrayList<>();
    private final List<ComputedPropertyDefinition> computed = new ArrayList<>();
    private final List<Holder<NodeRenderProvider<?>>> renders = new ArrayList<>();

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

    public NodeBuilder addProperty(String name, Holder<GraphDataType<?>> type) {
        return this.addProperty(name, type, false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public NodeBuilder addProperty(String name, Holder<GraphDataType<?>> type, boolean optional) {
        this.properties.add(new NodeProperty(name, type, optional));
        return this;
    }

    public NodeBuilder addComputedProperty(String name, Holder<GraphDataType<?>> type,
                                           Holder<ComputedProperty<?>> computation) {
        return this.addComputedProperty(name, type, computation, false);
    }

    public NodeBuilder addComputedProperty(String name, Holder<GraphDataType<?>> type,
                                           Holder<ComputedProperty<?>> computation, boolean hidden) {
        this.computed.add(new ComputedPropertyDefinition(name, type, computation, hidden));
        return this;
    }

    public NodeBuilder addRender(Holder<NodeRenderProvider<?>> provider) {
        this.renders.add(provider);
        return this;
    }

    public NodeDefinition build() {
        return new NodeDefinition(
                List.copyOf(this.inputs),
                List.copyOf(this.outputs),
                List.copyOf(this.properties),
                List.copyOf(this.computed),
                List.copyOf(this.renders));
    }
}

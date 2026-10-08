package dev.robotgryphon.screenlib.graph.schema.node;

import dev.robotgryphon.screenlib.client.render.NodeStateRenderer;
import dev.robotgryphon.screenlib.graph.context.NodeContextSupplier;
import dev.robotgryphon.screenlib.graph.schema.port.InputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.port.OutputPortDefinition;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NodeBuilder {
    private final List<InputPortDefinition> inputs = new ArrayList<>();
    private final List<OutputPortDefinition> outputs = new ArrayList<>();
    private final List<PropertyDefinition> properties = new ArrayList<>();
    private final List<WidgetDefinition> widgets = new ArrayList<>();
    private final List<Holder<NodeContextSupplier>> context = new ArrayList<>();
    private boolean hidden;

    public NodeBuilder() {}

    public NodeBuilder addProperty(PropertyDefinition property) {
        this.properties.add(property);
        return this;
    }

    public NodeBuilder addProperty(String name, Consumer<PropertyBuilder> configure) {
        final var builder = new PropertyBuilder(name);
        configure.accept(builder);
        if (builder.isInput()) {
            this.inputs.add(new InputPortDefinition(name));
        }
        if (builder.isOutput()) {
            this.outputs.add(new OutputPortDefinition(name));
        }
        return this.addProperty(builder.build());
    }

    public NodeBuilder addConstant(String name, Holder<GraphDataType<?>> type, Object value) {
        return this.addProperty(new ConstantPropertyDefinition(name, type, value));
    }

    public NodeBuilder addWidget(ResourceKey<NodeStateRenderer<?>> type, String property) {
        this.widgets.add(new WidgetDefinition(type, property));
        return this;
    }

    public NodeBuilder addContextSupplier(Holder<NodeContextSupplier> supplier) {
        this.context.add(supplier);
        return this;
    }

    public NodeBuilder hidden() {
        this.hidden = true;
        return this;
    }

    public NodeDefinition build() {
        return new NodeDefinition(
                List.copyOf(this.inputs),
                List.copyOf(this.outputs),
                List.copyOf(this.properties),
                List.copyOf(this.widgets),
                List.copyOf(this.context),
                this.hidden);
    }
}

package dev.robotgryphon.screenlib.graph.schema;

import net.neoforged.neoforge.registries.DeferredHolder;
import dev.robotgryphon.screenlib.graph.schema.properties.ConstantPropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.properties.PropertyBuilder;
import dev.robotgryphon.screenlib.graph.schema.properties.PropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.context.NodeContextSupplier;
import dev.robotgryphon.screenlib.graph.schema.ports.InputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.ports.OutputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import net.minecraft.core.Holder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NodeDefinitionBuilder {
    private final List<InputPortDefinition> inputs = new ArrayList<>();
    private final List<OutputPortDefinition> outputs = new ArrayList<>();
    private final List<PropertyDefinition<?>> properties = new ArrayList<>();
    private final List<Holder<NodeContextSupplier>> context = new ArrayList<>();
    private boolean hidden;

    public NodeDefinitionBuilder() {}

    public NodeDefinitionBuilder addProperty(PropertyDefinition<?> property) {
        this.properties.add(property);
        return this;
    }

    public NodeDefinitionBuilder addProperty(String name, Consumer<PropertyBuilder> configure) {
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

    public <T> NodeDefinitionBuilder addConstant(String name, DeferredHolder<GraphDataType<?>, ? extends GraphDataType<T>> type,
                                                 T value) {
        return this.addProperty(new ConstantPropertyDefinition<>(name, type, type.get(), value));
    }

    public NodeDefinitionBuilder addContextSupplier(Holder<NodeContextSupplier> supplier) {
        this.context.add(supplier);
        return this;
    }

    public NodeDefinitionBuilder hidden() {
        this.hidden = true;
        return this;
    }

    public NodeDefinition build() {
        return new NodeDefinition(
                List.copyOf(this.inputs),
                List.copyOf(this.outputs),
                List.copyOf(this.properties),
                List.copyOf(this.context),
                this.hidden);
    }
}

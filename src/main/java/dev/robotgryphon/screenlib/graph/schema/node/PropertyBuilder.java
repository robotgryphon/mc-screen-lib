package dev.robotgryphon.screenlib.graph.schema.node;

import dev.robotgryphon.screenlib.client.render.PropertyValueRenderer;
import dev.robotgryphon.screenlib.graph.computed.ComputedProperty;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class PropertyBuilder {
    private final String name;
    private Holder<GraphDataType<?>> type;
    private boolean optional;
    private boolean input;
    private boolean output;
    private boolean hideLabel;
    private @Nullable ResourceKey<PropertyValueRenderer<?>> renderer;
    private @Nullable ComputedPropertyBuilder computed;

    public PropertyBuilder(String name) {
        this.name = name;
    }

    public PropertyBuilder type(Holder<GraphDataType<?>> type) {
        this.type = type;
        return this;
    }

    public PropertyBuilder optional() {
        this.optional = true;
        return this;
    }

    public PropertyBuilder input() {
        this.input = true;
        return this;
    }

    public PropertyBuilder output() {
        this.output = true;
        return this;
    }

    public PropertyBuilder hideLabel() {
        this.hideLabel = true;
        return this;
    }

    public PropertyBuilder renderer(ResourceKey<PropertyValueRenderer<?>> renderer) {
        this.renderer = renderer;
        return this;
    }

    public ComputedPropertyBuilder computed(Holder<ComputedProperty<?>> computation) {
        this.computed = new ComputedPropertyBuilder(computation);
        return this.computed;
    }

    public String name() {
        return this.name;
    }

    public boolean isInput() {
        return this.input;
    }

    public boolean isOutput() {
        return this.output;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public PropertyDefinition build() {
        if (this.computed == null) {
            return new StoredPropertyDefinition(this.name, this.type, this.optional, this.hideLabel,
                    Optional.ofNullable(this.renderer));
        }
        if (this.input || this.optional) {
            throw new IllegalStateException("Computed property '" + this.name + "' cannot be an input or optional");
        }
        return this.computed.build(this.name, this.type, this.hideLabel, this.renderer);
    }
}

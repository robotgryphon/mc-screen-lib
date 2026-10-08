package dev.robotgryphon.screenlib.graph.schema.node;

import dev.robotgryphon.screenlib.client.render.PropertyValueRenderer;
import dev.robotgryphon.screenlib.graph.computed.ComputedProperty;
import dev.robotgryphon.screenlib.graph.computed.DataKey;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ComputedPropertyBuilder {
    private final Holder<ComputedProperty<?>> computation;
    private final Map<String, String> data = new LinkedHashMap<>();
    private boolean hideLabel;
    private @Nullable ResourceKey<PropertyValueRenderer<?>> renderer;

    ComputedPropertyBuilder(Holder<ComputedProperty<?>> computation) {
        this.computation = computation;
    }

    public ComputedPropertyBuilder data(String key, String property) {
        this.data.put(key, property);
        return this;
    }

    public ComputedPropertyBuilder data(DataKey<?> key, String property) {
        return this.data(key.name(), property);
    }

    public ComputedPropertyBuilder hideLabel() {
        this.hideLabel = true;
        return this;
    }

    public ComputedPropertyBuilder renderer(ResourceKey<PropertyValueRenderer<?>> renderer) {
        this.renderer = renderer;
        return this;
    }

    ComputedPropertyDefinition build(String name, Holder<GraphDataType<?>> type, boolean hideLabel,
                                     @Nullable ResourceKey<PropertyValueRenderer<?>> renderer) {
        return new ComputedPropertyDefinition(name, type, this.computation, Map.copyOf(this.data),
                this.hideLabel || hideLabel, Optional.ofNullable(this.renderer != null ? this.renderer : renderer));
    }
}

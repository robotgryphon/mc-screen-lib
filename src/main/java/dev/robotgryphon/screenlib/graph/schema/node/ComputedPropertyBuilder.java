package dev.robotgryphon.screenlib.graph.schema.node;

import dev.robotgryphon.screenlib.graph.computed.ComputedProperty;
import dev.robotgryphon.screenlib.graph.computed.DataKey;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

import java.util.LinkedHashMap;
import java.util.Map;

public class ComputedPropertyBuilder {
    private final Holder<ComputedProperty<?>> computation;
    private final Map<String, String> data = new LinkedHashMap<>();
    private boolean hideLabel;

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

    ComputedPropertyDefinition build(String name, Holder<GraphDataType<?>> type) {
        return new ComputedPropertyDefinition(name, type, this.computation, Map.copyOf(this.data), this.hideLabel);
    }
}

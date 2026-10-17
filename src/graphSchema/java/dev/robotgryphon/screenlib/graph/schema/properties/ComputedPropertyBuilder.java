package dev.robotgryphon.screenlib.graph.schema.properties;

import org.jspecify.annotations.Nullable;
import dev.robotgryphon.screenlib.graph.schema.properties.DataKey;
import dev.robotgryphon.screenlib.graph.schema.properties.computed.ComputedProperty;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import net.minecraft.core.Holder;

import java.util.LinkedHashMap;
import java.util.Map;

public class ComputedPropertyBuilder {
    private final Holder<ComputedProperty<?>> computation;
    private final Map<String, String> data = new LinkedHashMap<>();

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

    ComputedPropertyDefinition<?> build(String name, @Nullable Holder<GraphDataType<?>> declared) {
        var definition = new ComputedPropertyDefinition<>(name, this.computation, this.computation.value(), Map.copyOf(this.data));
        if (declared != null && declared.value() != definition.type()) {
            throw new IllegalStateException("Computed property '" + name + "' declares a type its computation does not produce");
        }
        return definition;
    }
}

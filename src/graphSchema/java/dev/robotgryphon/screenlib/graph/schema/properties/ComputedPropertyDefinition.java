package dev.robotgryphon.screenlib.graph.schema.properties;

import dev.robotgryphon.screenlib.graph.schema.properties.computed.ComputedProperty;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import net.minecraft.core.Holder;

import java.util.Map;

public record ComputedPropertyDefinition<T>(String name, Holder<ComputedProperty<?>> computation,
                                            ComputedProperty<T> function, Map<String, String> data)
        implements PropertyDefinition<T> {
    @Override
    public Holder<GraphDataType<?>> anyType() {
        return this.function.type();
    }

    @Override
    public GraphDataType<T> type() {
        return this.function.type().get();
    }
}

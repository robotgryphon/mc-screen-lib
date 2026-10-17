package dev.robotgryphon.screenlib.graph.schema.properties;

import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import net.minecraft.core.Holder;

public sealed interface PropertyDefinition<T>
        permits StoredPropertyDefinition, ConstantPropertyDefinition, ComputedPropertyDefinition {
    String name();

    Holder<GraphDataType<?>> anyType();

    GraphDataType<T> type();
}

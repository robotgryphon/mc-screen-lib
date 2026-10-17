package dev.robotgryphon.screenlib.graph.schema.properties;

import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import net.minecraft.core.Holder;

public record StoredPropertyDefinition<T>(String name, Holder<GraphDataType<?>> anyType, GraphDataType<T> type,
                                          boolean optional)
        implements PropertyDefinition<T> {
}

package dev.robotgryphon.screenlib.graph.schema.properties;

import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import net.minecraft.core.Holder;

public record ConstantPropertyDefinition<T>(String name, Holder<GraphDataType<?>> anyType, GraphDataType<T> type,
                                            T value) implements PropertyDefinition<T> {
}

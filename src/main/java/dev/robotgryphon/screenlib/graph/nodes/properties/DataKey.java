package dev.robotgryphon.screenlib.graph.nodes.properties;

import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.neoforged.neoforge.registries.DeferredHolder;

public record DataKey<T>(String name, DeferredHolder<GraphDataType<?>, ? extends GraphDataType<T>> type,
                         boolean optional) {
    public static <T> DataKey<T> required(String name, DeferredHolder<GraphDataType<?>, ? extends GraphDataType<T>> type) {
        return new DataKey<>(name, type, false);
    }

    public static <T> DataKey<T> optional(String name, DeferredHolder<GraphDataType<?>, ? extends GraphDataType<T>> type) {
        return new DataKey<>(name, type, true);
    }
}

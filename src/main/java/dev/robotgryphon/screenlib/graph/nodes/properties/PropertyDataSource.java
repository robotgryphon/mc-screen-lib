package dev.robotgryphon.screenlib.graph.nodes.properties;

import java.util.Optional;

@FunctionalInterface
public interface PropertyDataSource<T> {
    Optional<T> resolve();

    static <T> PropertyDataSource<T> as(Class<T> type, PropertyDataSource<?> source) {
        return () -> source.resolve().filter(type::isInstance).map(type::cast);
    }
}

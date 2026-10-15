package dev.robotgryphon.screenlib.graph.nodes.properties;

import java.util.Optional;

@FunctionalInterface
public interface PropertyDataSource {
    Optional<Object> resolve();
}

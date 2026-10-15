package dev.robotgryphon.screenlib.graph.nodes.properties;

import dev.robotgryphon.screenlib.graph.nodes.Node;

import java.util.Optional;

public record NodeProperty(Node node, String property) implements NodePropertyReference, PropertyDataSource {
    @Override
    public Optional<Object> resolve() {
        return this.node.propertyManager().resolve(this.property);
    }
}

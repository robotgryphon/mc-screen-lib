package dev.robotgryphon.screenlib.graph.nodes.properties;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.PropertyDefinition;

import java.util.Optional;

public record NodeProperty<T>(Node node, PropertyDefinition<T> property)
        implements NodePropertyReference<T>, PropertyDataSource<T> {
    @Override
    public Optional<T> resolve() {
        return this.node.propertyManager().resolve(this.property);
    }
}

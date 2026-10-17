package dev.robotgryphon.screenlib.nodes.common.properties;

import dev.robotgryphon.screenlib.nodes.common.Node;
import dev.robotgryphon.screenlib.graph.schema.properties.PropertyDefinition;

import java.util.Optional;
import dev.robotgryphon.screenlib.nodes.common.properties.NodePropertyReference;
import dev.robotgryphon.screenlib.nodes.common.properties.PropertyDataSource;

public record NodeProperty<T>(Node node, PropertyDefinition<T> property)
        implements NodePropertyReference<T>, PropertyDataSource<T> {
    @Override
    public Optional<T> resolve() {
        return this.node.propertyManager().resolve(this.property);
    }
}

package dev.robotgryphon.screenlib.graph.nodes.properties;

import dev.robotgryphon.screenlib.graph.nodes.NodeReference;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.PropertyDefinition;

public interface NodePropertyReference<T> {
    NodeReference node();

    PropertyDefinition<T> property();
}

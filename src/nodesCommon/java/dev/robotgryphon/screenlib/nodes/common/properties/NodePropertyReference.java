package dev.robotgryphon.screenlib.nodes.common.properties;

import dev.robotgryphon.screenlib.nodes.api.NodeReference;
import dev.robotgryphon.screenlib.graph.schema.properties.PropertyDefinition;

public interface NodePropertyReference<T> {
    NodeReference node();

    PropertyDefinition<T> property();
}

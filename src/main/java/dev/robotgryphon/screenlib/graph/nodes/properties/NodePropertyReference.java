package dev.robotgryphon.screenlib.graph.nodes.properties;

import dev.robotgryphon.screenlib.graph.nodes.NodeReference;

public interface NodePropertyReference {
    NodeReference node();

    String property();
}

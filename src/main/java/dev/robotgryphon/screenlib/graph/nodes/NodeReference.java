package dev.robotgryphon.screenlib.graph.nodes;

import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import net.minecraft.resources.ResourceKey;

import java.util.UUID;

public interface NodeReference {
    ResourceKey<NodeDefinition> type();

    UUID id();
}

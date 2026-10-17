package dev.robotgryphon.screenlib.nodes.api;

import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import net.minecraft.resources.ResourceKey;

import java.util.UUID;

public interface NodeReference {
    ResourceKey<NodeDefinition> type();

    UUID id();
}

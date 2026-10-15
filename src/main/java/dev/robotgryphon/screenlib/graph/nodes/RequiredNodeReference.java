package dev.robotgryphon.screenlib.graph.nodes;

import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import net.minecraft.resources.ResourceKey;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record RequiredNodeReference(ResourceKey<NodeDefinition> type, UUID id) implements NodeReference {
    public static RequiredNodeReference of(ResourceKey<NodeDefinition> type) {
        return new RequiredNodeReference(type, UUID.nameUUIDFromBytes(
                ("required:" + type.identifier()).getBytes(StandardCharsets.UTF_8)));
    }
}

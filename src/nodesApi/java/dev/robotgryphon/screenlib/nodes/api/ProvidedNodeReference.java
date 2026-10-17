package dev.robotgryphon.screenlib.nodes.api;

import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import net.minecraft.resources.ResourceKey;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public record ProvidedNodeReference(ResourceKey<NodeDefinition> type, UUID id, Map<String, Object> values)
        implements NodeReference {
    public static ProvidedNodeReference of(ResourceKey<NodeDefinition> type, Map<String, Object> values) {
        return new ProvidedNodeReference(type, idFor(type), Map.copyOf(values));
    }

    public static UUID idFor(ResourceKey<NodeDefinition> type) {
        return UUID.nameUUIDFromBytes(("provided:" + type.identifier()).getBytes(StandardCharsets.UTF_8));
    }
}

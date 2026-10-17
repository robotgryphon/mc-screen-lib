package dev.robotgryphon.screenlib.nodes.api.canvas;

import dev.robotgryphon.screenlib.nodes.api.state.CanvasState;
import com.mojang.serialization.MapCodec;
import dev.robotgryphon.screenlib.nodes.api.ProvidedNodeReference;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import net.minecraft.resources.ResourceKey;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CanvasDefinition {
    public static final MapCodec<CanvasDefinition> MAP_CODEC =
            CanvasState.MAP_CODEC.xmap(CanvasDefinition::new, CanvasDefinition::state);

    private final Map<ResourceKey<NodeDefinition>, ProvidedNodeReference> provided = new LinkedHashMap<>();
    private CanvasState state;

    public CanvasDefinition() {
        this(CanvasState.empty());
    }

    public CanvasDefinition(CanvasState state) {
        this.state = state;
    }

    public ProvidedNodeReference provide(ResourceKey<NodeDefinition> type) {
        return this.provide(type, Map.of());
    }

    public ProvidedNodeReference provide(ResourceKey<NodeDefinition> type, Map<String, Object> values) {
        if (this.provided.containsKey(type)) {
            throw new IllegalArgumentException("Node type " + type.identifier() + " is already provided");
        }
        ProvidedNodeReference node = ProvidedNodeReference.of(type, values);
        this.provided.put(type, node);
        return node;
    }

    public Collection<ProvidedNodeReference> provided() {
        return Collections.unmodifiableCollection(this.provided.values());
    }

    public Optional<ProvidedNodeReference> provided(ResourceKey<NodeDefinition> type) {
        return Optional.ofNullable(this.provided.get(type));
    }

    public CanvasState state() {
        return this.state;
    }

    public void setState(CanvasState state) {
        this.state = state;
    }
}

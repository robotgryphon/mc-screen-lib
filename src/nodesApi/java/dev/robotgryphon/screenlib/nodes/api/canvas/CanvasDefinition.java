package dev.robotgryphon.screenlib.nodes.api.canvas;

import dev.robotgryphon.screenlib.nodes.api.state.CanvasState;
import com.mojang.serialization.MapCodec;
import dev.robotgryphon.screenlib.nodes.api.RequiredNodeReference;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import net.minecraft.resources.ResourceKey;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CanvasDefinition {
    public static final MapCodec<CanvasDefinition> MAP_CODEC =
            CanvasState.MAP_CODEC.xmap(CanvasDefinition::new, CanvasDefinition::state);

    private final Map<ResourceKey<NodeDefinition>, RequiredNodeReference> required = new LinkedHashMap<>();
    private CanvasState state;

    public CanvasDefinition() {
        this(CanvasState.empty());
    }

    public CanvasDefinition(CanvasState state) {
        this.state = state;
    }

    public RequiredNodeReference require(ResourceKey<NodeDefinition> type) {
        if (this.required.containsKey(type)) {
            throw new IllegalArgumentException("Node type " + type.identifier() + " is already required");
        }
        RequiredNodeReference node = RequiredNodeReference.of(type);
        this.required.put(type, node);
        return node;
    }

    public Collection<RequiredNodeReference> required() {
        return Collections.unmodifiableCollection(this.required.values());
    }

    public CanvasState state() {
        return this.state;
    }

    public void setState(CanvasState state) {
        this.state = state;
    }
}

package dev.robotgryphon.screenlib.graph.canvas;

import dev.robotgryphon.screenlib.graph.state.CanvasState;

import com.mojang.serialization.MapCodec;
import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.state.NodeState;
import dev.robotgryphon.screenlib.graph.nodes.RequiredNodeReference;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class CanvasDefinition {
    public static final MapCodec<CanvasDefinition> MAP_CODEC =
            CanvasState.MAP_CODEC.xmap(CanvasDefinition::new, CanvasDefinition::state);

    private static final int NEW_NODE_OFFSET = 24;

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

    public boolean reconcile(RegistryAccess registries) {
        var definitions = registries.lookupOrThrow(NodeDefinition.REGISTRY_KEY);
        Set<UUID> present = this.state.nodes().stream().map(NodeState::id).collect(Collectors.toSet());
        List<NodeState> nodes = new ArrayList<>(this.state.nodes());
        for (RequiredNodeReference required : this.required.values()) {
            if (present.contains(required.id())) {
                continue;
            }
            var definition = definitions.get(required.type());
            if (definition.isEmpty()) {
                ScreenLib.LOGGER.error("Required node uses unknown node type {}", required.type().identifier());
                continue;
            }
            int offset = (nodes.size() - this.state.nodes().size()) * NEW_NODE_OFFSET;
            nodes.add(new NodeState(required.id(), definition.get(), new Vector2i(offset, offset), Map.of()));
        }
        if (nodes.size() == this.state.nodes().size()) {
            return false;
        }
        this.state = this.state.withContents(nodes, this.state.connections());
        return true;
    }
}

package dev.robotgryphon.screenlib.client.render;

import dev.robotgryphon.screenlib.graph.render.NodeRenderProvider;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class NodeRenderers {
    private static final Map<Identifier, NodeStateRenderer<?>> RENDERERS = new HashMap<>();

    private NodeRenderers() {}

    public static <S> void register(Holder<? extends NodeRenderProvider<?>> provider, NodeStateRenderer<S> renderer) {
        RENDERERS.put(provider.unwrapKey().orElseThrow().identifier(), renderer);
    }

    @SuppressWarnings("unchecked")
    public static <S> @Nullable NodeStateRenderer<S> get(Holder<NodeRenderProvider<?>> provider) {
        return provider.unwrapKey()
                .map(key -> (NodeStateRenderer<S>) RENDERERS.get(key.identifier()))
                .orElse(null);
    }
}

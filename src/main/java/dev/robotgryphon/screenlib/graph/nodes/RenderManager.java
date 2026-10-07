package dev.robotgryphon.screenlib.graph.nodes;

import dev.robotgryphon.screenlib.graph.render.NodeRenderProvider;
import net.minecraft.core.Holder;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RenderManager {
    private final List<Holder<NodeRenderProvider<?>>> providers;
    private final Map<Integer, Object> states = new HashMap<>();

    public RenderManager(List<Holder<NodeRenderProvider<?>>> providers) {
        this.providers = List.copyOf(providers);
    }

    public List<Holder<NodeRenderProvider<?>>> providers() {
        return this.providers;
    }

    public @Nullable Object state(int providerIndex) {
        return this.states.get(providerIndex);
    }

    public void setState(int providerIndex, @Nullable Object state) {
        if (state == null) {
            this.states.remove(providerIndex);
        } else {
            this.states.put(providerIndex, state);
        }
    }

    public void clear() {
        this.states.clear();
    }
}

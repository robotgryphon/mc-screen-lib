package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public final class HostedNode {
    private final String id;
    private final ResourceKey<NodeDefinition> type;
    private final int x;
    private final int y;
    private final Consumer<Optional<HostedInputs>> listener;
    private @Nullable Optional<HostedInputs> published;

    public HostedNode(String id, ResourceKey<NodeDefinition> type, int x, int y,
                      Consumer<Optional<HostedInputs>> listener) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.listener = listener;
    }

    public String id() {
        return this.id;
    }

    public ResourceKey<NodeDefinition> type() {
        return this.type;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    void publish(Optional<HostedInputs> inputs) {
        if (inputs.equals(this.published)) {
            return;
        }
        this.published = inputs;
        this.listener.accept(inputs);
    }
}

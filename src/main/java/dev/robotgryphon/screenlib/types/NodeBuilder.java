package dev.robotgryphon.screenlib.types;

import net.minecraft.core.Holder;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for {@link NodeDefinition} — the schema-level description
 * of a node type. The builder collects typed input and output ports in
 * declaration order and assembles them into a {@code NodeDefinition} that
 * can be persisted (via the data generator) or registered at runtime.
 *
 * <p>Builders intentionally don't carry layout fields like position or
 * size: those are instance state on a {@code Node}, not part of the type's
 * schema, and putting them here would invite mixing the two concerns.
 *
 * <p>Lives in the {@code types} package (rather than under the client UI)
 * because the data generator — which has no access to client-only classes —
 * needs to be able to construct {@link NodeDefinition}s through it.
 */
public class NodeBuilder {
    private final List<PortDefinition> inputs = new ArrayList<>();
    private final List<PortDefinition> outputs = new ArrayList<>();

    public NodeBuilder() {}

    public NodeBuilder addInput(PortDefinition def) {
        this.inputs.add(def);
        return this;
    }

    public NodeBuilder addInput(String name, Holder<PropertyType<?>> type) {
        return this.addInput(new PortDefinition(name, type));
    }

    public NodeBuilder addOutput(PortDefinition def) {
        this.outputs.add(def);
        return this;
    }

    public NodeBuilder addOutput(String name, Holder<PropertyType<?>> type) {
        return this.addOutput(new PortDefinition(name, type));
    }

    public NodeDefinition build() {
        return new NodeDefinition(List.copyOf(this.inputs), List.copyOf(this.outputs));
    }
}

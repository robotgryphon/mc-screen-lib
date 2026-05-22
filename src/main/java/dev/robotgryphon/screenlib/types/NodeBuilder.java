package dev.robotgryphon.screenlib.types;

import net.minecraft.core.Holder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fluent builder for {@link NodeDefinition} — the schema-level description
 * of a node type. The builder collects typed input and output ports plus
 * any configurable properties in declaration order and assembles them
 * into a {@code NodeDefinition} that can be persisted (via the data
 * generator) or registered at runtime.
 *
 * <p>Builders intentionally don't carry layout fields like position or
 * size: those are instance state on a {@code Node}, not part of the type's
 * schema, and putting them here would invite mixing the two concerns.
 *
 * <p>All three "add" methods (input, output, property) take the same
 * {@code (name, Holder<PropertyDefinition>)} pair because the three
 * roles share {@link PortDefinition}'s schema. Defaults for a property
 * come from the registered {@link PropertyDefinition} itself; no
 * per-use override path exists on the builder.
 *
 * <p>Lives in the {@code types} package (rather than under the client UI)
 * because the data generator — which has no access to client-only classes —
 * needs to be able to construct {@link NodeDefinition}s through it.
 */
public class NodeBuilder {
    private final List<PortDefinition> inputs = new ArrayList<>();
    private final List<PortDefinition> outputs = new ArrayList<>();
    private final List<PortDefinition> properties = new ArrayList<>();

    public NodeBuilder() {}

    public NodeBuilder addInput(PortDefinition def) {
        this.inputs.add(def);
        return this;
    }

    public NodeBuilder addInput(String name, Holder<PropertyDefinition<?>> type) {
        return this.addInput(new PortDefinition(name, type));
    }

    /**
     * Add an input port flagged as optional — the node functions without it
     * wired, and the widget layer renders the port as a hollow ring
     * (just the type-colored outline) so the user can tell at a glance
     * that the input has a fallback. Equivalent to
     * {@link #addInput(String, Holder)} otherwise.
     */
    public NodeBuilder addInput(String name, Holder<PropertyDefinition<?>> type, boolean optional) {
        return this.addInput(new PortDefinition(name, type, optional));
    }

    public NodeBuilder addOutput(PortDefinition def) {
        this.outputs.add(def);
        return this;
    }

    public NodeBuilder addOutput(String name, Holder<PropertyDefinition<?>> type) {
        return this.addOutput(new PortDefinition(name, type));
    }

    /**
     * Add an output port that relays the current value of one of the
     * node's own properties. The {@code linkedProperty} argument names
     * which property on this node — typically also declared via
     * {@link #addProperty} — supplies the value that wires from this
     * port carry downstream.
     *
     * <p>Used by primitive nodes (Boolean, Number, etc.) where the
     * editor inside the node sets a value and an output port exposes
     * it. The data flow is one-way: changes to the property write
     * through to anything wired off the linked output on the next
     * render frame. The runtime side of the resolution lives in
     * {@code NodeWidget.resolveUpstreamValue}.
     */
    public NodeBuilder addOutput(String name, Holder<PropertyDefinition<?>> type, String linkedProperty) {
        return this.addOutput(new PortDefinition(name, type, Optional.of(linkedProperty)));
    }

    /**
     * Add a configurable property rendered inside the node body. The name
     * is both the serialization key (for the per-instance value map on a
     * {@code Node}) and the rendered label. The default — if any — is
     * carried by the registered {@link PropertyDefinition} that
     * {@code type} resolves to.
     */
    public NodeBuilder addProperty(PortDefinition def) {
        this.properties.add(def);
        return this;
    }

    public NodeBuilder addProperty(String name, Holder<PropertyDefinition<?>> type) {
        return this.addProperty(new PortDefinition(name, type));
    }

    public NodeDefinition build() {
        return new NodeDefinition(
                List.copyOf(this.inputs),
                List.copyOf(this.outputs),
                List.copyOf(this.properties));
    }
}

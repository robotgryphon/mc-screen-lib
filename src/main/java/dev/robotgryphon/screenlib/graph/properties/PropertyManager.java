package dev.robotgryphon.screenlib.graph.properties;

import dev.robotgryphon.screenlib.graph.nodes.Node;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PropertyManager {

    private final Node node;
    private final List<NodeProperty<?>> properties;
    private final Map<String, Object> values = new HashMap<>();
    private @Nullable String focusedName;

    public PropertyManager(Node node, List<NodeProperty<?>> properties) {
        this.node = node;
        this.properties = List.copyOf(properties);
        for (NodeProperty<?> property : this.properties) {
            property.type().value().defaultValueRaw()
                    .ifPresent(value -> this.values.put(property.name(), value));
        }
    }

    public List<NodeProperty<?>> properties() {
        return this.properties;
    }

    public @Nullable Object value(String name) {
        return this.values.get(name);
    }

    public void set(String name, @Nullable Object value) {
        if (value == null) {
            this.values.remove(name);
        } else {
            this.values.put(name, value);
        }
        this.node.validate();
        this.node.canvas.revalidateDownstreamOf(this.node);
    }

    public @Nullable String focusedName() {
        return this.focusedName;
    }

    public void setFocusedName(@Nullable String name) {
        this.focusedName = name;
    }
}

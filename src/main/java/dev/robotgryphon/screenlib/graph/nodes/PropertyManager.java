package dev.robotgryphon.screenlib.graph.nodes;

import dev.robotgryphon.screenlib.graph.schema.node.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.node.NodeProperty;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PropertyManager {

    private final Node node;
    private final List<NodeProperty<?>> properties;
    private final List<ComputedPropertyDefinition> computed;
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Object> computedValues = new HashMap<>();
    private @Nullable String focusedName;

    public PropertyManager(Node node, List<NodeProperty<?>> properties, List<ComputedPropertyDefinition> computed) {
        this.node = node;
        this.properties = List.copyOf(properties);
        this.computed = List.copyOf(computed);
        for (NodeProperty<?> property : this.properties) {
            property.type().value().defaultValueRaw()
                    .ifPresent(value -> this.values.put(property.name(), value));
        }
    }

    public List<NodeProperty<?>> properties() {
        return this.properties;
    }

    public List<ComputedPropertyDefinition> computedProperties() {
        return this.computed;
    }

    public int rowCount() {
        return this.properties.size() + this.computed.size();
    }

    public @Nullable Object value(String name) {
        Object stored = this.values.get(name);
        return stored != null ? stored : this.computedValues.get(name);
    }

    public void set(String name, @Nullable Object value) {
        if (value == null) {
            this.values.remove(name);
        } else {
            this.values.put(name, value);
        }
        this.node.canvas.touch();
    }

    public @Nullable Object computedValue(String name) {
        return this.computedValues.get(name);
    }

    public void setComputedValue(String name, @Nullable Object value) {
        if (value == null) {
            this.computedValues.remove(name);
        } else {
            this.computedValues.put(name, value);
        }
    }

    public void clearComputed() {
        this.computedValues.clear();
    }

    public @Nullable String focusedName() {
        return this.focusedName;
    }

    public void setFocusedName(@Nullable String name) {
        this.focusedName = name;
    }
}

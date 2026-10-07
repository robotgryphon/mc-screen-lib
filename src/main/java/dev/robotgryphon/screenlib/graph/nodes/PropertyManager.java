package dev.robotgryphon.screenlib.graph.nodes;

import dev.robotgryphon.screenlib.graph.schema.node.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.node.NodeDefinition;
import dev.robotgryphon.screenlib.graph.schema.node.StoredPropertyDefinition;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PropertyManager {

    private final Node node;
    private final List<StoredPropertyDefinition<?>> stored;
    private final List<ComputedPropertyDefinition> computed;
    private final Map<String, Object> constants = new HashMap<>();
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Object> computedValues = new HashMap<>();
    private @Nullable String focusedName;

    public PropertyManager(Node node, NodeDefinition definition) {
        this.node = node;
        this.stored = definition.stored();
        this.computed = definition.computed();
        definition.constants().forEach(constant -> this.constants.put(constant.name(), constant.value()));
        for (StoredPropertyDefinition<?> property : this.stored) {
            property.type().value().defaultValueRaw()
                    .ifPresent(value -> this.values.put(property.name(), value));
        }
    }

    public List<StoredPropertyDefinition<?>> stored() {
        return this.stored;
    }

    public List<ComputedPropertyDefinition> computedProperties() {
        return this.computed;
    }

    public @Nullable Object value(String name) {
        Object stored = this.values.get(name);
        if (stored != null) {
            return stored;
        }
        Object computed = this.computedValues.get(name);
        return computed != null ? computed : this.constants.get(name);
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

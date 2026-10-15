package dev.robotgryphon.screenlib.graph.nodes.properties;

import dev.robotgryphon.screenlib.graph.nodes.Node;

import dev.robotgryphon.screenlib.graph.nodes.properties.schema.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PropertyManager {

    private final Node node;
    private final List<StoredPropertyDefinition<?>> stored;
    private final List<ComputedPropertyDefinition> computed;
    private final Map<String, Object> constants = new HashMap<>();
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Object> computedValues = new HashMap<>();
    private final Map<String, PropertyDataSource> sources = new HashMap<>();
    private final Set<String> resolving = new HashSet<>();

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

    public void bind(String name, PropertyDataSource source) {
        this.sources.put(name, source);
    }

    public void unbind(String name) {
        this.sources.remove(name);
    }

    public boolean isDriven(String name) {
        return this.sources.containsKey(name);
    }

    public Optional<Object> resolve(String name) {
        PropertyDataSource source = this.sources.get(name);
        if (source == null) {
            return Optional.ofNullable(this.value(name));
        }
        if (!this.resolving.add(name)) {
            return Optional.empty();
        }
        try {
            return source.resolve();
        } finally {
            this.resolving.remove(name);
        }
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
}

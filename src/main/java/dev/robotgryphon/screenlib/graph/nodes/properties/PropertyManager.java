package dev.robotgryphon.screenlib.graph.nodes.properties;

import dev.robotgryphon.screenlib.graph.types.DataTypeWithDefault;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.PropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.ConstantPropertyDefinition;
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
    private final List<ComputedPropertyDefinition<?>> computed;
    private final List<ConstantPropertyDefinition<?>> constants;
    private PropertyValues values = PropertyValues.EMPTY;
    private PropertyValues computedValues = PropertyValues.EMPTY;
    private final Map<String, PropertyDataSource<?>> sources = new HashMap<>();
    private final Set<String> resolving = new HashSet<>();

    public PropertyManager(Node node, NodeDefinition definition) {
        this.node = node;
        this.stored = definition.stored();
        this.computed = definition.computed();
        this.constants = definition.constants();
        for (StoredPropertyDefinition<?> property : this.stored) {
            this.values = withDefault(this.values, property);
        }
    }

    private static <T> PropertyValues withDefault(PropertyValues values, StoredPropertyDefinition<T> property) {
        return property.type() instanceof DataTypeWithDefault<T> withDefault
                ? values.with(property, Optional.of(withDefault.defaultValue()))
                : values;
    }

    public List<StoredPropertyDefinition<?>> stored() {
        return this.stored;
    }

    public List<ComputedPropertyDefinition<?>> computedProperties() {
        return this.computed;
    }

    public PropertyValues values() {
        return this.values;
    }

    public <T> Optional<T> get(StoredPropertyDefinition<T> property) {
        return this.values.get(property);
    }

    public @Nullable Object value(String name) {
        Object stored = this.values.get(name);
        if (stored != null) {
            return stored;
        }
        Object computed = this.computedValues.get(name);
        if (computed != null) {
            return computed;
        }
        for (ConstantPropertyDefinition<?> constant : this.constants) {
            if (constant.name().equals(name)) {
                return constant.value();
            }
        }
        return null;
    }

    public void load(PropertyValues saved) {
        this.values = this.values.merge(saved);
    }

    public void set(String name, @Nullable Object value) {
        for (StoredPropertyDefinition<?> property : this.stored) {
            if (property.name().equals(name)) {
                this.setChecked(property, value);
                return;
            }
        }
    }

    private <T> void setChecked(StoredPropertyDefinition<T> property, @Nullable Object value) {
        Class<T> type = property.type().dataType();
        if (value == null) {
            this.set(property, Optional.empty());
        } else if (type.isInstance(value)) {
            this.set(property, Optional.of(type.cast(value)));
        }
    }

    public <T> void set(StoredPropertyDefinition<T> property, Optional<T> value) {
        this.values = this.values.with(property, value);
        this.node.canvas.propertyChanged(this.node, property, value);
    }

    public <T> void bind(StoredPropertyDefinition<T> property, PropertyDataSource<T> source) {
        this.sources.put(property.name(), source);
    }

    public void unbind(StoredPropertyDefinition<?> property) {
        this.sources.remove(property.name());
    }

    public boolean isDriven(StoredPropertyDefinition<?> property) {
        return this.sources.containsKey(property.name());
    }

    public <T> Optional<T> resolve(PropertyDefinition<T> property) {
        return switch (property) {
            case StoredPropertyDefinition<T> stored -> this.resolveStored(stored);
            case ComputedPropertyDefinition<T> computed -> this.computedValues.get(computed);
            case ConstantPropertyDefinition<T> constant -> Optional.of(constant.value());
        };
    }

    private <T> Optional<T> resolveStored(StoredPropertyDefinition<T> property) {
        PropertyDataSource<?> source = this.sources.get(property.name());
        if (source == null) {
            return this.values.get(property);
        }
        if (!this.resolving.add(property.name())) {
            return Optional.empty();
        }
        try {
            return PropertyDataSource.as(property.type().dataType(), source).resolve();
        } finally {
            this.resolving.remove(property.name());
        }
    }

    public Optional<Object> resolve(String name) {
        return this.property(name).flatMap(this::resolveAny);
    }

    private <T> Optional<Object> resolveAny(PropertyDefinition<T> property) {
        return this.resolve(property).map(value -> value);
    }

    public Optional<PropertyDefinition<?>> property(String name) {
        return this.node.definition().value().property(name);
    }

    public @Nullable Object computedValue(String name) {
        return this.computedValues.get(name);
    }

    public <T> Optional<T> computed(ComputedPropertyDefinition<T> property) {
        return this.computedValues.get(property);
    }

    public <T> boolean setComputed(ComputedPropertyDefinition<T> property, Optional<T> value) {
        if (this.computedValues.get(property).equals(value)) {
            return false;
        }
        this.computedValues = this.computedValues.with(property, value);
        return true;
    }

    public PropertyValues computedValues() {
        return this.computedValues;
    }

    public void loadComputed(PropertyValues values) {
        this.computedValues = values;
    }

    public void clearComputed() {
        this.computedValues = PropertyValues.EMPTY;
    }
}

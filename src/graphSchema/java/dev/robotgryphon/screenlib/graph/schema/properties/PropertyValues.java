package dev.robotgryphon.screenlib.graph.schema.properties;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.robotgryphon.screenlib.graph.schema.properties.PropertyDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PropertyValues {
    public static final PropertyValues EMPTY = new PropertyValues(Map.of());

    private final Map<String, Object> values;

    private PropertyValues(Map<String, Object> values) {
        this.values = values;
    }

    public <T> Optional<T> get(PropertyDefinition<T> property) {
        Class<T> type = property.type().dataType();
        return Optional.ofNullable(this.values.get(property.name())).filter(type::isInstance).map(type::cast);
    }

    public @Nullable Object get(String name) {
        return this.values.get(name);
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    public <T> PropertyValues with(PropertyDefinition<T> property, Optional<T> value) {
        Map<String, Object> next = new HashMap<>(this.values);
        value.ifPresentOrElse(present -> next.put(property.name(), present), () -> next.remove(property.name()));
        return new PropertyValues(Map.copyOf(next));
    }

    public <T> PropertyValues withChecked(PropertyDefinition<T> property, @Nullable Object value) {
        Class<T> type = property.type().dataType();
        if (value == null) {
            return this.with(property, Optional.empty());
        }
        return type.isInstance(value) ? this.with(property, Optional.of(type.cast(value))) : this;
    }

    public PropertyValues merge(PropertyValues overrides) {
        Map<String, Object> next = new HashMap<>(this.values);
        next.putAll(overrides.values);
        return new PropertyValues(Map.copyOf(next));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PropertyValues values && this.values.equals(values.values);
    }

    @Override
    public int hashCode() {
        return this.values.hashCode();
    }

    @Override
    public String toString() {
        return "PropertyValues" + this.values;
    }

    public static Codec<PropertyValues> codec(List<? extends PropertyDefinition<?>> properties) {
        return new Codec<>() {
            @Override
            public <O> DataResult<Pair<PropertyValues, O>> decode(DynamicOps<O> ops, O input) {
                return ops.getMap(input).map(map -> {
                    PropertyValues decoded = PropertyValues.EMPTY;
                    for (PropertyDefinition<?> property : properties) {
                        decoded = decodeInto(decoded, property, ops, map);
                    }
                    return Pair.of(decoded, input);
                });
            }

            @Override
            public <O> DataResult<O> encode(PropertyValues input, DynamicOps<O> ops, O prefix) {
                RecordBuilder<O> builder = ops.mapBuilder();
                for (PropertyDefinition<?> property : properties) {
                    encodeInto(input, property, ops, builder);
                }
                return builder.build(prefix);
            }
        };
    }

    public static StreamCodec<RegistryFriendlyByteBuf, PropertyValues> streamCodec(List<? extends PropertyDefinition<?>> properties) {
        return StreamCodec.of(
                (buffer, values) -> {
                    for (PropertyDefinition<?> property : properties) {
                        write(values, property, buffer);
                    }
                },
                buffer -> {
                    PropertyValues decoded = PropertyValues.EMPTY;
                    for (PropertyDefinition<?> property : properties) {
                        decoded = read(decoded, property, buffer);
                    }
                    return decoded;
                });
    }

    private static <T, O> PropertyValues decodeInto(PropertyValues values, PropertyDefinition<T> property,
                                                    DynamicOps<O> ops, MapLike<O> map) {
        O input = map.get(property.name());
        if (input == null) {
            return values;
        }
        return property.type().codec().parse(ops, input).result()
                .map(value -> values.with(property, Optional.of(value)))
                .orElse(values);
    }

    private static <T, O> void encodeInto(PropertyValues values, PropertyDefinition<T> property,
                                          DynamicOps<O> ops, RecordBuilder<O> builder) {
        values.get(property).ifPresent(value -> builder.add(property.name(), property.type().codec().encodeStart(ops, value)));
    }

    private static <T> void write(PropertyValues values, PropertyDefinition<T> property, RegistryFriendlyByteBuf buffer) {
        Optional<T> value = values.get(property);
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(present -> property.type().streamCodec().encode(buffer, present));
    }

    private static <T> PropertyValues read(PropertyValues values, PropertyDefinition<T> property, RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return values;
        }
        return values.with(property, Optional.of(property.type().streamCodec().decode(buffer)));
    }
}

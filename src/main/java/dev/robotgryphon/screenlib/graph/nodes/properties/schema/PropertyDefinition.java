package dev.robotgryphon.screenlib.graph.nodes.properties.schema;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

import java.util.Locale;
import java.util.stream.Stream;

public sealed interface PropertyDefinition
        permits StoredPropertyDefinition, ConstantPropertyDefinition, ComputedPropertyDefinition {
    Codec<PropertyDefinition> CODEC = Codec.lazyInitialized(() -> new MapCodec<PropertyDefinition>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(StoredPropertyDefinition.MAP_CODEC, ConstantPropertyDefinition.MAP_CODEC,
                    ComputedPropertyDefinition.MAP_CODEC).flatMap(codec -> codec.keys(ops)).distinct();
        }

        @Override
        public <T> DataResult<PropertyDefinition> decode(DynamicOps<T> ops, MapLike<T> input) {
            if (input.get("computation") != null) {
                return ComputedPropertyDefinition.MAP_CODEC.decode(ops, input).map(property -> property);
            }
            if (input.get("value") != null) {
                return ConstantPropertyDefinition.MAP_CODEC.decode(ops, input).map(property -> property);
            }
            return StoredPropertyDefinition.MAP_CODEC.decode(ops, input).map(property -> property);
        }

        @Override
        public <T> RecordBuilder<T> encode(PropertyDefinition input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return switch (input) {
                case StoredPropertyDefinition<?> stored -> StoredPropertyDefinition.MAP_CODEC.encode(stored, ops, prefix);
                case ConstantPropertyDefinition constant -> ConstantPropertyDefinition.MAP_CODEC.encode(constant, ops, prefix);
                case ComputedPropertyDefinition computed -> ComputedPropertyDefinition.MAP_CODEC.encode(computed, ops, prefix);
            };
        }
    }.codec().validate(property -> property.name().equals(property.name().toLowerCase(Locale.ROOT))
            ? DataResult.success(property)
            : DataResult.error(() -> "Property name '" + property.name() + "' must be lowercase")));

    String name();

    Holder<GraphDataType<?>> anyType();
}

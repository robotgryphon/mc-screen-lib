package dev.robotgryphon.screenlib.graph.schema;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.schema.context.NodeContextSupplier;
import dev.robotgryphon.screenlib.graph.schema.ports.InputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.ports.OutputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.properties.DataKey;
import dev.robotgryphon.screenlib.graph.schema.properties.computed.ComputedProperty;
import dev.robotgryphon.screenlib.graph.schema.properties.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.properties.ConstantPropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.properties.PropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.properties.StoredPropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.types.GraphDataType;
import net.minecraft.core.Holder;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class NodeDefinitionCodecs {
    private NodeDefinitionCodecs() {}

    public static final MapCodec<StoredPropertyDefinition<?>> STORED_PROPERTY = RecordCodecBuilder.<StoredPropertyDefinition<?>>mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(StoredPropertyDefinition::name),
            GraphDataType.HOLDER_CODEC.fieldOf("type").forGetter(StoredPropertyDefinition::anyType),
            Codec.BOOL.optionalFieldOf("optional", false).forGetter(StoredPropertyDefinition::optional)
    ).apply(i, (name, type, optional) -> new StoredPropertyDefinition<>(name, type, type.value(), optional)));

    public static final MapCodec<ComputedPropertyDefinition<?>> COMPUTED_PROPERTY = RecordCodecBuilder.<ComputedPropertyDefinition<?>>mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(ComputedPropertyDefinition::name),
            ComputedProperty.HOLDER_CODEC.fieldOf("computation").forGetter(ComputedPropertyDefinition::computation),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("data", Map.of()).forGetter(ComputedPropertyDefinition::data)
    ).apply(i, (name, computation, data) -> new ComputedPropertyDefinition<>(name, computation, computation.value(), data)));

    public static final MapCodec<ConstantPropertyDefinition<?>> CONSTANT_PROPERTY = new MapCodec<>() {
        @Override
        public <O> Stream<O> keys(DynamicOps<O> ops) {
            return Stream.of(ops.createString("name"), ops.createString("type"), ops.createString("value"));
        }

        @Override
        public <O> DataResult<ConstantPropertyDefinition<?>> decode(DynamicOps<O> ops, MapLike<O> input) {
            O name = input.get("name");
            O type = input.get("type");
            O value = input.get("value");
            if (name == null || type == null || value == null) {
                return DataResult.error(() -> "Constant property requires a name, type and value");
            }
            return Codec.STRING.parse(ops, name).flatMap(parsedName -> GraphDataType.HOLDER_CODEC.parse(ops, type)
                    .flatMap(holder -> parseConstant(parsedName, holder, holder.value(), ops, value)));
        }

        @Override
        public <O> RecordBuilder<O> encode(ConstantPropertyDefinition<?> input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
            return prefix.add("name", ops.createString(input.name()))
                    .add("type", GraphDataType.HOLDER_CODEC.encodeStart(ops, input.anyType()))
                    .add("value", encodeConstant(input, ops));
        }
    };

    public static final Codec<PropertyDefinition<?>> PROPERTY = new MapCodec<PropertyDefinition<?>>() {
        @Override
        public <O> Stream<O> keys(DynamicOps<O> ops) {
            return Stream.of(STORED_PROPERTY, CONSTANT_PROPERTY, COMPUTED_PROPERTY)
                    .flatMap(codec -> codec.keys(ops)).distinct();
        }

        @Override
        public <O> DataResult<PropertyDefinition<?>> decode(DynamicOps<O> ops, MapLike<O> input) {
            if (input.get("computation") != null) {
                return COMPUTED_PROPERTY.decode(ops, input).map(property -> property);
            }
            if (input.get("value") != null) {
                return CONSTANT_PROPERTY.decode(ops, input).map(property -> property);
            }
            return STORED_PROPERTY.decode(ops, input).map(property -> property);
        }

        @Override
        public <O> RecordBuilder<O> encode(PropertyDefinition<?> input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
            return switch (input) {
                case StoredPropertyDefinition<?> stored -> STORED_PROPERTY.encode(stored, ops, prefix);
                case ConstantPropertyDefinition<?> constant -> CONSTANT_PROPERTY.encode(constant, ops, prefix);
                case ComputedPropertyDefinition<?> computed -> COMPUTED_PROPERTY.encode(computed, ops, prefix);
            };
        }
    }.codec().validate(property -> property.name().equals(property.name().toLowerCase(Locale.ROOT))
            ? DataResult.success(property)
            : DataResult.error(() -> "Property name '" + property.name() + "' must be lowercase"));

    public static final Codec<NodeDefinition> NODE = RecordCodecBuilder.<NodeDefinition>create(i -> i.group(
            InputPortDefinition.CODEC.listOf()
                    .optionalFieldOf("inputs", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.inputs())),

            OutputPortDefinition.CODEC.listOf()
                    .optionalFieldOf("outputs", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.outputs())),

            PROPERTY.listOf()
                    .optionalFieldOf("properties", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.properties())),

            NodeContextSupplier.HOLDER_CODEC.listOf()
                    .optionalFieldOf("context", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.context())),

            Codec.BOOL.optionalFieldOf("hidden", false)
                    .forGetter(NodeDefinition::hidden)
    ).apply(i, NodeDefinition::new)).validate(NodeDefinitionCodecs::validate);

    private static <T, O> DataResult<ConstantPropertyDefinition<?>> parseConstant(String name, Holder<GraphDataType<?>> holder,
                                                                                  GraphDataType<T> type, DynamicOps<O> ops, O value) {
        return type.codec().parse(ops, value).map(parsed -> new ConstantPropertyDefinition<>(name, holder, type, parsed));
    }

    private static <T, O> DataResult<O> encodeConstant(ConstantPropertyDefinition<T> constant, DynamicOps<O> ops) {
        return constant.type().codec().encodeStart(ops, constant.value());
    }

    private static DataResult<NodeDefinition> validate(NodeDefinition definition) {
        for (InputPortDefinition input : definition.inputs()) {
            var property = definition.property(input.property()).orElse(null);
            if (!(property instanceof StoredPropertyDefinition<?>)) {
                return DataResult.error(() -> "Input references '" + input.property()
                        + "', which is not a stored property");
            }
        }

        for (OutputPortDefinition output : definition.outputs()) {
            if (definition.property(output.property()).isEmpty()) {
                return DataResult.error(() -> "Output references unknown property '" + output.property() + "'");
            }
        }

        for (ComputedPropertyDefinition<?> computed : definition.computed()) {
            var error = validateData(definition, computed);
            if (error.isPresent()) {
                return DataResult.error(error::get);
            }
        }

        return DataResult.success(definition);
    }

    private static Optional<String> validateData(NodeDefinition definition, ComputedPropertyDefinition<?> computed) {
        String prefix = "Computed property '" + computed.name() + "'";
        for (var entry : computed.data().entrySet()) {
            if (definition.property(entry.getValue()).isEmpty()) {
                return Optional.of(prefix + " maps '" + entry.getKey() + "' to unknown property '" + entry.getValue() + "'");
            }
        }

        if (!computed.computation().isBound()) {
            return Optional.empty();
        }

        List<DataKey<?>> requires = computed.computation().value().requires();
        for (String key : computed.data().keySet()) {
            if (requires.stream().noneMatch(required -> required.name().equals(key))) {
                return Optional.of(prefix + " maps '" + key + "', which its computation does not use");
            }
        }

        for (DataKey<?> key : requires) {
            String mapped = computed.data().get(key.name());
            if (mapped == null) {
                if (!key.optional()) {
                    return Optional.of(prefix + " does not map required data '" + key.name() + "'");
                }
                continue;
            }
            var property = definition.property(mapped).orElseThrow();
            if (property.anyType().isBound() && key.type().isBound()
                    && property.anyType().value() != key.type().value()) {
                return Optional.of(prefix + " maps '" + key.name() + "' to '" + mapped + "', which has the wrong type");
            }
        }

        return Optional.empty();
    }
}

package dev.robotgryphon.screenlib.graph.nodes.schema;

import dev.robotgryphon.screenlib.graph.nodes.properties.schema.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.ConstantPropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.PropertyDefinition;
import dev.robotgryphon.screenlib.graph.nodes.properties.schema.StoredPropertyDefinition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.nodes.properties.DataKey;
import dev.robotgryphon.screenlib.graph.nodes.context.NodeContextSupplier;
import dev.robotgryphon.screenlib.graph.nodes.ports.InputPortDefinition;
import dev.robotgryphon.screenlib.graph.nodes.ports.OutputPortDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.codec.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record NodeDefinition(List<InputPortDefinition> inputs,
                             List<OutputPortDefinition> outputs,
                             List<PropertyDefinition> properties,
                             List<Holder<NodeContextSupplier>> context,
                             boolean hidden) {
    public static final ResourceKey<Registry<NodeDefinition>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("nodes"));

    public static final Codec<NodeDefinition> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.<NodeDefinition>create(i -> i.group(
            InputPortDefinition.CODEC.listOf()
                    .optionalFieldOf("inputs", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.inputs())),

            OutputPortDefinition.CODEC.listOf()
                    .optionalFieldOf("outputs", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.outputs())),

            PropertyDefinition.CODEC.listOf()
                    .optionalFieldOf("properties", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.properties())),

            NodeContextSupplier.HOLDER_CODEC.listOf()
                    .optionalFieldOf("context", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.context())),

            Codec.BOOL.optionalFieldOf("hidden", false)
                    .forGetter(NodeDefinition::hidden)
    ).apply(i, NodeDefinition::new)).validate(NodeDefinition::validate));

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
        for (ComputedPropertyDefinition computed : definition.computed()) {
            var error = validateData(definition, computed);
            if (error.isPresent()) {
                return DataResult.error(error::get);
            }
        }
        return DataResult.success(definition);
    }

    private static Optional<String> validateData(NodeDefinition definition, ComputedPropertyDefinition computed) {
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

    public Optional<PropertyDefinition> property(String name) {
        return this.properties.stream().filter(property -> property.name().equals(name)).findFirst();
    }

    public static final Codec<Holder<NodeDefinition>> HOLDER_CODEC = RegistryFixedCodec.create(REGISTRY_KEY);

    public List<StoredPropertyDefinition<?>> stored() {
        return this.properties.stream()
                .<StoredPropertyDefinition<?>>mapMulti((property, out) -> {
                    if (property instanceof StoredPropertyDefinition<?> stored) out.accept(stored);
                })
                .toList();
    }

    public List<ConstantPropertyDefinition> constants() {
        return this.properties.stream()
                .<ConstantPropertyDefinition>mapMulti((property, out) -> {
                    if (property instanceof ConstantPropertyDefinition constant) out.accept(constant);
                })
                .toList();
    }

    public List<ComputedPropertyDefinition> computed() {
        return this.properties.stream()
                .<ComputedPropertyDefinition>mapMulti((property, out) -> {
                    if (property instanceof ComputedPropertyDefinition computed) out.accept(computed);
                })
                .toList();
    }
}

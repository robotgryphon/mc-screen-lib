package dev.robotgryphon.screenlib.graph.schema;

import dev.robotgryphon.screenlib.graph.schema.properties.PropertyValues;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import dev.robotgryphon.screenlib.graph.schema.properties.ComputedPropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.properties.ConstantPropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.properties.PropertyDefinition;
import dev.robotgryphon.screenlib.graph.schema.properties.StoredPropertyDefinition;
import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.graph.schema.context.NodeContextSupplier;
import dev.robotgryphon.screenlib.graph.schema.ports.InputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.ports.OutputPortDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.codec.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.Optional;

public record NodeDefinition(List<InputPortDefinition> inputs,
                             List<OutputPortDefinition> outputs,
                             List<PropertyDefinition<?>> properties,
                             List<Holder<NodeContextSupplier>> context,
                             boolean hidden) {
    public static final ResourceKey<Registry<NodeDefinition>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("nodes"));

    public Codec<PropertyValues> storedValuesCodec() {
        return PropertyValues.codec(this.stored());
    }

    public StreamCodec<RegistryFriendlyByteBuf, PropertyValues> storedValuesStreamCodec() {
        return PropertyValues.streamCodec(this.stored());
    }

    public StreamCodec<RegistryFriendlyByteBuf, PropertyValues> computedValuesStreamCodec() {
        return PropertyValues.streamCodec(this.computed());
    }

    public Optional<PropertyDefinition<?>> property(String name) {
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

    public List<ConstantPropertyDefinition<?>> constants() {
        return this.properties.stream()
                .<ConstantPropertyDefinition<?>>mapMulti((property, out) -> {
                    if (property instanceof ConstantPropertyDefinition<?> constant) out.accept(constant);
                })
                .toList();
    }

    public List<ComputedPropertyDefinition<?>> computed() {
        return this.properties.stream()
                .<ComputedPropertyDefinition<?>>mapMulti((property, out) -> {
                    if (property instanceof ComputedPropertyDefinition<?> computed) out.accept(computed);
                })
                .toList();
    }
}

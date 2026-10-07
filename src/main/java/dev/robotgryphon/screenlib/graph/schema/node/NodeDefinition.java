package dev.robotgryphon.screenlib.graph.schema.node;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.schema.port.InputPortDefinition;
import dev.robotgryphon.screenlib.graph.schema.port.OutputPortDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.codec.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;

import java.util.Collections;
import java.util.List;

public record NodeDefinition(List<InputPortDefinition> inputs,
                             List<OutputPortDefinition> outputs,
                             List<NodeProperty<?>> properties,
                             List<ComputedPropertyDefinition> computed) {
    public static final ResourceKey<Registry<NodeDefinition>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("nodes"));

    public static final Codec<NodeDefinition> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            InputPortDefinition.CODEC.listOf()
                    .optionalFieldOf("inputs", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.inputs())),

            OutputPortDefinition.CODEC.listOf()
                    .optionalFieldOf("outputs", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.outputs())),

            NodeProperty.CODEC.listOf()
                    .optionalFieldOf("properties", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.properties())),

            ComputedPropertyDefinition.CODEC.listOf()
                    .optionalFieldOf("computed", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.computed()))
    ).apply(i, NodeDefinition::new)));

    public static final Codec<Holder<NodeDefinition>> HOLDER_CODEC = RegistryFixedCodec.create(REGISTRY_KEY);
}

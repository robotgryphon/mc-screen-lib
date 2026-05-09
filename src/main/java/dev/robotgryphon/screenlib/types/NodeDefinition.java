package dev.robotgryphon.screenlib.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.Collections;
import java.util.List;

public record NodeDefinition(List<PropertyType<?>> properties, List<String> inputs, List<String> outputs) {
    public static final ResourceKey<Registry<NodeDefinition>> REGISTRY_KEY = ResourceKey.createRegistryKey(ScreenLib.id("nodes"));

    public static Codec<NodeDefinition> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            PropertyType.CODEC.listOf()
                    .fieldOf("properties")
                    .forGetter(def -> Collections.unmodifiableList(def.properties)),

            Codec.STRING.listOf()
                    .fieldOf("inputs")
                    .forGetter(nodeDefinition -> Collections.unmodifiableList(nodeDefinition.inputs())),

            Codec.STRING.listOf()
                    .fieldOf("outputs")
                    .forGetter(nodeDefinition -> Collections.unmodifiableList(nodeDefinition.outputs()))
    ).apply(i, NodeDefinition::new)));
}

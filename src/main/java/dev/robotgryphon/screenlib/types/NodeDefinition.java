package dev.robotgryphon.screenlib.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.Collections;
import java.util.List;

/**
 * Datapack-defined description of a node type — what configurable
 * properties it exposes and the typed input/output ports it presents.
 *
 * <p>Inputs and outputs reference a {@link PropertyType} by id, which
 * is what gives each port its color when the canvas renders the node.
 */
public record NodeDefinition(List<PortDefinition> inputs,
                             List<PortDefinition> outputs) {

    public static final ResourceKey<Registry<NodeDefinition>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("nodes"));

    public static final Codec<NodeDefinition> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            PortDefinition.CODEC.listOf()
                    .optionalFieldOf("inputs", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.inputs())),

            PortDefinition.CODEC.listOf()
                    .optionalFieldOf("outputs", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.outputs()))
    ).apply(i, NodeDefinition::new)));
}

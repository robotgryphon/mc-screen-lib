package dev.robotgryphon.screenlib.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;

import java.util.Collections;
import java.util.List;

/**
 * Datapack-defined description of a node type — the typed input/output
 * ports it presents on its sides, and the configurable properties shown
 * inside its body.
 *
 * <p>All three lists are {@link PortDefinition}s. They share an identical
 * "name + typed holder" shape; the role (input, output, configurable
 * property) is just which list the entry lives in. Each entry's
 * {@link PortDefinition#type()} resolves to a registered
 * {@link PropertyDefinition} which carries the codec/color/displayName
 * for the value type, plus any default value for property entries.
 */
public record NodeDefinition(List<PortDefinition> inputs,
                             List<PortDefinition> outputs,
                             List<PortDefinition> properties) {

    public static final ResourceKey<Registry<NodeDefinition>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("nodes"));

    public static final Codec<NodeDefinition> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            PortDefinition.CODEC.listOf()
                    .optionalFieldOf("inputs", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.inputs())),

            PortDefinition.CODEC.listOf()
                    .optionalFieldOf("outputs", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.outputs())),

            // Properties share PortDefinition's schema: the "name" field
            // is the per-property key on the node (used for storage and
            // as the rendered label), and the "type" field resolves to
            // a registered PropertyDefinition whose default value, if
            // present, seeds the property when a fresh node is spawned.
            PortDefinition.CODEC.listOf()
                    .optionalFieldOf("properties", List.of())
                    .forGetter(def -> Collections.unmodifiableList(def.properties()))
    ).apply(i, NodeDefinition::new)));

    /**
     * Codec that serializes a {@link Holder} reference to a node definition
     * by its registered id (e.g. {@code "screenlib:block_position"}).
     * Resolved through the codec's {@code RegistryOps} at use time, so it
     * round-trips whether the registry was set up via data generation or at
     * runtime via the datapack.
     */
    public static final Codec<Holder<NodeDefinition>> HOLDER_CODEC = RegistryFixedCodec.create(REGISTRY_KEY);
}

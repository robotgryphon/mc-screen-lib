package dev.robotgryphon.screenlib.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;

/**
 * Schema-level description of a single port on a node.
 *
 * <p>This is the data form serialized into a {@link NodeDefinition}'s JSON.
 * The runtime/UI {@code Port} on a {@code NodeWidget} is built from one of
 * these — the widget reads the type's color to tint the port's diamond.
 *
 * @param name a human-facing label rendered next to the port (e.g. "Position", "x")
 * @param type the data type carried by the port; resolved through the
 *             property-types registry so color and codec are available at runtime
 */
public record PortDefinition(String name, Holder<PropertyType<?>> type) {

    public static final Codec<PortDefinition> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(PortDefinition::name),
            PropertyType.HOLDER_CODEC.fieldOf("type").forGetter(PortDefinition::type)
    ).apply(i, PortDefinition::new)));
}

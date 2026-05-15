package dev.robotgryphon.screenlib.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;

/**
 * Schema-level description of a single named, typed entry on a node — the
 * shared shape used for inputs, outputs, and properties. The "name" is a
 * local identifier (the port label for an input/output; the property key
 * for a node property), and the "type" is a reference to a registered
 * {@link PropertyDefinition} that carries the codec, color, display name,
 * and (for properties) any default value.
 *
 * <p>The same record serves all three roles because they share an
 * identical schema — a name plus a type holder. The role is determined
 * by which list on {@link NodeDefinition} the entry sits in
 * ({@code inputs}, {@code outputs}, or {@code properties}). Reusing the
 * record keeps the JSON form consistent across the three lists and means
 * downstream consumers (the runtime port builder, the data generator,
 * the preview filter) don't have to dispatch on shape.
 *
 * @param name a human-facing label / local key — e.g. "Position", "x",
 *             "seed". Used both as the renderable port label and as the
 *             persistence key for property values.
 * @param type the data type carried by this entry, resolved through the
 *             property-definitions registry so color, codec, and default
 *             are available at runtime.
 */
public record PortDefinition(String name, Holder<PropertyDefinition<?>> type) {

    public static final Codec<PortDefinition> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(PortDefinition::name),
            PropertyDefinition.HOLDER_CODEC.fieldOf("type").forGetter(PortDefinition::type)
    ).apply(i, PortDefinition::new)));
}

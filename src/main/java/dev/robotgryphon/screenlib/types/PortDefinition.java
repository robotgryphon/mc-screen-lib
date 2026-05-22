package dev.robotgryphon.screenlib.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;

import java.util.Optional;

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
 * <p>{@link #linkedProperty} is the bridge between an output port and a
 * property on the same node. Properties no longer expose right-side
 * "output" ports of their own — values flow OUT of a node only through
 * regular output ports — so a primitive node that wants to publish a
 * property's value declares an output with {@code linkedProperty} set to
 * that property's name. The widget layer reads through to the property's
 * current value when computing what a wire from that output carries
 * downstream. Inputs and properties leave it {@link Optional#empty()}.
 *
 * @param name           a human-facing label / local key — e.g. "Position", "x",
 *                       "seed". Used both as the renderable port label and as the
 *                       persistence key for property values.
 * @param type           the data type carried by this entry, resolved through the
 *                       property-definitions registry so color, codec, and default
 *                       are available at runtime.
 * @param linkedProperty for output ports, the name of a property on the
 *                       same node whose value this output relays; absent
 *                       for inputs, for properties, and for outputs that
 *                       don't shadow a property.
 */
public record PortDefinition(String name,
                             Holder<PropertyDefinition<?>> type,
                             Optional<String> linkedProperty) {

    /** Convenience constructor — no linked property, the common case. */
    public PortDefinition(String name, Holder<PropertyDefinition<?>> type) {
        this(name, type, Optional.empty());
    }

    /**
     * Datapack-facing codec. {@code linked_property} is optional so
     * pre-existing JSON entries — and the vast majority of port
     * definitions, which don't shadow any property — round-trip
     * unchanged. JSON form:
     * <pre>
     * { "name": "BOOLEAN", "type": "minecraft:bool", "linked_property": "value" }
     * </pre>
     */
    public static final Codec<PortDefinition> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(PortDefinition::name),
            PropertyDefinition.HOLDER_CODEC.fieldOf("type").forGetter(PortDefinition::type),
            Codec.STRING.optionalFieldOf("linked_property").forGetter(PortDefinition::linkedProperty)
    ).apply(i, PortDefinition::new)));
}

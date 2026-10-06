package dev.robotgryphon.screenlib.graph.ports;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

public record PropertyOutputPortDefinition(String name, Holder<GraphDataType<?>> type,
                                           String linkedProperty) implements OutputPortDefinition {
    public static final Codec<PropertyOutputPortDefinition> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(PropertyOutputPortDefinition::name),
            GraphDataType.HOLDER_CODEC.fieldOf("type").forGetter(PropertyOutputPortDefinition::type),
            Codec.STRING.fieldOf("linked_property").forGetter(PropertyOutputPortDefinition::linkedProperty)
    ).apply(i, PropertyOutputPortDefinition::new)));
}

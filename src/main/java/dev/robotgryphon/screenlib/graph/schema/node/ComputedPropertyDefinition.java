package dev.robotgryphon.screenlib.graph.schema.node;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.computed.ComputedProperty;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

public record ComputedPropertyDefinition(String name, Holder<GraphDataType<?>> type,
                                         Holder<ComputedProperty<?>> computation) {
    public static final Codec<ComputedPropertyDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(ComputedPropertyDefinition::name),
            GraphDataType.HOLDER_CODEC.fieldOf("type").forGetter(ComputedPropertyDefinition::type),
            ComputedProperty.HOLDER_CODEC.fieldOf("computation").forGetter(ComputedPropertyDefinition::computation)
    ).apply(i, ComputedPropertyDefinition::new));
}

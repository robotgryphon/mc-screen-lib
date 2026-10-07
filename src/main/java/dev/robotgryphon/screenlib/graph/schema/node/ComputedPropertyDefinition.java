package dev.robotgryphon.screenlib.graph.schema.node;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.computed.ComputedProperty;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

public record ComputedPropertyDefinition(String name, Holder<GraphDataType<?>> type,
                                         Holder<ComputedProperty<?>> computation, boolean hidden) {
    public ComputedPropertyDefinition(String name, Holder<GraphDataType<?>> type,
                                      Holder<ComputedProperty<?>> computation) {
        this(name, type, computation, false);
    }

    public static final Codec<ComputedPropertyDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(ComputedPropertyDefinition::name),
            GraphDataType.HOLDER_CODEC.fieldOf("type").forGetter(ComputedPropertyDefinition::type),
            ComputedProperty.HOLDER_CODEC.fieldOf("computation").forGetter(ComputedPropertyDefinition::computation),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(ComputedPropertyDefinition::hidden)
    ).apply(i, ComputedPropertyDefinition::new));
}

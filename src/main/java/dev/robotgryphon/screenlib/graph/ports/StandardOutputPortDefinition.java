package dev.robotgryphon.screenlib.graph.ports;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

public record StandardOutputPortDefinition(String name, Holder<GraphDataType<?>> type) implements OutputPortDefinition {
    public static final Codec<StandardOutputPortDefinition> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(StandardOutputPortDefinition::name),
            GraphDataType.HOLDER_CODEC.fieldOf("type").forGetter(StandardOutputPortDefinition::type)
    ).apply(i, StandardOutputPortDefinition::new)));
}

package dev.robotgryphon.screenlib.graph.schema.port;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

public record InputPortDefinition(String name, Holder<GraphDataType<?>> type,
                                  boolean optional) implements PortDefinition {
    public InputPortDefinition(String name, Holder<GraphDataType<?>> type) {
        this(name, type, false);
    }

    public static final Codec<InputPortDefinition> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(InputPortDefinition::name),
            GraphDataType.HOLDER_CODEC.fieldOf("type").forGetter(InputPortDefinition::type),
            Codec.BOOL.optionalFieldOf("optional", false).forGetter(InputPortDefinition::optional)
    ).apply(i, InputPortDefinition::new)));
}

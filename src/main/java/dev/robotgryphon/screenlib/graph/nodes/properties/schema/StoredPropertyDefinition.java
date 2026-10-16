package dev.robotgryphon.screenlib.graph.nodes.properties.schema;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

public record StoredPropertyDefinition<T>(String name, Holder<GraphDataType<?>> anyType, GraphDataType<T> type,
                                          boolean optional)
        implements PropertyDefinition<T> {
    public static final MapCodec<StoredPropertyDefinition<?>> MAP_CODEC = RecordCodecBuilder.<StoredPropertyDefinition<?>>mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(StoredPropertyDefinition::name),
            GraphDataType.HOLDER_CODEC.fieldOf("type").forGetter(StoredPropertyDefinition::anyType),
            Codec.BOOL.optionalFieldOf("optional", false).forGetter(StoredPropertyDefinition::optional)
    ).apply(i, (name, type, optional) -> new StoredPropertyDefinition<>(name, type, type.value(), optional)));
}

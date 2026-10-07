package dev.robotgryphon.screenlib.graph.schema.node;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

public record NodeProperty<T>(String name, Holder<GraphDataType<T>> type, boolean optional) {
    public NodeProperty(String name, Holder<GraphDataType<T>> type) {
        this(name, type, false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Holder<GraphDataType<?>> anyType() {
        return (Holder) this.type;
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Codec<NodeProperty<?>> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(NodeProperty::name),
            GraphDataType.HOLDER_CODEC.fieldOf("type").forGetter(p -> (Holder<GraphDataType<?>>) (Holder) p.type()),
            Codec.BOOL.optionalFieldOf("optional", false).forGetter(NodeProperty::optional)
    ).apply(i, (name, type, optional) -> new NodeProperty(name, type, optional)));
}

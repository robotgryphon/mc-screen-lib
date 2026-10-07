package dev.robotgryphon.screenlib.graph.schema.port;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

public record ConstantOutputPortDefinition(String name, Holder<GraphDataType<?>> type,
                                           Object value) implements OutputPortDefinition {
    private record Typed(Holder<GraphDataType<?>> type, Object value) {
        @SuppressWarnings("unchecked")
        static final MapCodec<Typed> CODEC = GraphDataType.HOLDER_CODEC.fieldOf("type").dispatchMap(
                Typed::type,
                holder -> ((Codec<Object>) holder.value().codec()).fieldOf("value")
                        .xmap(value -> new Typed(holder, value), Typed::value));
    }

    public static final Codec<ConstantOutputPortDefinition> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(ConstantOutputPortDefinition::name),
            Typed.CODEC.forGetter(d -> new Typed(d.type, d.value))
    ).apply(i, (name, typed) -> new ConstantOutputPortDefinition(name, typed.type(), typed.value()))));
}

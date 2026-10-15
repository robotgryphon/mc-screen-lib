package dev.robotgryphon.screenlib.graph.nodes.properties.schema;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

public record ConstantPropertyDefinition(String name, Holder<GraphDataType<?>> type,
                                         Object value) implements PropertyDefinition {
    private record Typed(Holder<GraphDataType<?>> type, Object value) {
        @SuppressWarnings("unchecked")
        static final MapCodec<Typed> CODEC = GraphDataType.HOLDER_CODEC.fieldOf("type").dispatchMap(
                Typed::type,
                holder -> ((Codec<Object>) holder.value().codec()).fieldOf("value")
                        .xmap(value -> new Typed(holder, value), Typed::value));
    }

    public static final MapCodec<ConstantPropertyDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(ConstantPropertyDefinition::name),
            Typed.CODEC.forGetter(d -> new Typed(d.type, d.value))
    ).apply(i, (name, typed) -> new ConstantPropertyDefinition(name, typed.type(), typed.value())));

    @Override
    public Holder<GraphDataType<?>> anyType() {
        return this.type;
    }
}

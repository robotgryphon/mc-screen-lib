package dev.robotgryphon.screenlib.graph.nodes.properties.schema;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;

import java.util.stream.Stream;

public record ConstantPropertyDefinition<T>(String name, Holder<GraphDataType<?>> anyType, GraphDataType<T> type,
                                            T value) implements PropertyDefinition<T> {
    public static final MapCodec<ConstantPropertyDefinition<?>> MAP_CODEC = new MapCodec<>() {
        @Override
        public <O> Stream<O> keys(DynamicOps<O> ops) {
            return Stream.of(ops.createString("name"), ops.createString("type"), ops.createString("value"));
        }

        @Override
        public <O> DataResult<ConstantPropertyDefinition<?>> decode(DynamicOps<O> ops, MapLike<O> input) {
            O name = input.get("name");
            O type = input.get("type");
            O value = input.get("value");
            if (name == null || type == null || value == null) {
                return DataResult.error(() -> "Constant property requires a name, type and value");
            }
            return Codec.STRING.parse(ops, name).flatMap(parsedName -> GraphDataType.HOLDER_CODEC.parse(ops, type)
                    .flatMap(holder -> parse(parsedName, holder, holder.value(), ops, value)));
        }

        @Override
        public <O> RecordBuilder<O> encode(ConstantPropertyDefinition<?> input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
            return prefix.add("name", ops.createString(input.name()))
                    .add("type", GraphDataType.HOLDER_CODEC.encodeStart(ops, input.anyType()))
                    .add("value", input.encodeValue(ops));
        }
    };

    private static <T, O> DataResult<ConstantPropertyDefinition<?>> parse(String name, Holder<GraphDataType<?>> holder,
                                                                          GraphDataType<T> type, DynamicOps<O> ops, O value) {
        return type.codec().parse(ops, value).map(parsed -> new ConstantPropertyDefinition<>(name, holder, type, parsed));
    }

    private <O> DataResult<O> encodeValue(DynamicOps<O> ops) {
        return this.type.codec().encodeStart(ops, this.value);
    }
}

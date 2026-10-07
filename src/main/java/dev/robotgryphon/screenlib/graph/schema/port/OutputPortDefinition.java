package dev.robotgryphon.screenlib.graph.schema.port;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;

public sealed interface OutputPortDefinition extends PortDefinition
        permits StandardOutputPortDefinition, PropertyOutputPortDefinition, ConstantOutputPortDefinition {
    Codec<OutputPortDefinition> CODEC = Codec.lazyInitialized(() -> Codec.of(new Encoder<>() {
        @Override
        public <T> DataResult<T> encode(OutputPortDefinition input, DynamicOps<T> ops, T prefix) {
            return switch (input) {
                case ConstantOutputPortDefinition c -> ConstantOutputPortDefinition.CODEC.encode(c, ops, prefix);
                case PropertyOutputPortDefinition p -> PropertyOutputPortDefinition.CODEC.encode(p, ops, prefix);
                case StandardOutputPortDefinition s -> StandardOutputPortDefinition.CODEC.encode(s, ops, prefix);
            };
        }
    }, new Decoder<>() {
        @Override
        public <T> DataResult<Pair<OutputPortDefinition, T>> decode(DynamicOps<T> ops, T input) {
            var constant = ConstantOutputPortDefinition.CODEC.decode(ops, input);
            if (constant.result().isPresent()) return constant.map(p -> p.mapFirst(d -> d));
            var property = PropertyOutputPortDefinition.CODEC.decode(ops, input);
            if (property.result().isPresent()) return property.map(p -> p.mapFirst(d -> d));
            return StandardOutputPortDefinition.CODEC.decode(ops, input).map(p -> p.mapFirst(d -> d));
        }
    }));
}

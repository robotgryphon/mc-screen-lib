package dev.robotgryphon.screenlib.graph.nodes.properties.schema;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.nodes.properties.computed.ComputeContext;
import dev.robotgryphon.screenlib.graph.nodes.properties.computed.ComputedProperty;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;

import java.util.Map;

public record ComputedPropertyDefinition<T>(String name, Holder<ComputedProperty<?>> computation,
                                            ComputedProperty<T> function, Map<String, String> data)
        implements PropertyDefinition<T> {
    public static final MapCodec<ComputedPropertyDefinition<?>> MAP_CODEC = RecordCodecBuilder.<ComputedPropertyDefinition<?>>mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(ComputedPropertyDefinition::name),
            ComputedProperty.HOLDER_CODEC.fieldOf("computation").forGetter(ComputedPropertyDefinition::computation),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("data", Map.of()).forGetter(ComputedPropertyDefinition::data)
    ).apply(i, (name, computation, data) -> new ComputedPropertyDefinition<>(name, computation, computation.value(), data)));

    @Override
    public Holder<GraphDataType<?>> anyType() {
        return this.function.type();
    }

    @Override
    public GraphDataType<T> type() {
        return this.function.type().get();
    }

    public ComputeContext context(Node node, MinecraftServer server) {
        return new ComputeContext(node, this.data, server);
    }
}

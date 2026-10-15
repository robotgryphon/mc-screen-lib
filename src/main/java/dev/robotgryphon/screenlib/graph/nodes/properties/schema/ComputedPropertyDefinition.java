package dev.robotgryphon.screenlib.graph.nodes.properties.schema;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.graph.nodes.properties.computed.ClientDataProvider;
import dev.robotgryphon.screenlib.graph.nodes.properties.computed.ComputeContext;
import dev.robotgryphon.screenlib.graph.nodes.properties.computed.ComputedProperty;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.Optional;

public record ComputedPropertyDefinition(String name, Holder<GraphDataType<?>> type,
                                         Holder<ComputedProperty<?>> computation,
                                         Map<String, String> data)
        implements PropertyDefinition {
    public static final MapCodec<ComputedPropertyDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(ComputedPropertyDefinition::name),
            GraphDataType.HOLDER_CODEC.fieldOf("type").forGetter(ComputedPropertyDefinition::type),
            ComputedProperty.HOLDER_CODEC.fieldOf("computation").forGetter(ComputedPropertyDefinition::computation),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("data", Map.of()).forGetter(ComputedPropertyDefinition::data)
    ).apply(i, ComputedPropertyDefinition::new));

    @Override
    public Holder<GraphDataType<?>> anyType() {
        return this.type;
    }

    public ComputeContext context(Node node, MinecraftServer server) {
        return new ComputeContext(node, this.data, server);
    }

    @SuppressWarnings("unchecked")
    public Optional<ClientDataProvider<Object, Object>> clientData() {
        return this.computation.value() instanceof ClientDataProvider<?, ?> provider
                ? Optional.of((ClientDataProvider<Object, Object>) provider)
                : Optional.empty();
    }
}

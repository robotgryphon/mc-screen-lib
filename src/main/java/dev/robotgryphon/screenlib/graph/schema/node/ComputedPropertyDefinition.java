package dev.robotgryphon.screenlib.graph.schema.node;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.client.render.PropertyValueRenderer;
import dev.robotgryphon.screenlib.graph.computed.ClientDataProvider;
import dev.robotgryphon.screenlib.graph.computed.ComputeContext;
import dev.robotgryphon.screenlib.graph.computed.ComputedProperty;
import dev.robotgryphon.screenlib.graph.nodes.Node;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

import java.util.Map;
import java.util.Optional;

public record ComputedPropertyDefinition(String name, Holder<GraphDataType<?>> type,
                                         Holder<ComputedProperty<?>> computation,
                                         Map<String, String> data,
                                         boolean hideLabel,
                                         Optional<ResourceKey<PropertyValueRenderer<?>>> renderer)
        implements PropertyDefinition {
    public static final MapCodec<ComputedPropertyDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(ComputedPropertyDefinition::name),
            GraphDataType.HOLDER_CODEC.fieldOf("type").forGetter(ComputedPropertyDefinition::type),
            ComputedProperty.HOLDER_CODEC.fieldOf("computation").forGetter(ComputedPropertyDefinition::computation),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("data", Map.of()).forGetter(ComputedPropertyDefinition::data),
            Codec.BOOL.optionalFieldOf("hide_label", false).forGetter(ComputedPropertyDefinition::hideLabel),
            PropertyDefinition.RENDERER_CODEC.optionalFieldOf("renderer").forGetter(ComputedPropertyDefinition::renderer)
    ).apply(i, ComputedPropertyDefinition::new));

    @Override
    public Holder<GraphDataType<?>> anyType() {
        return this.type;
    }

    public ComputeContext context(Node node) {
        return new ComputeContext(node, this.data);
    }

    @SuppressWarnings("unchecked")
    public Optional<ClientDataProvider<Object, Object>> clientData() {
        return this.computation.value() instanceof ClientDataProvider<?, ?> provider
                ? Optional.of((ClientDataProvider<Object, Object>) provider)
                : Optional.empty();
    }
}

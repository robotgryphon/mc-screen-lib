package dev.robotgryphon.screenlib.graph.schema.node;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.client.render.PropertyValueRenderer;
import dev.robotgryphon.screenlib.graph.types.GraphDataType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;

public record StoredPropertyDefinition<T>(String name, Holder<GraphDataType<T>> type,
                                          boolean optional, boolean hideLabel,
                                          Optional<ResourceKey<PropertyValueRenderer<?>>> renderer)
        implements PropertyDefinition {
    public StoredPropertyDefinition(String name, Holder<GraphDataType<T>> type) {
        this(name, type, false, false, Optional.empty());
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Holder<GraphDataType<?>> anyType() {
        return (Holder) this.type;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final MapCodec<StoredPropertyDefinition<?>> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(StoredPropertyDefinition::name),
            GraphDataType.HOLDER_CODEC.fieldOf("type").forGetter(StoredPropertyDefinition::anyType),
            Codec.BOOL.optionalFieldOf("optional", false).forGetter(StoredPropertyDefinition::optional),
            Codec.BOOL.optionalFieldOf("hide_label", false).forGetter(StoredPropertyDefinition::hideLabel),
            PropertyDefinition.RENDERER_CODEC.optionalFieldOf("renderer").forGetter(StoredPropertyDefinition::renderer)
    ).apply(i, (name, type, optional, hideLabel, renderer) ->
            new StoredPropertyDefinition(name, type, optional, hideLabel, renderer)));
}

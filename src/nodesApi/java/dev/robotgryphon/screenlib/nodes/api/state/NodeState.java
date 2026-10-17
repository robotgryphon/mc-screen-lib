package dev.robotgryphon.screenlib.nodes.api.state;

import dev.robotgryphon.screenlib.graph.schema.properties.PropertyValues;
import dev.robotgryphon.screenlib.graph.schema.properties.StoredPropertyDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.robotgryphon.screenlib.core.ScreenLibCodecs;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.joml.Vector2ic;

import java.util.Optional;
import java.util.UUID;

public record NodeState(UUID id, Holder<NodeDefinition> definition, Vector2ic position,
                        PropertyValues propertyValues) {
    public static final Codec<NodeState> CODEC =
            NodeDefinition.HOLDER_CODEC.dispatch("definition", NodeState::definition, NodeState::contentCodec);

    public static final StreamCodec<RegistryFriendlyByteBuf, NodeState> STREAM_CODEC =
            ByteBufCodecs.holderRegistry(NodeDefinition.REGISTRY_KEY).dispatch(NodeState::definition, NodeState::contentStreamCodec);

    private static MapCodec<NodeState> contentCodec(Holder<NodeDefinition> definition) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(NodeState::id),
                ScreenLibCodecs.VECTOR2I.fieldOf("position").forGetter(NodeState::position),
                definition.value().storedValuesCodec()
                        .optionalFieldOf("properties", PropertyValues.EMPTY)
                        .forGetter(NodeState::propertyValues)
        ).apply(i, (id, position, values) -> new NodeState(id, definition, position, values)));
    }

    private static StreamCodec<RegistryFriendlyByteBuf, NodeState> contentStreamCodec(Holder<NodeDefinition> definition) {
        return StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, NodeState::id,
                ScreenLibCodecs.VECTOR2I_STREAM, NodeState::position,
                definition.value().storedValuesStreamCodec(), NodeState::propertyValues,
                (id, position, values) -> new NodeState(id, definition, position, values)
        );
    }

    public NodeState withPosition(Vector2ic position) {
        return new NodeState(this.id, this.definition, position, this.propertyValues);
    }

    public <T> NodeState withProperty(StoredPropertyDefinition<T> property, Optional<T> value) {
        return new NodeState(this.id, this.definition, this.position, this.propertyValues.with(property, value));
    }
}

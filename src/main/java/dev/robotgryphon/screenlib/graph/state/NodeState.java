package dev.robotgryphon.screenlib.graph.state;

import dev.robotgryphon.screenlib.ScreenLibCodecs;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.joml.Vector2ic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record NodeState(UUID id, Holder<NodeDefinition> definition, Vector2ic position,
                        Map<String, Dynamic<?>> propertyValues) {
    public static final Codec<NodeState> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(NodeState::id),
            NodeDefinition.HOLDER_CODEC.fieldOf("definition").forGetter(NodeState::definition),
            ScreenLibCodecs.VECTOR2I.fieldOf("position").forGetter(NodeState::position),
            Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH)
                    .optionalFieldOf("properties", Map.of())
                    .forGetter(NodeState::propertyValues)
    ).apply(i, NodeState::new));

    private static final StreamCodec<ByteBuf, Dynamic<?>> PROPERTY_VALUE_STREAM_CODEC = ByteBufCodecs.TAG.map(
            tag -> new Dynamic<>(NbtOps.INSTANCE, tag),
            value -> value.convert(NbtOps.INSTANCE).getValue());

    public static final StreamCodec<RegistryFriendlyByteBuf, NodeState> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, NodeState::id,
            ByteBufCodecs.holderRegistry(NodeDefinition.REGISTRY_KEY), NodeState::definition,
            ScreenLibCodecs.VECTOR2I_STREAM, NodeState::position,
            ByteBufCodecs.<ByteBuf, String, Dynamic<?>, Map<String, Dynamic<?>>>map(
                    HashMap::new, ByteBufCodecs.STRING_UTF8, PROPERTY_VALUE_STREAM_CODEC), NodeState::propertyValues,
            NodeState::new
    );
}

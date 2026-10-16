package dev.robotgryphon.screenlib.graph.sync;

import dev.robotgryphon.screenlib.graph.nodes.properties.PropertyValues;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record NodeComputed(UUID node, Holder<NodeDefinition> definition, PropertyValues values) {
    public static final StreamCodec<RegistryFriendlyByteBuf, NodeComputed> STREAM_CODEC =
            ByteBufCodecs.holderRegistry(NodeDefinition.REGISTRY_KEY).dispatch(NodeComputed::definition, NodeComputed::contentStreamCodec);

    private static StreamCodec<RegistryFriendlyByteBuf, NodeComputed> contentStreamCodec(Holder<NodeDefinition> definition) {
        return StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, NodeComputed::node,
                definition.value().computedValuesStreamCodec(), NodeComputed::values,
                (node, values) -> new NodeComputed(node, definition, values)
        );
    }
}

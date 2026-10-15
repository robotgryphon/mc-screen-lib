package dev.robotgryphon.screenlib.graph.sync;

import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

public record ClientDataSubscription(ResourceKey<NodeDefinition> node, String property) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientDataSubscription> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(NodeDefinition.REGISTRY_KEY), ClientDataSubscription::node,
            ByteBufCodecs.STRING_UTF8, ClientDataSubscription::property,
            ClientDataSubscription::new
    );
}

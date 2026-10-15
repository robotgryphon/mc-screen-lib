package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;

public record AddNodeResultPayload(Optional<ResourceKey<NodeDefinition>> selected) implements CustomPacketPayload {
    public static final Type<AddNodeResultPayload> TYPE =
            new Type<>(ScreenLib.id("add_node_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddNodeResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.optional(ResourceKey.streamCodec(NodeDefinition.REGISTRY_KEY)),
                    AddNodeResultPayload::selected,
                    AddNodeResultPayload::new
            );

    @Override
    public Type<AddNodeResultPayload> type() {
        return TYPE;
    }
}

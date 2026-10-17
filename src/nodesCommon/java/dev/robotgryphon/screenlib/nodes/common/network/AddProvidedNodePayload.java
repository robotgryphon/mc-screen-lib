package dev.robotgryphon.screenlib.nodes.common.network;

import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.core.ScreenLibCodecs;
import dev.robotgryphon.screenlib.graph.schema.NodeDefinition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import org.joml.Vector2ic;

public record AddProvidedNodePayload(ResourceKey<NodeDefinition> definition, Vector2ic position) implements CustomPacketPayload {
    public static final Type<AddProvidedNodePayload> TYPE = new Type<>(ScreenLib.id("add_provided_node"));

    public static final StreamCodec<ByteBuf, AddProvidedNodePayload> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(NodeDefinition.REGISTRY_KEY), AddProvidedNodePayload::definition,
            ScreenLibCodecs.VECTOR2I_STREAM, AddProvidedNodePayload::position,
            AddProvidedNodePayload::new
    );

    @Override
    public Type<AddProvidedNodePayload> type() {
        return TYPE;
    }
}

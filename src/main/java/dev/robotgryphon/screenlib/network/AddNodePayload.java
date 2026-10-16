package dev.robotgryphon.screenlib.network;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.ScreenLibCodecs;
import dev.robotgryphon.screenlib.graph.nodes.schema.NodeDefinition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import org.joml.Vector2ic;

public record AddNodePayload(ResourceKey<NodeDefinition> definition, Vector2ic position) implements CustomPacketPayload {
    public static final Type<AddNodePayload> TYPE = new Type<>(ScreenLib.id("add_node"));

    public static final StreamCodec<ByteBuf, AddNodePayload> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(NodeDefinition.REGISTRY_KEY), AddNodePayload::definition,
            ScreenLibCodecs.VECTOR2I_STREAM, AddNodePayload::position,
            AddNodePayload::new
    );

    @Override
    public Type<AddNodePayload> type() {
        return TYPE;
    }
}

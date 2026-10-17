package dev.robotgryphon.screenlib.nodes.api.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record ConnectionState(UUID sourceNode, String sourcePort, UUID targetNode, String targetPort) {
    public static final Codec<ConnectionState> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("source_node").forGetter(ConnectionState::sourceNode),
            Codec.STRING.fieldOf("source_port").forGetter(ConnectionState::sourcePort),
            UUIDUtil.CODEC.fieldOf("target_node").forGetter(ConnectionState::targetNode),
            Codec.STRING.fieldOf("target_port").forGetter(ConnectionState::targetPort)
    ).apply(i, ConnectionState::new));

    public static final StreamCodec<ByteBuf, ConnectionState> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ConnectionState::sourceNode,
            ByteBufCodecs.STRING_UTF8, ConnectionState::sourcePort,
            UUIDUtil.STREAM_CODEC, ConnectionState::targetNode,
            ByteBufCodecs.STRING_UTF8, ConnectionState::targetPort,
            ConnectionState::new
    );
}

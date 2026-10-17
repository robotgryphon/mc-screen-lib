package dev.robotgryphon.screenlib.nodes.common.sync;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record NodeClientData(UUID node, String property, byte[] data) {
    public static final StreamCodec<RegistryFriendlyByteBuf, NodeClientData> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            NodeClientData::node,
            ByteBufCodecs.STRING_UTF8,
            NodeClientData::property,
            ByteBufCodecs.BYTE_ARRAY,
            NodeClientData::data,
            NodeClientData::new
    );
}

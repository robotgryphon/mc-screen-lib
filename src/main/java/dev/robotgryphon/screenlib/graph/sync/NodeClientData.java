package dev.robotgryphon.screenlib.graph.sync;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;

public record NodeClientData(int nodeIndex, String property, byte[] data) {
    public static final StreamCodec<RegistryFriendlyByteBuf, NodeClientData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            NodeClientData::nodeIndex,
            ByteBufCodecs.STRING_UTF8,
            NodeClientData::property,
            ByteBufCodecs.BYTE_ARRAY,
            NodeClientData::data,
            NodeClientData::new
    );
}

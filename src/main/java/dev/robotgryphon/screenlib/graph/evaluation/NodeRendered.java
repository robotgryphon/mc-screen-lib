package dev.robotgryphon.screenlib.graph.evaluation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record NodeRendered(int nodeIndex, int providerIndex, byte[] data) {
    public static final StreamCodec<RegistryFriendlyByteBuf, NodeRendered> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            NodeRendered::nodeIndex,
            ByteBufCodecs.VAR_INT,
            NodeRendered::providerIndex,
            ByteBufCodecs.BYTE_ARRAY,
            NodeRendered::data,
            NodeRendered::new
    );
}

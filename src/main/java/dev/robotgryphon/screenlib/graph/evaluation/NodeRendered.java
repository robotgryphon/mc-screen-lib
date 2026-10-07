package dev.robotgryphon.screenlib.graph.evaluation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;

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

    @Override
    public boolean equals(Object other) {
        return other instanceof NodeRendered rendered
                && this.nodeIndex == rendered.nodeIndex
                && this.providerIndex == rendered.providerIndex
                && Arrays.equals(this.data, rendered.data);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * this.nodeIndex + this.providerIndex) + Arrays.hashCode(this.data);
    }
}

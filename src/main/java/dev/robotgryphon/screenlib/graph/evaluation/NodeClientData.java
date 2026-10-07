package dev.robotgryphon.screenlib.graph.evaluation;

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

    @Override
    public boolean equals(Object other) {
        return other instanceof NodeClientData clientData
                && this.nodeIndex == clientData.nodeIndex
                && this.property.equals(clientData.property)
                && Arrays.equals(this.data, clientData.data);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * this.nodeIndex + this.property.hashCode()) + Arrays.hashCode(this.data);
    }
}

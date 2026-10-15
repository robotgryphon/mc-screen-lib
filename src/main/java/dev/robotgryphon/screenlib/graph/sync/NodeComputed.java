package dev.robotgryphon.screenlib.graph.sync;

import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

public record NodeComputed(int nodeIndex, Map<String, Tag> values) {
    public static final StreamCodec<RegistryFriendlyByteBuf, NodeComputed> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            NodeComputed::nodeIndex,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.TAG),
            NodeComputed::values,
            NodeComputed::new
    );
}

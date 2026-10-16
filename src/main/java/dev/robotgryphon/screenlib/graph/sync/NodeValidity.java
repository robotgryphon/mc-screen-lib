package dev.robotgryphon.screenlib.graph.sync;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.UUID;

public record NodeValidity(UUID node, List<String> errors) {
    public static final StreamCodec<RegistryFriendlyByteBuf, NodeValidity> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            NodeValidity::node,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            NodeValidity::errors,
            NodeValidity::new
    );
}

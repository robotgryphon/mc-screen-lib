package dev.robotgryphon.screenlib.graph.sync;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record NodeValidity(int nodeIndex, List<String> errors) {
    public static final StreamCodec<RegistryFriendlyByteBuf, NodeValidity> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            NodeValidity::nodeIndex,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            NodeValidity::errors,
            NodeValidity::new
    );
}

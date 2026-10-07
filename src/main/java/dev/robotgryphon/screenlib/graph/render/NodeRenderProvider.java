package dev.robotgryphon.screenlib.graph.render;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.graph.computed.ComputeContext;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;

public interface NodeRenderProvider<S> {
    ResourceKey<Registry<NodeRenderProvider<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("render_providers"));

    Codec<Holder<NodeRenderProvider<?>>> HOLDER_CODEC = RegistryCodecs.holder(REGISTRY_KEY);

    int width();

    int height();

    StreamCodec<RegistryFriendlyByteBuf, S> codec();

    Optional<S> extract(ComputeContext context);
}

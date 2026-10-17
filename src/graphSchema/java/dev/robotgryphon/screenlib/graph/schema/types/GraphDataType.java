package dev.robotgryphon.screenlib.graph.schema.types;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.core.ScreenLib;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.ResourceKey;

public interface GraphDataType<T> {
    ResourceKey<Registry<GraphDataType<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("data_types"));

    Codec<Holder<GraphDataType<?>>> HOLDER_CODEC = RegistryCodecs.holder(REGISTRY_KEY);

    static Component displayName(Holder<GraphDataType<?>> type) {
        var id = type.unwrapKey().orElseThrow().identifier();
        return Component.translatableWithFallback(id.toLanguageKey("graph_type"), id.getPath());
    }

    Class<T> dataType();

    Codec<T> codec();

    StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec();
}

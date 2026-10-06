package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.ResourceKey;

import java.util.Collection;
import java.util.Optional;

public interface GraphDataType<T> {
    ResourceKey<Registry<GraphDataType<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("data_types"));

    Codec<Holder<GraphDataType<?>>> HOLDER_CODEC = RegistryCodecs.holder(REGISTRY_KEY);

    int DEFAULT_COLOR = 0xFF8FA0FF;

    static Component displayName(Holder<GraphDataType<?>> type) {
        var id = type.unwrapKey().orElseThrow().identifier();
        return Component.translatableWithFallback(id.toLanguageKey("graph_type"), id.getPath());
    }

    Class<T> dataType();

    default int color() {
        return DEFAULT_COLOR;
    }

    Codec<T> codec();

    default Optional<T> defaultValueRaw() {
        return Optional.empty();
    }

    default Optional<Collection<T>> allowedValues() {
        return Optional.empty();
    }
}

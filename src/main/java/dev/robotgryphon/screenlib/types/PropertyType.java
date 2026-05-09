package dev.robotgryphon.screenlib.types;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.ScreenLib;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;

/**
 * A registered "data type" that nodes can produce or consume on a port.
 *
 * <p>Each {@code PropertyType} pairs a Codec (used to serialize values of
 * that type) with a UI color and display name. The color is read by the
 * widget renderer to tint a port's diamond, so that two ports of the same
 * type are visually identifiable as compatible at a glance.
 *
 * @param <T> the value carried by this type
 */
public record PropertyType<T>(Codec<T> codec, int color, Component displayName) {

    /** Registry key for this registry. {@link ScreenLib#PROPERTY_TYPES} backs it. */
    public static final ResourceKey<Registry<PropertyType<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ScreenLib.id("property_types"));

    /** Convenience constant — used for ports whose type doesn't carry a strong color. */
    public static final int DEFAULT_COLOR = 0xFF8FA0FF;

    /**
     * Codec that serializes a PropertyType reference by its registered id.
     * Resolution is lazy because the registry is built up at mod-bus time.
     */
    public static final Codec<PropertyType<?>> CODEC = Codec.lazyInitialized(() -> ScreenLib.PROPERTY_TYPES
            .getRegistry()
            .get()
            .byNameCodec());

    public static final Codec<Holder<PropertyType<?>>> HOLDER_CODEC = RegistryFixedCodec.create(REGISTRY_KEY);
}

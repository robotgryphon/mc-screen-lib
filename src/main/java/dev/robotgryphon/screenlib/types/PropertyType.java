package dev.robotgryphon.screenlib.types;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.ScreenLib;

public record PropertyType<T>(Codec<T> codec) {

    public static final Codec<PropertyType<?>> CODEC = Codec.lazyInitialized(() -> ScreenLib.PROPERTY_TYPES
            .getRegistry()
            .get()
            .byNameCodec());
}

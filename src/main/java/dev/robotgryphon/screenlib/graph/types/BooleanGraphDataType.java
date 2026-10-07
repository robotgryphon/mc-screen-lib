package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;

import java.util.Optional;

public class BooleanGraphDataType implements GraphDataType<Boolean> {
    @Override
    public Class<Boolean> dataType() {
        return Boolean.class;
    }

    @Override
    public Codec<Boolean> codec() {
        return Codec.BOOL;
    }

    @Override
    public Optional<Boolean> defaultValueRaw() {
        return Optional.of(false);
    }
}

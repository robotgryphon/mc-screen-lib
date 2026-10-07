package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;

public class StringGraphDataType implements GraphDataType<String> {
    @Override
    public Class<String> dataType() {
        return String.class;
    }

    @Override
    public Codec<String> codec() {
        return Codec.STRING;
    }
}

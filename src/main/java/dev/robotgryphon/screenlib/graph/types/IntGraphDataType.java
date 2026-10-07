package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;

public class IntGraphDataType implements GraphDataType<Integer> {
    @Override
    public Class<Integer> dataType() {
        return Integer.class;
    }

    @Override
    public Codec<Integer> codec() {
        return Codec.INT;
    }
}

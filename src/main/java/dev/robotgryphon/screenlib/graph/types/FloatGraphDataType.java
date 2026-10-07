package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;

public class FloatGraphDataType implements GraphDataType<Float> {
    @Override
    public Class<Float> dataType() {
        return Float.class;
    }

    @Override
    public Codec<Float> codec() {
        return Codec.FLOAT;
    }
}

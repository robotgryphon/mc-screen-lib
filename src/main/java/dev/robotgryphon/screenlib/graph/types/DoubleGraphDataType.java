package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;

public class DoubleGraphDataType implements GraphDataType<Double> {
    @Override
    public Class<Double> dataType() {
        return Double.class;
    }

    @Override
    public Codec<Double> codec() {
        return Codec.DOUBLE;
    }
}

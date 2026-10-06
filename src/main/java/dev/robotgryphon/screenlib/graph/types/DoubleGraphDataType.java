package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.graph.GraphTheme;

public class DoubleGraphDataType implements GraphDataType<Double> {
    @Override
    public Class<Double> dataType() {
        return Double.class;
    }

    @Override
    public int color() {
        return GraphTheme.COLOR_NUMBER;
    }

    @Override
    public Codec<Double> codec() {
        return Codec.DOUBLE;
    }
}

package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.graph.GraphTheme;

public class FloatGraphDataType implements GraphDataType<Float> {
    @Override
    public Class<Float> dataType() {
        return Float.class;
    }

    @Override
    public int color() {
        return GraphTheme.COLOR_NUMBER;
    }

    @Override
    public Codec<Float> codec() {
        return Codec.FLOAT;
    }
}

package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.graph.GraphTheme;

public class IntGraphDataType implements GraphDataType<Integer> {
    @Override
    public Class<Integer> dataType() {
        return Integer.class;
    }

    @Override
    public int color() {
        return GraphTheme.COLOR_NUMBER;
    }

    @Override
    public Codec<Integer> codec() {
        return Codec.INT;
    }
}

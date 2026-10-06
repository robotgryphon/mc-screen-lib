package dev.robotgryphon.screenlib.graph.types;

import com.mojang.serialization.Codec;
import dev.robotgryphon.screenlib.graph.GraphTheme;

public class StringGraphDataType implements GraphDataType<String> {

    @Override
    public int color() {
        return GraphTheme.COLOR_STRING;
    }

    @Override
    public Codec<String> codec() {
        return Codec.STRING;
    }
}


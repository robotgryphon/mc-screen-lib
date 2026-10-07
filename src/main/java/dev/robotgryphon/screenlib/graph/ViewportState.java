package dev.robotgryphon.screenlib.graph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ViewportState(double centerX, double centerY, float zoom) {
    public static final Codec<ViewportState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("center_x").forGetter(ViewportState::centerX),
            Codec.DOUBLE.fieldOf("center_y").forGetter(ViewportState::centerY),
            Codec.FLOAT.fieldOf("zoom").forGetter(ViewportState::zoom)
    ).apply(i, ViewportState::new));
}

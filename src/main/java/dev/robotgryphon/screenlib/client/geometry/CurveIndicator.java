package dev.robotgryphon.screenlib.client.geometry;

import org.joml.Vector2fc;

public record CurveIndicator(Vector2fc center, float radius, boolean hovered) {
    public CurveIndicator(Vector2fc center, float radius) {
        this(center, radius, false);
    }
}

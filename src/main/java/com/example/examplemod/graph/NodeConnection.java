package com.example.examplemod.graph;

import com.example.examplemod.math.BezierCurveCalculator;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Vector2f;
import org.joml.Vector2fc;

public record NodeConnection(Vector2fc start, NodeSide startSide, Vector2fc end, NodeSide endSide, int color) {

    public static NodeConnection rightToLeft(Vector2fc start, Vector2fc end, int color) {
        return new NodeConnection(start, NodeSide.RIGHT, end, NodeSide.LEFT, color);
    }

    public Vector2fc[] realControlPoints() {
        return BezierCurveCalculator.calculateRightToLeft(start, end);
    }

    public Vector2fc[] relativeControlPoints(ScreenRectangle within) {
        var real = BezierCurveCalculator.calculateRightToLeft(start, end);
        Vector2fc[] transformed = new Vector2fc[4];
        for (int i = 0; i < real.length; i++) {
            float x = (real[i].x() - within.left()) / (float) within.width();
            float y = (real[i].y() - within.top()) / (float) within.height();
            transformed[i] = new Vector2f(x, y);
        }

        return transformed;
    }

    enum NodeSide {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }
}

package com.example.examplemod.graph;

import com.example.examplemod.math.BezierCurveCalculator;
import net.minecraft.client.gui.navigation.ScreenPosition;
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

    public ScreenRectangle bounds() {
        int minX = (int) Math.min(start.x(), end.x());
        int minY = (int) Math.min(start.y(), end.y());
        int maxX = (int) Math.max(start.x(), end.x());
        int maxY = (int) Math.max(start.y(), end.y());
        // Enforce a minimum 1x1 size so the picture-in-picture renderer never
        // tries to allocate a 0-dimension GPU texture (and so relativeControlPoints
        // never divides by zero) when start and end are axis-aligned.
        int width = Math.max(1, maxX - minX);
        int height = Math.max(1, maxY - minY);
        return new ScreenRectangle(new ScreenPosition(minX, minY), width, height);
    }

    public enum NodeSide {
        LEFT,
        RIGHT
    }
}

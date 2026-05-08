package com.example.examplemod.graph;

import com.example.examplemod.math.BezierCurveCalculator;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Vector2f;
import org.joml.Vector2fc;

import static com.example.examplemod.math.BezierCurveCalculator.LINE_PADDING_PX;

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
        int minX = (int) Math.min(start.x(), end.x()) - LINE_PADDING_PX;
        int minY = (int) Math.min(start.y(), end.y()) - LINE_PADDING_PX;
        int maxX = (int) Math.max(start.x(), end.x()) + LINE_PADDING_PX;
        int maxY = (int) Math.max(start.y(), end.y()) + LINE_PADDING_PX;
        // The padding gives the fixed-pixel-width line headroom around the
        // curve's bbox, so axis-aligned curves no longer clip down to a sliver
        // (or zero-dimension texture). Padding must match BezierCurveCalculator.
        return new ScreenRectangle(new ScreenPosition(minX, minY), maxX - minX, maxY - minY);
    }

    public enum NodeSide {
        LEFT,
        RIGHT
    }
}

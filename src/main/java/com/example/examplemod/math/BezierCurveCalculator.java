package com.example.examplemod.math;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.NonNull;

public class BezierCurveCalculator {

    public static Vector2fc[] calculateRightToLeft(Vector2fc start, Vector2fc end) {
        Vector2fc point1 = new Vector2f((start.x() + end.x()) / 2f, start.y());
        Vector2fc point2 = new Vector2f((start.x() + end.x()) / 2f, end.y());

        return new Vector2fc[]{start, point1, point2, end};
    }

    public static @NonNull ScreenRectangle getBounds(
            Vector2fc[] points,
            Matrix3x2fc pose
    ) {
        int minX = (int) Math.floor(Math.min(Math.min(points[0].x(), points[1].x()), Math.min(points[2].x(), points[3].x())));
        int minY = (int) Math.floor(Math.min(Math.min(points[0].y(), points[1].y()), Math.min(points[2].y(), points[3].y())));
        int maxX = (int) Math.ceil(Math.max(Math.max(points[0].x(), points[1].x()), Math.max(points[2].x(), points[3].x())));
        int maxY = (int) Math.ceil(Math.max(Math.max(points[0].y(), points[1].y()), Math.max(points[2].y(), points[3].y())));

        return new ScreenRectangle(minX, minY, maxX - minX, maxY - minY).transformMaxBounds(pose);
    }
}

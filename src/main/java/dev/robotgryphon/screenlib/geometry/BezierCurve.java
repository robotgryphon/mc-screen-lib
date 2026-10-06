package dev.robotgryphon.screenlib.geometry;

import dev.robotgryphon.screenlib.math.BezierCurveCalculator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

public record BezierCurve(Vector2fc start, Vector2fc end, ScreenRectangle bounds, int color,
                          @Nullable CurveIndicator indicator) {
    public static BezierCurve from(GuiGraphicsExtractor graphics,
                                   Vector2fc start,
                                   Vector2fc end,
                                   int color) {
        return from(graphics, start, end, color, null);
    }

    public static BezierCurve from(GuiGraphicsExtractor graphics,
                                   Vector2fc start,
                                   Vector2fc end,
                                   int color,
                                   @Nullable CurveIndicator indicator) {
        Matrix3x2f matrix = new Matrix3x2f(graphics.pose());

        final var controlPoints = BezierCurveCalculator.calculateRightToLeft(start, end);
        var bounds = BezierCurveCalculator.getBounds(controlPoints, matrix);

        return new BezierCurve(start, end, bounds, color, indicator);
    }

    public Vector2fc[] toRelativeControlPoints() {
        var real = BezierCurveCalculator.calculateRightToLeft(start, end);
        ScreenRectangle canvasBounds = canvasBounds();
        Vector2fc[] transformed = new Vector2fc[4];
        for (int i = 0; i < real.length; i++) {
            transformed[i] = relativeToBounds(real[i], canvasBounds);
        }
        return transformed;
    }

    public @Nullable Vector2fc toRelativeIndicatorCenter() {
        if (this.indicator == null) {
            return null;
        }
        return relativeToBounds(this.indicator.center(), canvasBounds());
    }

    private ScreenRectangle canvasBounds() {
        var real = BezierCurveCalculator.calculateRightToLeft(start, end);
        return BezierCurveCalculator.getBounds(real, new Matrix3x2f());
    }

    private static Vector2fc relativeToBounds(Vector2fc point, ScreenRectangle bounds) {
        float x = (point.x() - bounds.left()) / (float) bounds.width();
        float y = (point.y() - bounds.top()) / (float) bounds.height();
        return new Vector2f(x, y);
    }
}

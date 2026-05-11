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

        // Bounds are computed by applying the caller's pose to the model-space
        // control points, so they're already in screen space. The PiP framework
        // calls BlitRenderState.addVertexWith2DPose(pose, x0, y0) when it puts
        // the texture on screen — meaning it applies state.pose() to these
        // bounds again. To avoid that double-transform (which broke curves the
        // moment a parent canvas applied any non-identity pan/zoom), hand the
        // state an identity pose. The bounds carry the transform; the blit
        // shouldn't add another.
        final var controlPoints = BezierCurveCalculator.calculateRightToLeft(start, end);
        var bounds = BezierCurveCalculator.getBounds(controlPoints, matrix);

        return new BezierCurve(start, end, bounds, color, indicator);
    }

    /**
     * Control points expressed in {@code [0, 1]} relative to the curve's
     * <em>canvas-space</em> bounding box.
     *
     * <p>This is the shader's expected input. Relative coords must be computed
     * against a bbox in the <em>same</em> coordinate system as the points
     * themselves (canvas space) — feeding it the texture rect (which lives at
     * origin {@code (0, 0)} in scaled-pixel space) would mix coordinate
     * systems and produce out-of-range "relative" coords.
     *
     * <p>The proportions are identical to the screen-space bbox under any
     * translate+scale pose, so this gives the same visual result as if we had
     * derived relatives from the screen bounds — but without needing the pose
     * at this point in the pipeline.
     */
    public Vector2fc[] toRelativeControlPoints() {
        var real = BezierCurveCalculator.calculateRightToLeft(start, end);
        ScreenRectangle canvasBounds = canvasBounds();
        Vector2fc[] transformed = new Vector2fc[4];
        for (int i = 0; i < real.length; i++) {
            transformed[i] = relativeToBounds(real[i], canvasBounds);
        }
        return transformed;
    }

    /**
     * The {@link #indicator}'s center expressed in the same {@code [0, 1]}
     * canvas-space bbox-relative coords as {@link #toRelativeControlPoints()}.
     * Returns {@code null} when no indicator is attached.
     *
     * <p>Keeping the relative space consistent across both feeds means the
     * shader can multiply by {@code size} once for either and the two land in
     * the same pixel coordinate system.
     */
    public @Nullable Vector2fc toRelativeIndicatorCenter() {
        if (this.indicator == null) {
            return null;
        }
        return relativeToBounds(this.indicator.center(), canvasBounds());
    }

    private ScreenRectangle canvasBounds() {
        // Identity pose — we want the *canvas-space* (pre-pose) bbox so
        // dividing canvas-space points by it yields proportions in [0, 1].
        var real = BezierCurveCalculator.calculateRightToLeft(start, end);
        return BezierCurveCalculator.getBounds(real, new Matrix3x2f());
    }

    private static Vector2fc relativeToBounds(Vector2fc point, ScreenRectangle bounds) {
        float x = (point.x() - bounds.left()) / (float) bounds.width();
        float y = (point.y() - bounds.top()) / (float) bounds.height();
        return new Vector2f(x, y);
    }
}

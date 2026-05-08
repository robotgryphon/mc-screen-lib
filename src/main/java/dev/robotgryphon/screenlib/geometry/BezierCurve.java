package dev.robotgryphon.screenlib.geometry;

import dev.robotgryphon.screenlib.math.BezierCurveCalculator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.joml.Vector2fc;

public record BezierCurve(Vector2fc start, Vector2fc end, ScreenRectangle bounds, int color) {

    public static BezierCurve from(GuiGraphicsExtractor graphics,
                                   Vector2fc start,
                                   Vector2fc end,
                                   int color) {
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

        return new BezierCurve(start, end, bounds, color);
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
        // Identity pose — we want the *canvas-space* (pre-pose) bbox so
        // dividing canvas-space points by it yields proportions in [0, 1].
        ScreenRectangle canvasBounds = BezierCurveCalculator.getBounds(real, new Matrix3x2f());
        Vector2fc[] transformed = new Vector2fc[4];
        for (int i = 0; i < real.length; i++) {
            float x = (real[i].x() - canvasBounds.left()) / (float) canvasBounds.width();
            float y = (real[i].y() - canvasBounds.top()) / (float) canvasBounds.height();
            transformed[i] = new Vector2f(x, y);
        }

        return transformed;
    }
}

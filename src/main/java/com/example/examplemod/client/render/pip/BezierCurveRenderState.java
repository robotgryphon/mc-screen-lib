package com.example.examplemod.client.render.pip;

import com.example.examplemod.math.BezierCurveCalculator;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

public record BezierCurveRenderState(Matrix3x2f pose,
                                     Vector2fc[] controlPoints,
                                     int color,
                                     ScreenRectangle bounds) implements PictureInPictureRenderState {

    public static BezierCurveRenderState from(Matrix3x2f pose,
                                              Vector2fc[] screenPoints, Vector2fc[] relativePoints, int color) {
        // Bounds are computed by applying the caller's pose to the model-space
        // control points, so they're already in screen space. The PiP framework
        // calls BlitRenderState.addVertexWith2DPose(pose, x0, y0) when it puts
        // the texture on screen — meaning it applies state.pose() to these
        // bounds again. To avoid that double-transform (which broke curves the
        // moment a parent canvas applied any non-identity pan/zoom), hand the
        // state an identity pose. The bounds carry the transform; the blit
        // shouldn't add another.
        var bounds = BezierCurveCalculator.getBounds(screenPoints, pose);
        return new BezierCurveRenderState(new Matrix3x2f(), relativePoints, color, bounds);
    }

    @Override
    public int x0() {
        return bounds.left();
    }

    @Override
    public int x1() {
        return bounds.right();
    }

    @Override
    public int y0() {
        return bounds.top();
    }

    @Override
    public int y1() {
        return bounds.bottom();
    }

    @Override
    public float scale() {
        return 1;
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return null;
    }
}

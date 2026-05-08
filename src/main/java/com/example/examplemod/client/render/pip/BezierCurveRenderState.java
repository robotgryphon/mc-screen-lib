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
        var bounds = BezierCurveCalculator.getBounds(screenPoints, pose);
        return new BezierCurveRenderState(pose, relativePoints, color, bounds);
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

package dev.robotgryphon.screenlib.client.ui.render.pip;

import dev.robotgryphon.screenlib.geometry.BezierCurve;
import dev.robotgryphon.screenlib.math.BezierCurveCalculator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

public record BezierCurveRenderState(BezierCurve curve, float zoom) implements PictureInPictureRenderState {

    public static BezierCurveRenderState from(GuiGraphicsExtractor graphics,
                                              BezierCurve curve) {
        Matrix3x2f matrix = new Matrix3x2f(graphics.pose());
        float zoom = matrix.m00;

        return new BezierCurveRenderState(curve, zoom);
    }

    @Override
    public int x0() {
        return curve.bounds().left();
    }

    @Override
    public int x1() {
        return curve.bounds().right();
    }

    @Override
    public int y0() {
        return curve.bounds().top();
    }

    @Override
    public int y1() {
        return curve.bounds().bottom();
    }

    @Override
    public float scale() {
        return 1;
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return null;
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        // The PiP renderer reads bounds().width()/height() to size the texture;
        // returning null here NPE'd silently. curve.bounds() is already in
        // screen space (BezierCurve.from applies the canvas pose when computing
        // it), which is exactly what the framework wants for x0/y0/x1/y1.
        return curve.bounds();
    }
}

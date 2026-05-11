package dev.robotgryphon.screenlib.client.ui.render.uniforms;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.DynamicUniformStorage;
import org.joml.Vector2fc;
import org.joml.Vector4fc;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 * Std140 layout sent to the bezier-curve fragment shader.
 * <ul>
 *   <li>{@code controlPoints[0..3]} — cubic bezier control points, normalized
 *       to the curve's bounding box (each axis in {@code [0, 1]}).</li>
 *   <li>{@code area} — bounding-box dimensions in scaled (window) pixels;
 *       used by the shader to convert normalized control points into pixel
 *       space for distance calculations.</li>
 *   <li>{@code halfWidth} / {@code feather} — line thickness and AA falloff,
 *       both in scaled (window) pixels. Computed Java-side as
 *       {@code BezierCurveCalculator.LINE_HALFWIDTH * zoom * guiScale} (and
 *       similarly for feather), so the line scales with canvas zoom rather
 *       than staying a fixed device-pixel width.</li>
 *   <li>{@code indicator} — optional close-button circle. {@code xy} is the
 *       center in the same {@code [0, 1]} bbox-relative space as the control
 *       points, {@code z} is the radius in scaled pixels, {@code w} is the
 *       AA feather in scaled pixels. Setting the radius (z) to {@code 0}
 *       disables the indicator; the shader skips it entirely.</li>
 * </ul>
 */
public record BezierCurveUniform(Vector2fc[] controlPoints, ScreenRectangle area,
                                 float halfWidth, float feather,
                                 Vector4fc indicator) implements RenderPipelineUniforms {
    public static final String NAME = "BezierCurve";

    public static final Supplier<DynamicUniformStorage<BezierCurveUniform>> STORAGE = RenderPipelineUniformsStorage.register(
            NAME + " UBO",
            2,
            new Std140SizeCalculator()
                    .putVec2()  // point1
                    .putVec2()  // point2
                    .putVec2()  // point3
                    .putVec2()  // point4
                    .putVec2()  // size
                    .putVec2()  // lineParams (halfWidth, feather)
                    .putVec4()  // indicator (relCenterX, relCenterY, radiusPx, featherPx)
    );


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void write(ByteBuffer buffer) {
        Std140Builder.intoBuffer(buffer)
                .putVec2(controlPoints[0])
                .putVec2(controlPoints[1])
                .putVec2(controlPoints[2])
                .putVec2(controlPoints[3])
                .putVec2(area.width(), area.height())
                .putVec2(halfWidth, feather)
                .putVec4(indicator)
                .get();
    }
}

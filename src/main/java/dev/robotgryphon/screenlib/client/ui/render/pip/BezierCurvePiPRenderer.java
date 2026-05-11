package dev.robotgryphon.screenlib.client.ui.render.pip;

import dev.robotgryphon.screenlib.client.ui.render.ExRenderPipelines;
import dev.robotgryphon.screenlib.client.ui.render.PipelineRenderer;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.BezierCurveUniform;
import dev.robotgryphon.screenlib.math.BezierCurveCalculator;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.OptionalInt;

public class BezierCurvePiPRenderer
        extends net.minecraft.client.gui.render.pip.PictureInPictureRenderer<BezierCurveRenderState> {

    private BezierCurveRenderState lastState;

    public BezierCurvePiPRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public @NotNull Class<BezierCurveRenderState> getRenderStateClass() {
        return BezierCurveRenderState.class;
    }

    @Override
    protected boolean textureIsReadyToBlit(BezierCurveRenderState state) {
        return this.lastState != null && this.lastState.equals(state);
    }

    @Override
    protected void renderToTexture(BezierCurveRenderState state, @NonNull PoseStack stack) {
        var bounds = state.bounds();

        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        float scaledWidth = bounds.width() * scale;
        float scaledHeight = bounds.height() * scale;

        final var scaledBounds = new ScreenRectangle(0, 0, (int) scaledWidth, (int) scaledHeight);
        // Line width scales with zoom so the rendered line stays a consistent
        // thickness in canvas units rather than shrinking/growing on screen.
        // Multiplied by guiScale to land in scaled (window) pixels — the same
        // space the shader computes distances in.
        float zoomFactor = state.zoom();
        float halfWidthScaled = BezierCurveCalculator.LINE_HALFWIDTH * zoomFactor * scale;
        float featherScaled = BezierCurveCalculator.LINE_FEATHER * zoomFactor * scale;

        final var color = state.curve().color();
        // Pass no bbox — toRelativeControlPoints uses the curve's canvas-space
        // bbox internally, which is the only one in the same coordinate system
        // as the canvas-space control points. Passing scaledBounds here was
        // mixing canvas-space points with texture-pixel dimensions, producing
        // garbage relative coords that the shader couldn't draw.
        final var relativeControlPoints = state.curve().toRelativeControlPoints();

        // Indicator vec4: rel-center xy in [0, 1] bbox coords, radius and
        // feather in scaled pixels. A zero radius tells the shader "no
        // indicator" — used for every non-hovered connection.
        final Vector4fc indicatorParams;
        final var indicator = state.curve().indicator();
        if (indicator != null) {
            Vector2fc relCenter = state.curve().toRelativeIndicatorCenter();
            float radiusScaled = indicator.radius() * zoomFactor * scale;
            indicatorParams = new Vector4f(relCenter.x(), relCenter.y(), radiusScaled, featherScaled);
        } else {
            indicatorParams = new Vector4f(0f, 0f, 0f, 0f);
        }

        final var uniform = new BezierCurveUniform(relativeControlPoints, scaledBounds,
                halfWidthScaled, featherScaled, indicatorParams);

        GpuDevice device = RenderSystem.getDevice();
        var target = Minecraft.getInstance().getMainRenderTarget();

        try (var byteBuffer = new ByteBufferBuilder(256)) {
            var buffer = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            buffer.addVertex(0f, 0f, 0f).setColor(color);
            buffer.addVertex(0f, scaledHeight, 0f).setColor(color);
            buffer.addVertex(scaledWidth, scaledHeight, 0f).setColor(color);
            buffer.addVertex(scaledWidth, 0f, 0f).setColor(color);

            MeshData mesh = buffer.buildOrThrow();

            var buffers = PipelineRenderer.Buffers.of(mesh, ExRenderPipelines.BEZIER_CURVED_LINES);
            var encoder = device.createCommandEncoder();

            var uniforms = PipelineRenderer.getDynamicUniforms(-1);
            var bezierSlice = BezierCurveUniform.STORAGE.get().writeUniform(uniform);

            try (mesh; var pass = encoder.createRenderPass(
                    () -> "Pipeline Render Pass for: " + ExRenderPipelines.BEZIER_CURVED_LINES.getLocation(),
                    Objects.requireNonNullElse(RenderSystem.outputColorTextureOverride, target.getColorTextureView()),
                    OptionalInt.empty()
            )) {
                pass.setPipeline(ExRenderPipelines.BEZIER_CURVED_LINES);

                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", uniforms);
                pass.setUniform(BezierCurveUniform.NAME, bezierSlice);

                pass.setVertexBuffer(0, buffers.vertex());
                pass.setIndexBuffer(buffers.index(), buffers.type());

                pass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1);
            }
        }

        this.lastState = state;
    }

    @Override
    protected @NotNull String getTextureLabel() {
        return "bezier_curve";
    }
}
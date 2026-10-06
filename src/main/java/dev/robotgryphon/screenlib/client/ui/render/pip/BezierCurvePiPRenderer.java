package dev.robotgryphon.screenlib.client.ui.render.pip;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import dev.robotgryphon.screenlib.client.ui.render.ExRenderPipelines;
import dev.robotgryphon.screenlib.client.ui.render.PipelineRenderer;
import dev.robotgryphon.screenlib.client.ui.render.PipelineRenderer.UploadedMesh;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.BezierCurveUniform;
import dev.robotgryphon.screenlib.math.BezierCurveCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;


public class BezierCurvePiPRenderer
        extends OffscreenPiPRenderer<BezierCurveRenderState> {

    private BezierCurveRenderState lastState;
    private final BezierCurveUniform.Storage storage = new BezierCurveUniform.Storage();

    @Override
    public @NotNull Class<BezierCurveRenderState> getRenderStateClass() {
        return BezierCurveRenderState.class;
    }

    @Override
    protected boolean textureIsReadyToBlit(BezierCurveRenderState state) {
        return this.lastState != null && this.lastState.equals(state);
    }

    @Override
    protected DrawCall prepareDraw(BezierCurveRenderState state, int width, int height) {
        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();

        final var scaledBounds = new ScreenRectangle(0, 0, width, height);
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
        // indicator" — used for every non-hovered connection. We piggyback
        // the hovered flag on the *sign* of the scaled radius: negative
        // radius means "cursor is over this close circle"; the shader reads
        // abs(radius) for the SDF and the sign for the hover branch. This
        // keeps the uniform layout unchanged while still letting the shader
        // react to cursor proximity.
        final Vector4fc indicatorParams;
        final var indicator = state.curve().indicator();
        if (indicator != null) {
            Vector2fc relCenter = state.curve().toRelativeIndicatorCenter();
            float radiusScaled = indicator.radius() * zoomFactor * scale;
            if (indicator.hovered()) {
                radiusScaled = -radiusScaled;
            }
            indicatorParams = new Vector4f(relCenter.x(), relCenter.y(), radiusScaled, featherScaled);
        } else {
            indicatorParams = new Vector4f(0f, 0f, 0f, 0f);
        }

        final var uniform = new BezierCurveUniform(relativeControlPoints, scaledBounds,
                halfWidthScaled, featherScaled, indicatorParams);

        var bezierSlice = storage.write(uniform);
        var transforms = PipelineRenderer.getDynamicTransforms(-1);

        UploadedMesh mesh;
        try (var byteBuffer = new ByteBufferBuilder(256)) {
            var buffer = new BufferBuilder(byteBuffer, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);

            buffer.addVertex(0f, 0f, 0f).setColor(color);
            buffer.addVertex(0f, height, 0f).setColor(color);
            buffer.addVertex(width, height, 0f).setColor(color);
            buffer.addVertex(width, 0f, 0f).setColor(color);
            mesh = UploadedMesh.upload(buffer.buildOrThrow());
        }

        this.lastState = state;
        return new DrawCall() {
            @Override
            public void record(RenderPass pass) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(ExRenderPipelines.BEZIER_CURVED_LINES));
                pass.setUniform("DynamicTransforms", transforms);
                pass.setUniform(BezierCurveUniform.NAME, bezierSlice);
                mesh.draw(pass);
            }

            @Override
            public void close() {
                mesh.close();
            }
        };
    }

    @Override
    public void close() {
        storage.close();
        super.close();
    }

    @Override
    protected @NotNull String getTextureLabel() {
        return "bezier_curve";
    }
}
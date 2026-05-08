package com.example.examplemod.client.render.pip;

import com.example.examplemod.client.render.ExRenderPipelines;
import com.example.examplemod.client.render.PipelineRenderer;
import com.example.examplemod.client.render.uniforms.BezierCurveUniform;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;
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
        final var uniform = new BezierCurveUniform(state.controlPoints(), scaledBounds);

        GpuDevice device = RenderSystem.getDevice();
        var target = Minecraft.getInstance().getMainRenderTarget();

        try (var byteBuffer = new ByteBufferBuilder(256)) {
            var buffer = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            buffer.addVertex(0f, 0f, 0f).setColor(state.color());
            buffer.addVertex(0f, scaledHeight, 0f).setColor(state.color());
            buffer.addVertex(scaledWidth, scaledHeight, 0f).setColor(state.color());
            buffer.addVertex(scaledWidth, 0f, 0f).setColor(state.color());

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
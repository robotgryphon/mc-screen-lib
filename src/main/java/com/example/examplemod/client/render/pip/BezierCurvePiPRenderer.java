package com.example.examplemod.client.render.pip;

import com.example.examplemod.client.render.ExRenderPipelines;
import com.example.examplemod.client.render.PipelineRenderer;
import com.example.examplemod.client.render.uniforms.BezierCurveUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;

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
    protected void renderToTexture(BezierCurveRenderState state, PoseStack stack) {
        var bounds = state.bounds();

        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);

        buffer.addVertex(0f, 0f, 0f).setColor(state.color());
        buffer.addVertex(0f, bounds.height(), 0f).setColor(state.color());
        buffer.addVertex(bounds.width(), bounds.height(), 0f).setColor(state.color());
        buffer.addVertex(bounds.width(), 0f, 0f).setColor(state.color());

        PipelineRenderer.builder(ExRenderPipelines.BEZIER_CURVED_LINES, buffer.buildOrThrow())
                .uniform(BezierCurveUniform.STORAGE, new BezierCurveUniform(state.relativePoints(), bounds))
                .draw();

        this.lastState = state;
    }

    @Override
    protected @NotNull String getTextureLabel() {
        return "bezier_curve";
    }
}
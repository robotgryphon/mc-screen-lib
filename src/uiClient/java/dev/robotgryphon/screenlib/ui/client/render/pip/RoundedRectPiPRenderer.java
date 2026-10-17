package dev.robotgryphon.screenlib.ui.client.render.pip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import dev.robotgryphon.screenlib.ui.client.render.ExRenderPipelines;
import dev.robotgryphon.screenlib.ui.client.render.PipelineRenderer;
import dev.robotgryphon.screenlib.ui.client.render.PipelineRenderer.UploadedMesh;
import dev.robotgryphon.screenlib.ui.client.render.RenderHelper;
import dev.robotgryphon.screenlib.ui.client.render.uniforms.RoundedRectUniform;
import org.jetbrains.annotations.NotNull;
import dev.robotgryphon.screenlib.ui.api.render.RoundedRectRenderState;

public class RoundedRectPiPRenderer
        extends OffscreenPiPRenderer<RoundedRectRenderState> {
    private RoundedRectRenderState lastState;
    private final RoundedRectUniform.Storage STORAGE = new RoundedRectUniform.Storage();

    @Override
    public @NotNull Class<RoundedRectRenderState> getRenderStateClass() {
        return RoundedRectRenderState.class;
    }

    @Override
    protected boolean textureIsReadyToBlit(RoundedRectRenderState state) {
        return this.lastState != null && this.lastState.equals(state);
    }

    @Override
    protected DrawCall prepareDraw(RoundedRectRenderState state, int width, int height) {
        var uniform = new RoundedRectUniform(
                width, height,
                state.cornerRadiusScaled(),
                state.featherScaled(),
                state.borderThicknessScaled(),
                state.entries());

        var dynamicTransforms = PipelineRenderer.getDynamicTransforms(-1);
        var batchSlice = STORAGE.write(uniform);

        int color = 0xFFFFFFFF;
        UploadedMesh mesh = RenderHelper.createMesh(width, height, color);

        this.lastState = state;
        return new DrawCall() {
            @Override
            public void record(RenderPass pass) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(ExRenderPipelines.NODE_BACKGROUND));
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                pass.setUniform(RoundedRectUniform.Storage.NAME, batchSlice);
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
        STORAGE.close();
        super.close();
    }

    @Override
    protected @NotNull String getTextureLabel() {
        return "node_background";
    }
}

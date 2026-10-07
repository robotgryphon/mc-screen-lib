package dev.robotgryphon.screenlib.client.ui.render.pip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import dev.robotgryphon.screenlib.client.ui.render.ExRenderPipelines;
import dev.robotgryphon.screenlib.client.ui.render.PipelineRenderer;
import dev.robotgryphon.screenlib.client.ui.render.PipelineRenderer.UploadedMesh;
import dev.robotgryphon.screenlib.client.ui.render.RenderHelper;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import org.jetbrains.annotations.NotNull;

public class NodeBackgroundPiPRenderer
        extends OffscreenPiPRenderer<NodeBackgroundRenderState> {
    private NodeBackgroundRenderState lastState;
    private final NodeBackgroundUniform.Storage STORAGE = new NodeBackgroundUniform.Storage();

    @Override
    public @NotNull Class<NodeBackgroundRenderState> getRenderStateClass() {
        return NodeBackgroundRenderState.class;
    }

    @Override
    protected boolean textureIsReadyToBlit(NodeBackgroundRenderState state) {
        return this.lastState != null && this.lastState.equals(state);
    }

    @Override
    protected DrawCall prepareDraw(NodeBackgroundRenderState state, int width, int height) {
        var uniform = new NodeBackgroundUniform(
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
                pass.setUniform(NodeBackgroundUniform.Storage.NAME, batchSlice);
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

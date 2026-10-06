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
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import org.jetbrains.annotations.NotNull;


/**
 * PiP renderer for the batched node-background pass. One state represents
 * an entire batch of nodes; this class draws a single textured quad whose
 * fragments are colored by the {@code node_background} shader iterating
 * the per-node uniform arrays.
 *
 * <p>The structure mirrors {@link BezierCurvePiPRenderer} — same vertex
 * format, same dynamic-uniform plumbing, same single-quad mesh — with
 * the bezier-curve uniform swapped for {@link NodeBackgroundUniform}.
 * The PiP framework caches the rendered texture across frames when the
 * state hasn't changed (via {@link #textureIsReadyToBlit}), so a static
 * canvas costs effectively zero work after the first frame.
 */
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
        // Record equality compares every field, so the cached texture is
        // only reused when the entire batch (bounds, every entry's color
        // + position, every shared parameter) lines up. Any node move,
        // hover change, or zoom adjustment makes a fresh state on the
        // canvas side and forces a re-render here.
        return this.lastState != null && this.lastState.equals(state);
    }

    @Override
    protected DrawCall prepareDraw(NodeBackgroundRenderState state, int width, int height) {
        // The canvas widget has already pre-scaled the per-node bounds and
        // the shared parameters into scaled-pixel space, so this just feeds
        // them through.
        var uniform = new NodeBackgroundUniform(
                width, height,
                state.cornerRadiusScaled(),
                state.featherScaled(),
                state.borderThicknessScaled(),
                state.entries());

        var dynamicTransforms = PipelineRenderer.getDynamicTransforms(-1);
        var batchSlice = STORAGE.write(uniform);

        UploadedMesh mesh;
        try (var byteBuffer = new ByteBufferBuilder(256)) {
            var buffer = new BufferBuilder(byteBuffer, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);

            // Single quad covering the whole texture; the fragment
            // shader uses gl_FragCoord (texture-pixel space) to figure
            // out which node each fragment belongs to.
            int color = 0xFFFFFFFF;
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

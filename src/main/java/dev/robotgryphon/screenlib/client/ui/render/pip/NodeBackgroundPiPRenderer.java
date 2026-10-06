package dev.robotgryphon.screenlib.client.ui.render.pip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import dev.robotgryphon.screenlib.client.ui.render.ExRenderPipelines;
import dev.robotgryphon.screenlib.client.ui.render.PipelineRenderer;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Optional;

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
        extends net.minecraft.client.gui.render.pip.PictureInPictureRenderer<NodeBackgroundRenderState> {

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
    @ParametersAreNonnullByDefault
    protected void renderToTexture(NodeBackgroundRenderState state, PoseStack stack, SubmitNodeCollector submitNodeCollector) {
        var bounds = state.bounds();
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        float scaledWidth = bounds.width() * guiScale;
        float scaledHeight = bounds.height() * guiScale;

        // Build the UBO payload — the canvas widget has already
        // pre-scaled the per-node bounds and the shared parameters into
        // scaled-pixel space, so this just feeds them through.
        var uniform = new NodeBackgroundUniform(
                (int) scaledWidth, (int) scaledHeight,
                state.cornerRadiusScaled(),
                state.featherScaled(),
                state.borderThicknessScaled(),
                state.entries());

        GpuDevice device = RenderSystem.getDevice();
        var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();

        try (var byteBuffer = new ByteBufferBuilder(256)) {
            var buffer = new BufferBuilder(byteBuffer, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);

            // Single quad covering the whole texture; the fragment
            // shader uses gl_FragCoord (texture-pixel space) to figure
            // out which node each fragment belongs to.
            int color = 0xFFFFFFFF;
            buffer.addVertex(0f, 0f, 0f).setColor(color);
            buffer.addVertex(0f, scaledHeight, 0f).setColor(color);
            buffer.addVertex(scaledWidth, scaledHeight, 0f).setColor(color);
            buffer.addVertex(scaledWidth, 0f, 0f).setColor(color);

            MeshData mesh = buffer.buildOrThrow();

//            var buffers = PipelineRenderer.Buffers.of(mesh, ExRenderPipelines.NODE_BACKGROUND);
            var encoder = device.createCommandEncoder();

            var dynamicTransforms = PipelineRenderer.getDynamicTransforms(-1);
            var batchSlice = STORAGE.write(uniform);

            try (mesh; var pass = encoder.createRenderPass(
                    () -> "Pipeline Render Pass for: " + ExRenderPipelines.NODE_BACKGROUND.getLocation(),
                    Objects.requireNonNull(target.getColorTextureView()),
                    Optional.empty()
            )) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(ExRenderPipelines.NODE_BACKGROUND));

                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                pass.setUniform(NodeBackgroundUniform.Storage.NAME, batchSlice);

                // FIXME: 26.1 code
//                pass.setVertexBuffer(0, mesh.vertexBufferSlice());
//                pass.setIndexBuffer(buffers.index(), IndexType.INT);
//                pass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1);
            }
        }

        this.lastState = state;
    }

    @Override
    protected @NotNull String getTextureLabel() {
        return "node_background";
    }
}

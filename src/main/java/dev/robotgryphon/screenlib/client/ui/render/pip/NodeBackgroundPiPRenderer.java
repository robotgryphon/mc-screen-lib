package dev.robotgryphon.screenlib.client.ui.render.pip;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.robotgryphon.screenlib.client.ui.render.ExRenderPipelines;
import dev.robotgryphon.screenlib.client.ui.render.PipelineRenderer;
import dev.robotgryphon.screenlib.client.ui.render.uniforms.NodeBackgroundUniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.OptionalInt;

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

    public NodeBackgroundPiPRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

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
    protected void renderToTexture(NodeBackgroundRenderState state, @NonNull PoseStack stack) {
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
        var target = Minecraft.getInstance().getMainRenderTarget();

        try (var byteBuffer = new ByteBufferBuilder(256)) {
            var buffer = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            // Single quad covering the whole texture; the fragment
            // shader uses gl_FragCoord (texture-pixel space) to figure
            // out which node each fragment belongs to.
            int color = 0xFFFFFFFF;
            buffer.addVertex(0f, 0f, 0f).setColor(color);
            buffer.addVertex(0f, scaledHeight, 0f).setColor(color);
            buffer.addVertex(scaledWidth, scaledHeight, 0f).setColor(color);
            buffer.addVertex(scaledWidth, 0f, 0f).setColor(color);

            MeshData mesh = buffer.buildOrThrow();

            var buffers = PipelineRenderer.Buffers.of(mesh, ExRenderPipelines.NODE_BACKGROUND);
            var encoder = device.createCommandEncoder();

            var dynamicUniforms = PipelineRenderer.getDynamicUniforms(-1);
            var batchSlice = NodeBackgroundUniform.STORAGE.get().writeUniform(uniform);

            try (mesh; var pass = encoder.createRenderPass(
                    () -> "Pipeline Render Pass for: " + ExRenderPipelines.NODE_BACKGROUND.getLocation(),
                    Objects.requireNonNullElse(RenderSystem.outputColorTextureOverride, target.getColorTextureView()),
                    OptionalInt.empty()
            )) {
                pass.setPipeline(ExRenderPipelines.NODE_BACKGROUND);

                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicUniforms);
                pass.setUniform(NodeBackgroundUniform.NAME, batchSlice);

                pass.setVertexBuffer(0, buffers.vertex());
                pass.setIndexBuffer(buffers.index(), buffers.type());

                pass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1);
            }
        }

        this.lastState = state;
    }

    @Override
    protected @NotNull String getTextureLabel() {
        return "node_background";
    }
}

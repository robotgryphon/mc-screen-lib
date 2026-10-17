package dev.robotgryphon.screenlib.ui.client.render.pip;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public abstract class OffscreenPiPRenderer<T extends PictureInPictureRenderState>
        extends PictureInPictureRenderer<T> {
    private @Nullable GpuTexture texture;
    private @Nullable GpuTextureView textureView;
    private final Projection projection = new Projection();
    private final ProjectionMatrixBuffer projectionMatrixBuffer =
            new ProjectionMatrixBuffer("PIP - " + this.getClass().getSimpleName());

    @FunctionalInterface
    protected interface DrawCall extends AutoCloseable {
        void record(RenderPass pass);

        @Override
        default void close() {
        }
    }

    protected abstract DrawCall prepareDraw(T state, int width, int height);

    @Override
    public void prepare(T state, GuiRenderState guiRenderState,
                        FeatureRenderDispatcher featureRenderDispatcher, int guiScale) {
        int width = (state.x1() - state.x0()) * guiScale;
        int height = (state.y1() - state.y0()) * guiScale;
        boolean needsResize = texture == null
                || texture.getWidth(0) != width || texture.getHeight(0) != height;

        if (!needsResize && textureIsReadyToBlit(state)) {
            blitTexture(state, guiRenderState);
            return;
        }

        if (needsResize) {
            closeTextures();
            var device = RenderSystem.getDevice();
            texture = device.createTexture(() -> "UI " + getTextureLabel() + " texture",
                    13, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
            textureView = device.createTextureView(texture);
        }

        projection.setupOrtho(-1000.0F, 1000.0F, width, height, true);
        RenderSystem.setProjectionMatrix(projectionMatrixBuffer.getBuffer(projection), ProjectionType.ORTHOGRAPHIC);

        try (DrawCall call = prepareDraw(state, width, height)) {
            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "Pipeline Render Pass for: " + getTextureLabel(),
                    textureView, Optional.of(GuiRenderer.CLEAR_COLOR))) {
                RenderSystem.bindDefaultUniforms(pass);
                call.record(pass);
            }
        }

        blitTexture(state, guiRenderState);
    }

    @Override
    protected final void renderToTexture(T state, PoseStack poseStack, SubmitNodeCollector collector) {
    }

    @Override
    protected void blitTexture(T state, GuiRenderState guiRenderState) {
        guiRenderState.addBlitToCurrentLayer(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                state.pose(),
                state.x0(), state.y0(), state.x1(), state.y1(),
                0.0F, 1.0F, 1.0F, 0.0F,
                -1,
                state.scissorArea(),
                null));
    }

    @Override
    public boolean canBeReusedFor(T state, int textureWidth, int textureHeight) {
        return texture == null || (texture.getWidth(0) == textureWidth && texture.getHeight(0) == textureHeight);
    }

    private void closeTextures() {
        if (textureView != null) textureView.close();
        if (texture != null) texture.close();
        textureView = null;
        texture = null;
    }

    @Override
    public void close() {
        closeTextures();
        projectionMatrixBuffer.close();
    }
}

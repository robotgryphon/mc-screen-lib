package dev.robotgryphon.screenlib.nodes.client.render.block;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public class BlockPreviewPiPRenderer extends PictureInPictureRenderer<BlockPreviewRenderState> {
    private static final int FULL_BRIGHT = LightCoordsUtil.pack(15, 15);

    @Override
    public Class<BlockPreviewRenderState> getRenderStateClass() {
        return BlockPreviewRenderState.class;
    }

    @Override
    protected void renderToTexture(BlockPreviewRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.rotate(Axis.XP.rotationDegrees(30.0F));
        poseStack.rotate(Axis.YP.rotationDegrees(225.0F));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        PreviewBlockState block = new PreviewBlockState();
        block.blockPos = renderState.pos();
        block.randomSeedPos = renderState.pos();
        block.blockState = renderState.state();

        ClientLevel level = minecraft.level;
        boolean inWorld = level != null && level.dimension().equals(renderState.level()) && level.isLoaded(renderState.pos());
        if (inWorld) {
            block.biome = level.getBiome(renderState.pos());
            block.modelData = level.getModelData(renderState.pos());
            block.blockEntity = level.getBlockEntity(renderState.pos());
        }

        if (renderState.state().getRenderShape() == RenderShape.MODEL) {
            submitNodeCollector.submitMovingBlock(poseStack, block, 0);
        }
        if (block.blockEntity != null) {
            this.submitBlockEntity(minecraft, block.blockEntity, poseStack, submitNodeCollector);
        }
    }

    private void submitBlockEntity(Minecraft minecraft, BlockEntity blockEntity, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        var dispatcher = minecraft.getBlockEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(blockEntity);
        if (renderer == null) {
            return;
        }
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        @Nullable BlockEntityRenderState state = dispatcher.tryExtractRenderState(blockEntity, partialTick, null,
                renderer.shouldRenderOffScreen());
        if (state != null) {
            state.lightCoords = FULL_BRIGHT;
            dispatcher.submit(state, poseStack, submitNodeCollector, new CameraRenderState());
        }
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "block preview";
    }
}

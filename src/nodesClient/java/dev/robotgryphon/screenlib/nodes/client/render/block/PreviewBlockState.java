package dev.robotgryphon.screenlib.nodes.client.render.block;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public class PreviewBlockState extends MovingBlockRenderState {
    public @Nullable BlockEntity blockEntity;

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return 15;
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return pos.equals(this.blockPos) ? this.blockEntity : null;
    }
}

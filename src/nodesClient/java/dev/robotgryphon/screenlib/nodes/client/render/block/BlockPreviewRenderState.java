package dev.robotgryphon.screenlib.nodes.client.render.block;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public record BlockPreviewRenderState(ResourceKey<Level> level, BlockPos pos, BlockState state,
                                      ScreenRectangle bounds, float scale,
                                      @Nullable ScreenRectangle scissorArea) implements PictureInPictureRenderState {
    @Override
    public int x0() {
        return this.bounds.left();
    }

    @Override
    public int x1() {
        return this.bounds.right();
    }

    @Override
    public int y0() {
        return this.bounds.top();
    }

    @Override
    public int y1() {
        return this.bounds.bottom();
    }
}

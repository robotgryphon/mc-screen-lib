package dev.robotgryphon.screenlib.nodes.client.render.block;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public final class BlockPreviews {
    private static final float BLOCK_SCALE = 0.625F;

    private BlockPreviews() {}

    public static void submit(GuiGraphicsExtractor graphics, ScreenRectangle area,
                              ResourceKey<Level> level, BlockPos pos, BlockState state) {
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        Vector2f topLeft = pose.transformPosition(area.left(), area.top(), new Vector2f());
        Vector2f bottomRight = pose.transformPosition(area.right(), area.bottom(), new Vector2f());
        int x0 = (int) Math.floor(topLeft.x());
        int y0 = (int) Math.floor(topLeft.y());
        int x1 = (int) Math.ceil(bottomRight.x());
        int y1 = (int) Math.ceil(bottomRight.y());
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        float size = Math.min(bottomRight.x() - topLeft.x(), bottomRight.y() - topLeft.y());
        graphics.submitPictureInPictureRenderState(new BlockPreviewRenderState(level, pos, state,
                new ScreenRectangle(x0, y0, x1 - x0, y1 - y0), size * BLOCK_SCALE, graphics.peekScissorStack()));
    }
}

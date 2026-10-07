package dev.robotgryphon.screenlib.client.render;

import dev.robotgryphon.screenlib.graph.render.BlockPreviewState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public class BlockPreviewRenderer implements NodeStateRenderer<BlockPreviewState> {
    private static final int ITEM_SIZE = 16;

    @Override
    public void render(GuiGraphicsExtractor graphics, BlockPreviewState state,
                       int x, int y, int width, int height, float alpha) {
        ItemStack stack = new ItemStack(state.state().getBlock());
        if (stack.isEmpty()) {
            return;
        }
        float scale = Math.min(width, height) / (float) ITEM_SIZE;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + (width - ITEM_SIZE * scale) / 2f, y + (height - ITEM_SIZE * scale) / 2f);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }
}

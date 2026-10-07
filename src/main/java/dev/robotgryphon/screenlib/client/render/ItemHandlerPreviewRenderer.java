package dev.robotgryphon.screenlib.client.render;

import dev.robotgryphon.screenlib.graph.render.ItemHandlerPreviewProvider;
import dev.robotgryphon.screenlib.graph.render.ItemHandlerPreviewState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

public class ItemHandlerPreviewRenderer implements NodeStateRenderer<ItemHandlerPreviewState> {
    private static final int SLOT_BACKGROUND = 0xFF2A2C33;
    private static final int SLOT_BORDER = 0xFF4A4D58;
    private static final int INFO_COLOR = 0xFFA0A4B3;

    @Override
    public void render(GuiGraphicsExtractor graphics, ItemHandlerPreviewState state,
                       int x, int y, int width, int height, float alpha) {
        Font font = Minecraft.getInstance().font;
        int slotSize = ItemHandlerPreviewProvider.SLOT_SIZE;
        int visibleSlots = ItemHandlerPreviewProvider.COLUMNS * ItemHandlerPreviewProvider.ROWS;

        for (int slot = 0; slot < visibleSlots; slot++) {
            int sx = x + (slot % ItemHandlerPreviewProvider.COLUMNS) * slotSize;
            int sy = y + (slot / ItemHandlerPreviewProvider.COLUMNS) * slotSize;
            boolean exists = slot < state.slotCount();
            graphics.fill(sx, sy, sx + slotSize, sy + slotSize, ARGB.multiply(SLOT_BORDER, ARGB.white(exists ? alpha : alpha * 0.4f)));
            graphics.fill(sx + 1, sy + 1, sx + slotSize - 1, sy + slotSize - 1,
                    ARGB.multiply(SLOT_BACKGROUND, ARGB.white(exists ? alpha : alpha * 0.4f)));
            if (slot < state.stacks().size()) {
                ItemStack stack = state.stacks().get(slot);
                if (!stack.isEmpty()) {
                    graphics.item(stack, sx + 1, sy + 1);
                    graphics.itemDecorations(font, stack, sx + 1, sy + 1);
                }
            }
        }

        int hidden = Math.max(0, state.slotCount() - visibleSlots);
        String info = state.slotCount() + " slots, " + state.totalItems() + " items"
                + (hidden > 0 ? " (+" + hidden + " not shown)" : "");
        graphics.text(font, Component.literal(info), x,
                y + ItemHandlerPreviewProvider.ROWS * slotSize + 2,
                ARGB.multiply(INFO_COLOR, ARGB.white(alpha)), false);
    }
}

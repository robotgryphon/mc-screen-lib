package dev.robotgryphon.screenlib.client.render;

import dev.robotgryphon.screenlib.transfer.ItemHandlerContents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.ARGB;

public class ItemHandlerPreviewRenderer implements NodeStateRenderer<ItemHandlerContents> {
    private static final int SLOT_BACKGROUND = 0xFF2A2C33;
    private static final int SLOT_BORDER = 0xFF4A4D58;
    private static final int SLOT_HOVER = 0x40FFFFFF;
    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final int SLOT_SIZE = 18;
    private static final int INFO_HEIGHT = 12;

    @Override
    public int width() {
        return COLUMNS * SLOT_SIZE;
    }

    @Override
    public int height() {
        return ROWS * SLOT_SIZE + INFO_HEIGHT;
    }

    private static int slotAt(ScreenRectangle area, int mouseX, int mouseY) {
        int slotSize = SLOT_SIZE;
        int column = (int) Math.floor((mouseX - area.left()) / slotSize);
        int row = (int) Math.floor((mouseY - area.top()) / slotSize);
        if (!area.containsPoint(mouseX, mouseY) || column < 0 || column >= COLUMNS
                || row < 0 || row >= ROWS) {
            return -1;
        }
        return row * COLUMNS + column;
    }

    @Override
    public void tooltip(GuiGraphicsExtractor graphics, ItemHandlerContents state, ScreenRectangle area,
                        int mouseX, int mouseY, int screenX, int screenY) {
        int slot = slotAt(area, mouseX, mouseY);
        if (slot >= 0 && slot < state.size() && !state.get(slot).isEmpty()) {
            graphics.setTooltipForNextFrame(Minecraft.getInstance().font, state.get(slot), screenX, screenY);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, ItemHandlerContents state,
                       ScreenRectangle area, int mouseX, int mouseY, float alpha) {
        int x = area.left();
        int y = area.top();
        Font font = Minecraft.getInstance().font;
        int hovered = slotAt(area, mouseX, mouseY);
        int slotSize = SLOT_SIZE;
        int visibleSlots = COLUMNS * ROWS;

        for (int slot = 0; slot < visibleSlots; slot++) {
            int sx = x + (slot % COLUMNS) * slotSize;
            int sy = y + (slot / COLUMNS) * slotSize;
            boolean exists = slot < state.size();
            graphics.fill(sx, sy, sx + slotSize, sy + slotSize, ARGB.multiply(SLOT_BORDER, ARGB.white(exists ? alpha : alpha * 0.4f)));
            graphics.fill(sx + 1, sy + 1, sx + slotSize - 1, sy + slotSize - 1,
                    ARGB.multiply(SLOT_BACKGROUND, ARGB.white(exists ? alpha : alpha * 0.4f)));
            if (slot == hovered) {
                graphics.fill(sx + 1, sy + 1, sx + slotSize - 1, sy + slotSize - 1, SLOT_HOVER);
            }
            if (slot < state.size()) {
                var stack = state.get(slot);
                if (!stack.isEmpty()) {
                    graphics.item(stack, sx + 1, sy + 1);
                    graphics.itemDecorations(font, stack, sx + 1, sy + 1);
                }
            }
        }
    }
}

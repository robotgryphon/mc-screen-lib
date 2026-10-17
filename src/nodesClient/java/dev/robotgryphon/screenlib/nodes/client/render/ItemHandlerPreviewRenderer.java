package dev.robotgryphon.screenlib.nodes.client.render;

import dev.robotgryphon.screenlib.nodes.api.render.NodeStateRenderer;
import dev.robotgryphon.screenlib.nodes.common.transfer.ItemHandlerContents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

public class ItemHandlerPreviewRenderer implements NodeStateRenderer<ItemHandlerContents> {
    private static final int SLOT_BACKGROUND = 0xFF2A2C33;
    private static final int SLOT_BORDER = 0xFF4A4D58;
    private static final int SLOT_HOVER = 0x40FFFFFF;
    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final int SLOT_SIZE = 18;
    private static final int INFO_HEIGHT = 12;
    private static final int COUNT_MAX_WIDTH = 17;
    private static final float SMALL_COUNT_SCALE = 0.6f;
    private static final int ITEM_WIDTH = 16;
    private static final String INFINITY = "\u221E";
    private static final String[] SUFFIXES = {"k", "m", "b", "t", "q"};

    @Override
    public Class<ItemHandlerContents> stateType() {
        return ItemHandlerContents.class;
    }

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
            graphics.setTooltipForNextFrame(Minecraft.getInstance().font, state.get(slot).resource().toStack(), screenX, screenY);
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
                var entry = state.get(slot);
                if (!entry.isEmpty()) {
                    ItemStack stack = entry.resource().toStack();
                    graphics.item(stack, sx + 1, sy + 1);
                    graphics.itemDecorations(font, stack, sx + 1, sy + 1, "");
                    if (entry.amount() != 1) {
                        renderCount(graphics, font, entry, sx + 1, sy + 1);
                    }
                }
            }
        }
    }

    private static void renderCount(GuiGraphicsExtractor graphics, Font font, ItemHandlerContents.Entry entry,
                                    int x, int y) {
        String text = entry.isInfinite() ? INFINITY : compactCount(entry.amount());
        int width = font.width(text);
        boolean abbreviated = !entry.isInfinite() && entry.amount() >= 1_000;
        float scale = abbreviated || text.length() > 2 ? Math.min(SMALL_COUNT_SCALE, ITEM_WIDTH / (float) width) : 1f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + COUNT_MAX_WIDTH, y + COUNT_MAX_WIDTH);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, -width, -font.lineHeight + 1, -1, true);
        graphics.pose().popMatrix();
    }

    private static String compactCount(long amount) {
        if (amount < 1_000) {
            return Long.toString(amount);
        }
        long unit = 1_000;
        int index = 0;
        while (index < SUFFIXES.length - 1 && amount / unit >= 1_000) {
            unit *= 1_000;
            index++;
        }
        long tenths = amount / (unit / 10);
        long whole = tenths / 10;
        long fraction = tenths % 10;
        return whole + (fraction == 0 ? "" : "." + fraction) + SUFFIXES[index];
    }
}

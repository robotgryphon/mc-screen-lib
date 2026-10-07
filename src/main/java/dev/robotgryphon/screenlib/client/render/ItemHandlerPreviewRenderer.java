package dev.robotgryphon.screenlib.client.render;

import dev.robotgryphon.screenlib.graph.render.ItemHandlerPreviewProvider;
import dev.robotgryphon.screenlib.graph.render.ItemHandlerPreviewState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class ItemHandlerPreviewRenderer implements NodeStateRenderer<ItemHandlerPreviewState> {
    private static final int SLOT_BACKGROUND = 0xFF2A2C33;
    private static final int SLOT_BORDER = 0xFF4A4D58;
    private static final int INFO_COLOR = 0xFFA0A4B3;
    private static final int SLOT_HOVER = 0x40FFFFFF;

    private static int slotAt(RenderArea area, double mouseX, double mouseY) {
        int slotSize = ItemHandlerPreviewProvider.SLOT_SIZE;
        int column = (int) Math.floor((mouseX - area.x()) / slotSize);
        int row = (int) Math.floor((mouseY - area.y()) / slotSize);
        if (!area.contains(mouseX, mouseY) || column < 0 || column >= ItemHandlerPreviewProvider.COLUMNS
                || row < 0 || row >= ItemHandlerPreviewProvider.ROWS) {
            return -1;
        }
        return row * ItemHandlerPreviewProvider.COLUMNS + column;
    }

    @Override
    public Optional<NodeTooltip> tooltip(ItemHandlerPreviewState state, RenderArea area, double mouseX, double mouseY) {
        int slot = slotAt(area, mouseX, mouseY);
        if (slot < 0 || slot >= state.slotCount()) {
            return Optional.empty();
        }
        if (slot < state.stacks().size() && !state.stacks().get(slot).isEmpty()) {
            return Optional.of(NodeTooltip.of(state.stacks().get(slot)));
        }

        return Optional.empty();
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, ItemHandlerPreviewState state,
                       RenderArea area, int mouseX, int mouseY, float alpha) {
        int x = area.x();
        int y = area.y();
        Font font = Minecraft.getInstance().font;
        int hovered = slotAt(area, mouseX, mouseY);
        int slotSize = ItemHandlerPreviewProvider.SLOT_SIZE;
        int visibleSlots = ItemHandlerPreviewProvider.COLUMNS * ItemHandlerPreviewProvider.ROWS;

        for (int slot = 0; slot < visibleSlots; slot++) {
            int sx = x + (slot % ItemHandlerPreviewProvider.COLUMNS) * slotSize;
            int sy = y + (slot / ItemHandlerPreviewProvider.COLUMNS) * slotSize;
            boolean exists = slot < state.slotCount();
            graphics.fill(sx, sy, sx + slotSize, sy + slotSize, ARGB.multiply(SLOT_BORDER, ARGB.white(exists ? alpha : alpha * 0.4f)));
            graphics.fill(sx + 1, sy + 1, sx + slotSize - 1, sy + slotSize - 1,
                    ARGB.multiply(SLOT_BACKGROUND, ARGB.white(exists ? alpha : alpha * 0.4f)));
            if (slot == hovered) {
                graphics.fill(sx + 1, sy + 1, sx + slotSize - 1, sy + slotSize - 1, SLOT_HOVER);
            }
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

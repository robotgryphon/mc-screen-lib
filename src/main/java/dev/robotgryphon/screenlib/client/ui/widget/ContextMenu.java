package dev.robotgryphon.screenlib.client.ui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Lightweight right-click popup. Not an {@code AbstractWidget} — it lives as
 * a transient child of {@link CanvasWidget}, which spawns one per right-click
 * and renders/forwards events to it manually. Keeping it off the screen's
 * widget list means position can be set per-click without juggling
 * {@code addRenderableWidget} / {@code removeWidget} calls.
 *
 * <p>The menu is presented in screen-space (above the canvas's pan/zoom
 * transform), so it stays anchored to the cursor regardless of canvas state.
 */
public class ContextMenu {

    private static final int ROW_HEIGHT = 14;
    private static final int H_PADDING = 6;
    private static final int V_PADDING = 2;

    private static final int BG_COLOR = 0xEE1F1F23;
    private static final int BORDER_COLOR = 0xFF7F7F8C;
    private static final int HOVER_COLOR = 0x33FFFFFF;
    private static final int TEXT_COLOR = 0xFFE0E0E0;

    public record Item(Component label, Runnable action) {}

    private final List<Item> items;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public ContextMenu(int x, int y, List<Item> items) {
        this.items = List.copyOf(items);

        Font font = Minecraft.getInstance().font;
        int maxLabel = 0;
        for (Item it : this.items) {
            maxLabel = Math.max(maxLabel, font.width(it.label()));
        }

        this.x = x;
        this.y = y;
        this.width = maxLabel + 2 * H_PADDING;
        this.height = this.items.size() * ROW_HEIGHT + 2 * V_PADDING;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    /**
     * Forwarded from the parent widget on click. Returns true if the click
     * landed on the menu (and an item action may have run); the caller is
     * expected to dismiss the menu unconditionally afterwards — even an
     * outside click should close it.
     */
    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        int relY = (int) (mouseY - this.y) - V_PADDING;
        int idx = relY / ROW_HEIGHT;
        if (idx >= 0 && idx < items.size()) {
            items.get(idx).action().run();
        }
        return true;
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, BG_COLOR);
        graphics.outline(this.x, this.y, this.width, this.height, BORDER_COLOR);

        Font font = Minecraft.getInstance().font;
        int textOffsetY = (ROW_HEIGHT - font.lineHeight) / 2 + 1;
        for (int i = 0; i < this.items.size(); i++) {
            int rowY = this.y + V_PADDING + i * ROW_HEIGHT;
            boolean hovered = mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hovered) {
                graphics.fill(this.x + 1, rowY, this.x + this.width - 1, rowY + ROW_HEIGHT, HOVER_COLOR);
            }
            graphics.text(font, this.items.get(i).label(),
                    this.x + H_PADDING, rowY + textOffsetY, TEXT_COLOR, false);
        }
    }
}

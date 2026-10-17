package dev.robotgryphon.screenlib.nodes.client.widget.property;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class DropdownPopup<T> {
    private static final int ROW_HEIGHT = 14;
    private static final int H_PADDING = 6;
    private static final int V_PADDING = 2;

    private static final int BG_COLOR = 0xEE1F1F23;
    private static final int BORDER_COLOR = 0xFF7F7F8C;
    private static final int HOVER_COLOR = 0x33FFFFFF;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int CURRENT_TEXT_COLOR = 0xFFFFD24A;

    private static final String CLEAR_LABEL = "— (clear)";

    private final List<Optional<T>> options;
    private final Optional<T> currentValue;
    private final Consumer<Optional<T>> onSelect;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public DropdownPopup(int x, int y, int width, List<Optional<T>> options,
                         Optional<T> currentValue, Consumer<Optional<T>> onSelect) {
        this.options = List.copyOf(options);
        this.currentValue = currentValue;
        this.onSelect = onSelect;

        Font font = Minecraft.getInstance().font;
        int widestOption = 0;
        for (Optional<T> opt : this.options) {
            widestOption = Math.max(widestOption, font.width(display(opt)));
        }
        int minWidth = widestOption + 2 * H_PADDING;

        this.x = x;
        this.y = y;
        this.width = Math.max(width, minWidth);
        this.height = this.options.size() * ROW_HEIGHT + 2 * V_PADDING;
    }

    private static String display(Optional<?> option) {
        return option.<String>map(PropertyValues::display).orElse(CLEAR_LABEL);
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        int relY = (int) (mouseY - this.y) - V_PADDING;
        int idx = relY / ROW_HEIGHT;
        if (idx >= 0 && idx < this.options.size()) {
            this.onSelect.accept(this.options.get(idx));
        }
        return true;
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, BG_COLOR);
        graphics.outline(this.x, this.y, this.width, this.height, BORDER_COLOR);

        Font font = Minecraft.getInstance().font;
        int textOffsetY = (ROW_HEIGHT - font.lineHeight) / 2 + 1;
        for (int i = 0; i < this.options.size(); i++) {
            Optional<T> opt = this.options.get(i);
            int rowY = this.y + V_PADDING + i * ROW_HEIGHT;
            boolean hovered = mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hovered) {
                graphics.fill(this.x + 1, rowY, this.x + this.width - 1, rowY + ROW_HEIGHT, HOVER_COLOR);
            }
            int color = opt.equals(this.currentValue) ? CURRENT_TEXT_COLOR : TEXT_COLOR;
            graphics.text(font, Component.literal(display(opt)),
                    this.x + H_PADDING, rowY + textOffsetY, color, false);
        }
    }
}

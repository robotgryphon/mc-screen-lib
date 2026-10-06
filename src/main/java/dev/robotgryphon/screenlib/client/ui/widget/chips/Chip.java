package dev.robotgryphon.screenlib.client.ui.widget.chips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class Chip extends AbstractWidget {
    public static final int CHIP_HEIGHT = 14;

    private static final int CORNER_RADIUS = 3;
    private static final int PADDING_X = 5;
    private static final int DOT_RADIUS = 2;
    private static final int DOT_GAP = 4;
    private static final int LABEL_GAP = 4;
    private static final int DISMISS_GAP = 4;
    private static final int DISMISS_W = 6;

    private static final int BG_NORMAL = 0xFF2F2F38;
    private static final int BG_HOVER = 0xFF3F3F49;
    private static final int BORDER_COLOR = 0xFF50505A;
    private static final int CATEGORY_COLOR = 0xFFB5B5C0;
    private static final int VALUE_COLOR = 0xFFFFFFFF;
    private static final int DISMISS_COLOR = 0xFFC0C0C8;
    private static final int DISMISS_HOVER_COLOR = 0xFFFFFFFF;

    private final ChipHolder holder;
    private final int dotColor;
    private final Component category;
    private final Component value;
    private final boolean dismissable;
    private final @org.jspecify.annotations.Nullable Object data;
    private final int catTextWidth;
    private final int valTextWidth;

    private Chip(ChipHolder holder, int dotColor, Component category, Component value,
                 boolean dismissable, @org.jspecify.annotations.Nullable Object data) {
        super(0, 0, 0, CHIP_HEIGHT, value);
        this.holder = holder;
        this.dotColor = dotColor;
        this.category = category;
        this.value = value;
        this.dismissable = dismissable;
        this.data = data;

        Font font = Minecraft.getInstance().font;
        this.catTextWidth = font.width(uppercased(this.category));
        this.valTextWidth = font.width(this.value);
        this.setWidth(computeWidth());
    }

    public @org.jspecify.annotations.Nullable Object data() {
        return this.data;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int dotColor() {
        return this.dotColor;
    }

    public Component category() {
        return this.category;
    }

    public Component value() {
        return this.value;
    }

    private int computeWidth() {
        int w = PADDING_X
                + (2 * DOT_RADIUS + 1)
                + DOT_GAP
                + this.catTextWidth
                + LABEL_GAP
                + this.valTextWidth;
        if (this.dismissable) {
            w += DISMISS_GAP + DISMISS_W;
        }
        return w + PADDING_X;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        super.onClick(event, doubleClick);
        if (this.dismissable && hitDismiss(event.x(), event.y())) {
            this.holder.removeChip(this);
        }
    }

    private boolean hitDismiss(double mouseX, double mouseY) {
        if (!this.dismissable) {
            return false;
        }
        int dismissLeft = this.getX() + this.getWidth() - PADDING_X - DISMISS_W;
        return mouseX >= dismissLeft && mouseX < dismissLeft + DISMISS_W
                && mouseY >= this.getY() && mouseY < this.getY() + this.getHeight();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;

        fillRoundedRect(graphics, x, y, w, h, CORNER_RADIUS, BORDER_COLOR);
        fillRoundedRect(graphics, x + 1, y + 1, w - 2, h - 2,
                Math.max(0, CORNER_RADIUS - 1), hovered ? BG_HOVER : BG_NORMAL);

        int cursor = x + PADDING_X;

        int dotCx = cursor + DOT_RADIUS;
        int dotCy = y + h / 2;
        fillDot(graphics, dotCx, dotCy, DOT_RADIUS, this.dotColor);
        cursor += (2 * DOT_RADIUS + 1) + DOT_GAP;

        Font font = Minecraft.getInstance().font;
        int textY = y + (h - font.lineHeight) / 2 + 1;

        graphics.text(font, uppercased(this.category), cursor, textY, CATEGORY_COLOR, false);
        cursor += this.catTextWidth + LABEL_GAP;

        graphics.text(font, this.value, cursor, textY, VALUE_COLOR, false);
        cursor += this.valTextWidth;

        if (this.dismissable) {
            cursor += DISMISS_GAP;
            boolean dismissHover = hitDismiss(mouseX, mouseY);
            Component glyph = Component.literal("×");
            int glyphW = font.width(glyph);
            graphics.text(font, glyph,
                    cursor + (DISMISS_W - glyphW) / 2, textY,
                    dismissHover ? DISMISS_HOVER_COLOR : DISMISS_COLOR, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    private static Component uppercased(Component c) {
        return Component.literal(c.getString().toUpperCase(Locale.ROOT));
    }

    private static void fillRoundedRect(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        for (int dy = 0; dy < h; dy++) {
            int distFromEdge = Math.min(dy, h - 1 - dy);
            int indent = Math.max(0, radius - 1 - distFromEdge);
            graphics.fill(x + indent, y + dy, x + w - indent, y + dy + 1, color);
        }
    }

    private static void fillDot(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int trim = (Math.abs(dy) == r) ? 1 : 0;
            graphics.fill(cx - r + trim, cy + dy, cx + r + 1 - trim, cy + dy + 1, color);
        }
    }

    public static final class Builder {
        private int dotColor = 0xFFFFFFFF;
        private Component category;
        private Component value;
        private boolean dismissable;
        private @org.jspecify.annotations.Nullable Object data;

        private Builder() {
        }

        public Builder dotColor(int color) {
            this.dotColor = color;
            return this;
        }

        public Builder category(Component category) {
            this.category = category;
            return this;
        }

        public Builder value(Component value) {
            this.value = value;
            return this;
        }

        public Builder dismissable() {
            this.dismissable = true;
            return this;
        }

        public Builder data(@org.jspecify.annotations.Nullable Object data) {
            this.data = data;
            return this;
        }

        Chip build(ChipHolder holder) {
            if (this.category == null) {
                throw new IllegalStateException("Chip.Builder: category is required");
            }
            if (this.value == null) {
                throw new IllegalStateException("Chip.Builder: value is required");
            }
            return new Chip(holder, this.dotColor, this.category, this.value, this.dismissable, this.data);
        }
    }
}

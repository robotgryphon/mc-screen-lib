package dev.robotgryphon.screenlib.client.ui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Floating option list spawned by {@link DropdownEditor}. Renders the
 * available values for a string-typed property and reports which one
 * the user picked. Modeled on {@link ContextMenu} — both are transient
 * floating panels owned by the canvas, rendered in screen-space above
 * the canvas pose, dismissed by an outside click.
 *
 * <p>Position is supplied in screen pixels: the canvas widget converts
 * the editor's canvas-space anchor (typically the row's bottom-left)
 * through {@code Canvas.canvasToScreen} so the popup pins to the
 * trigger regardless of pan/zoom. Width is fixed at construction so the
 * dropdown lines up with the editor that spawned it.
 *
 * <p>The selection is delivered through a {@link Consumer} so the
 * surrounding logic — figuring out which {@code Node} and which
 * property to write to — stays out of this widget. {@code DropdownPopup}
 * only cares about "user picked one of these strings".
 */
public class DropdownPopup {

    private static final int ROW_HEIGHT = 14;
    private static final int H_PADDING = 6;
    private static final int V_PADDING = 2;

    /** Panel background — slightly more opaque than the context menu so it pops over busy graphs. */
    private static final int BG_COLOR = 0xEE1F1F23;
    private static final int BORDER_COLOR = 0xFF7F7F8C;
    private static final int HOVER_COLOR = 0x33FFFFFF;
    /** Color of regular option text. */
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    /** Color of the option matching the property's current value — softly highlighted. */
    private static final int CURRENT_TEXT_COLOR = 0xFFFFD24A;

    private final List<String> options;
    private final String currentValue;
    private final Consumer<String> onSelect;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public DropdownPopup(int x, int y, int width, List<String> options,
                         String currentValue, Consumer<String> onSelect) {
        this.options = List.copyOf(options);
        this.currentValue = currentValue;
        this.onSelect = onSelect;

        // Width is caller-controlled so the popup lines up with the editor
        // that spawned it — but enforce a floor of "fits the widest option"
        // so the popup never clips its own text. Font lookup is cheap and
        // happens once per popup construction.
        Font font = Minecraft.getInstance().font;
        int widestOption = 0;
        for (String opt : this.options) {
            widestOption = Math.max(widestOption, font.width(opt));
        }
        int minWidth = widestOption + 2 * H_PADDING;

        this.x = x;
        this.y = y;
        this.width = Math.max(width, minWidth);
        this.height = this.options.size() * ROW_HEIGHT + 2 * V_PADDING;
    }

    /** Outer bounds test — used by the canvas widget to decide "click inside vs outside". */
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    /**
     * Forwarded from the parent widget on click. Returns {@code true} if
     * the click landed on the popup — the caller dismisses the popup
     * unconditionally afterwards, even on an inside click that resolved
     * to an option. An outside click returns {@code false}, which is the
     * signal the caller uses to keep the click propagating to the canvas.
     */
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
            String opt = this.options.get(i);
            int rowY = this.y + V_PADDING + i * ROW_HEIGHT;
            boolean hovered = mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hovered) {
                graphics.fill(this.x + 1, rowY, this.x + this.width - 1, rowY + ROW_HEIGHT, HOVER_COLOR);
            }
            int color = opt.equals(this.currentValue) ? CURRENT_TEXT_COLOR : TEXT_COLOR;
            graphics.text(font, Component.literal(opt),
                    this.x + H_PADDING, rowY + textOffsetY, color, false);
        }
    }
}

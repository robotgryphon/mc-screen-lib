package dev.robotgryphon.screenlib.client.ui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * Inline editor for a property whose value is a pick from a fixed list.
 * Visually a rounded pill containing the current value text and a small
 * downward chevron on the right edge, indicating "click to open the
 * option list".
 *
 * <p>Inherits the pill background, hit test, and text-centering math
 * from {@link PropertyEditor}; the chevron and the hover-elevated
 * background tone live here. The popup itself is {@link DropdownPopup},
 * spawned by the surrounding {@code NodeWidget} on click.
 *
 * <p>Sibling to {@link NumericPropertyEditor} — both are stateless
 * utility classes extending the same base, drawing inside a property
 * row's value area, and relying on the surrounding {@code NodeWidget}
 * for hit-testing and click dispatch.
 */
public final class DropdownEditor extends PropertyEditor {

    /** Slightly lifted background when the row is hovered, like a real button. */
    private static final int BG_HOVER_COLOR = 0xCC242530;
    /** Color of the chevron glyph — softer than the value so the eye lands on the value first. */
    private static final int CHEVRON_COLOR = 0xFFB0B3BC;
    /** Chevron color when the dropdown is hovered, signaling it's a live target. */
    private static final int CHEVRON_HOVER_COLOR = 0xFFFFFFFF;

    /** Pixels of horizontal padding inside the pill on each side. */
    private static final int H_PADDING = 6;
    /** Half-width of the downward chevron triangle. Total triangle width = 2 * HALF + 1. */
    private static final int CHEVRON_HALF_WIDTH = 2;

    private DropdownEditor() {}

    /**
     * Draws the row-inline trigger:
     * <pre>
     * +-----------------------+
     * |  euler              v |
     * +-----------------------+
     * </pre>
     * The value text is left-aligned inside the padded area; the chevron
     * sits centered against the right padding. The whole pill reacts to
     * hover so the user sees the entire trigger surface light up at once.
     */
    public static void render(GuiGraphicsExtractor graphics, Font font,
                              int editorX, int editorY, int editorWidth, int editorHeight,
                              int mouseX, int mouseY,
                              String value, float alpha) {
        boolean hovered = isInside(mouseX, mouseY, editorX, editorY, editorWidth, editorHeight);
        int bg = hovered ? BG_HOVER_COLOR : BG_COLOR;

        // Rounded pill background — the radius and base color come from
        // the {@link PropertyEditor} base, hover state overrides locally.
        renderPill(graphics, editorX, editorY, editorWidth, editorHeight, bg, alpha);

        // Value text — left-aligned inside the padded area, vertically
        // centered like the rest of the row's text.
        int textY = textCenterY(editorY, editorHeight, font);
        int valueColor = ARGB.multiply(VALUE_TEXT_COLOR, ARGB.white(alpha));
        graphics.text(font, Component.literal(value),
                editorX + H_PADDING, textY, valueColor, false);

        // Chevron — drawn as filled pixel rows rather than a font glyph
        // so it renders consistently regardless of font availability.
        // The chevron's geometric center sits inside the right padding
        // strip, vertically centered against the pill's middle.
        int chevronColor = ARGB.multiply(
                hovered ? CHEVRON_HOVER_COLOR : CHEVRON_COLOR,
                ARGB.white(alpha));
        int chevronCenterX = editorX + editorWidth - H_PADDING - CHEVRON_HALF_WIDTH;
        int chevronTopY = editorY + (editorHeight - (CHEVRON_HALF_WIDTH + 1)) / 2;
        fillTriangleDown(graphics, chevronCenterX, chevronTopY, CHEVRON_HALF_WIDTH, chevronColor);
    }

    /**
     * Rasterizes a downward-pointing triangle with its top row centered
     * at ({@code cx}, {@code topY}). Each row from the top is one pixel
     * narrower on each side until it tapers to a single-pixel point.
     */
    private static void fillTriangleDown(GuiGraphicsExtractor graphics, int cx, int topY,
                                         int halfWidth, int color) {
        int height = halfWidth + 1;
        for (int dy = 0; dy < height; dy++) {
            int rowHalf = halfWidth - dy;
            graphics.fill(cx - rowHalf, topY + dy, cx + rowHalf + 1, topY + dy + 1, color);
        }
    }
}

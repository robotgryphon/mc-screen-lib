package dev.robotgryphon.screenlib.client.ui.widget;

import net.minecraft.client.Minecraft;
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
 * <p>Inherits its bounds, the pill background, the hit test, and the
 * text-centering math from {@link PropertyEditor}; the chevron and the
 * hover-elevated background tone live here. The popup itself is
 * {@link DropdownPopup}, spawned by the surrounding {@code NodeWidget}
 * (or any host of the editor) on click — the editor itself only renders
 * the trigger surface, it doesn't own the option list.
 *
 * <p>Sibling to {@link NumericPropertyEditor} — both extend the same
 * {@code LayoutElement} / {@code Renderable} base so a layout container
 * can position either kind without caring which it is. The static
 * {@link #render} entry point is kept for callers that already have
 * explicit row geometry on hand and don't want to allocate an instance
 * just to draw one row.
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

    /**
     * Current value shown in the trigger. Held as a plain {@link String}
     * because the dropdown is currently gated to string-codec properties
     * ({@link dev.robotgryphon.screenlib.types.PropertyDefinition}s with
     * an {@code allowedValues} list). If that ever expands to other
     * picker types, this becomes the natural point to broaden.
     */
    private String value;

    /** Opacity multiplier — see {@link NumericPropertyEditor#setAlpha}. */
    private float alpha = 1f;

    public DropdownEditor(int x, int y, int width, int height, String value) {
        super(x, y, width, height);
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    /**
     * Update the value shown in the trigger. Hosts call this after the
     * popup's {@code onSelect} writes a new pick through to the model;
     * the next render then displays the new selection without rebuilding
     * the editor.
     */
    public void setValue(String value) {
        this.value = value;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics,
                                   int mouseX, int mouseY, float partialTick) {
        // Instance render — defer to the existing static so the layout
        // logic (chevron geometry, text baseline) stays in one place.
        render(graphics, Minecraft.getInstance().font,
                this.x, this.y, this.width, this.height,
                mouseX, mouseY, this.value, this.alpha);
    }

    // -- Static API --------------------------------------------------------

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

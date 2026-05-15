package dev.robotgryphon.screenlib.client.ui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * Stateless inline editor for a numeric property value. Renders a small
 * rounded pill with a minus glyph on the left, the current value in the
 * middle, and a plus glyph on the right — the same shape the KSampler-
 * style reference UI uses for its numeric fields. Designed to live
 * inside the value area of a {@code NodeWidget}'s property row.
 *
 * <p>Inherits the pill background, hit test, and text-centering math
 * from {@link PropertyEditor}; the per-type click step methods and the
 * minus/plus glyph layout live here.
 *
 * <p>Rendering is shared across {@code int}, {@code float}, and
 * {@code double} via a {@link Number}-accepting entry point — the only
 * thing the three numeric types disagree on is the step size and the
 * post-step rounding to keep float drift from making "1.1 + 0.1" read as
 * {@code 1.2000001}. Each type therefore gets its own {@code apply*Click}
 * method.
 */
public final class NumericPropertyEditor extends PropertyEditor {

    /** Width of the minus / plus hit area on each side of the value column. */
    public static final int BUTTON_WIDTH = 12;

    /** Step size for float / double adjustments. Int always steps by 1. */
    private static final float FLOAT_STEP = 0.1f;
    private static final double DOUBLE_STEP = 0.1;

    /** Soft overlay drawn behind a hovered button to signal "this is the live click target". */
    private static final int BUTTON_HOVER_OVERLAY = 0x33FFFFFF;
    /** Glyph color for the minus and plus marks. */
    private static final int BUTTON_GLYPH_COLOR = 0xFFB0B3BC;
    /** Glyph color when the cursor is hovering the button — brighter so the affordance pops. */
    private static final int BUTTON_GLYPH_HOVER_COLOR = 0xFFFFFFFF;

    /** U+2212 (true minus sign) — visually wider than the ASCII hyphen, lines up with the plus. */
    private static final Component MINUS_GLYPH = Component.literal("−");
    private static final Component PLUS_GLYPH = Component.literal("+");

    private NumericPropertyEditor() {}

    /**
     * Integer step variant — exactly 1 per click, no rounding concerns.
     *
     * <ul>
     *   <li>Click in the minus button column → {@code currentValue - 1}</li>
     *   <li>Click in the plus button column → {@code currentValue + 1}</li>
     *   <li>Click in the value column between them → {@code currentValue} (no-op)</li>
     * </ul>
     */
    public static int applyIntClick(double mouseX, int editorX, int editorWidth, int currentValue) {
        if (mouseX < editorX + BUTTON_WIDTH) {
            return currentValue - 1;
        }
        if (mouseX >= editorX + editorWidth - BUTTON_WIDTH) {
            return currentValue + 1;
        }
        return currentValue;
    }

    /**
     * Float step variant — steps by {@value #FLOAT_STEP} and rounds the
     * result to the nearest tenth. Without the rounding, repeated clicks
     * accumulate the binary-fraction error in 0.1f and the display ends
     * up showing {@code 1.2000001}-style noise; the rounding keeps the
     * value text reading the same number the user would type.
     */
    public static float applyFloatClick(double mouseX, int editorX, int editorWidth, float currentValue) {
        if (mouseX < editorX + BUTTON_WIDTH) {
            return roundToTenth(currentValue - FLOAT_STEP);
        }
        if (mouseX >= editorX + editorWidth - BUTTON_WIDTH) {
            return roundToTenth(currentValue + FLOAT_STEP);
        }
        return currentValue;
    }

    /**
     * Double step variant — same step semantics as
     * {@link #applyFloatClick} but in double precision so a property
     * registered with {@code Codec.DOUBLE} round-trips through the
     * editor without a silent narrowing.
     */
    public static double applyDoubleClick(double mouseX, int editorX, int editorWidth, double currentValue) {
        if (mouseX < editorX + BUTTON_WIDTH) {
            return roundToTenth(currentValue - DOUBLE_STEP);
        }
        if (mouseX >= editorX + editorWidth - BUTTON_WIDTH) {
            return roundToTenth(currentValue + DOUBLE_STEP);
        }
        return currentValue;
    }

    /**
     * Draws the editor with a {@link Number} value. The number's natural
     * {@link Number#toString()} drives the text: {@code 8.0f} → "8.0",
     * {@code 20} → "20", {@code 1.5} → "1.5". Visual layout:
     * <pre>
     * +--------+-----------------+--------+
     * |   -    |       value     |   +    |
     * +--------+-----------------+--------+
     * </pre>
     * The value column is whatever's left between the two button columns;
     * truncation behavior for very long values (e.g., the sampler's
     * {@code seed}) is the font's standard "render past the bounds"
     * since clipping at this layer would require setting a scissor.
     */
    public static void render(GuiGraphicsExtractor graphics, Font font,
                              int editorX, int editorY, int editorWidth, int editorHeight,
                              int mouseX, int mouseY,
                              Number value, float alpha) {
        // Rounded pill background — the radius and BG color come from
        // the base class so the look stays consistent with the dropdown
        // editor.
        renderPill(graphics, editorX, editorY, editorWidth, editorHeight, BG_COLOR, alpha);

        int minusX = editorX;
        int plusX = editorX + editorWidth - BUTTON_WIDTH;
        boolean minusHovered = isInside(mouseX, mouseY, minusX, editorY, BUTTON_WIDTH, editorHeight);
        boolean plusHovered = isInside(mouseX, mouseY, plusX, editorY, BUTTON_WIDTH, editorHeight);

        if (minusHovered) {
            graphics.fill(minusX, editorY, minusX + BUTTON_WIDTH, editorY + editorHeight,
                    ARGB.multiply(BUTTON_HOVER_OVERLAY, ARGB.white(alpha)));
        }
        if (plusHovered) {
            graphics.fill(plusX, editorY, plusX + BUTTON_WIDTH, editorY + editorHeight,
                    ARGB.multiply(BUTTON_HOVER_OVERLAY, ARGB.white(alpha)));
        }

        // Glyphs + value text all align at the same vertical baseline so
        // the row reads as a single horizontal strip.
        int textY = textCenterY(editorY, editorHeight, font);

        int minusColor = ARGB.multiply(
                minusHovered ? BUTTON_GLYPH_HOVER_COLOR : BUTTON_GLYPH_COLOR,
                ARGB.white(alpha));
        graphics.centeredText(font, MINUS_GLYPH, minusX + BUTTON_WIDTH / 2, textY, minusColor);

        int plusColor = ARGB.multiply(
                plusHovered ? BUTTON_GLYPH_HOVER_COLOR : BUTTON_GLYPH_COLOR,
                ARGB.white(alpha));
        graphics.centeredText(font, PLUS_GLYPH, plusX + BUTTON_WIDTH / 2, textY, plusColor);

        // Value sits in the middle of the editor — centered between the
        // inside edges of the two button columns so an odd-width value
        // doesn't bias toward one button.
        int valueCenterX = editorX + editorWidth / 2;
        int valueColor = ARGB.multiply(VALUE_TEXT_COLOR, ARGB.white(alpha));
        graphics.centeredText(font, Component.literal(value.toString()), valueCenterX, textY, valueColor);
    }

    private static float roundToTenth(float v) {
        return Math.round(v * 10f) / 10f;
    }

    private static double roundToTenth(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}

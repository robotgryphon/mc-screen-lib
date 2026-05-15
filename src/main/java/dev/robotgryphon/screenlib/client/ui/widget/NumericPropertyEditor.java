package dev.robotgryphon.screenlib.client.ui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * Inline editor for a numeric property value. Renders a small rounded
 * pill with a minus glyph on the left, the current value in the middle,
 * and a plus glyph on the right — the same shape the KSampler-style
 * reference UI uses for its numeric fields. Designed to live inside the
 * value area of a {@code NodeWidget}'s property row or anywhere a
 * Minecraft layout container will accept a {@link PropertyEditor}.
 *
 * <p>Inherits its bounds, the pill background, the hit test, and the
 * text-centering math from {@link PropertyEditor}. The instance variant
 * carries the current value so a layout host can park the editor
 * somewhere and just call {@code setValue} as the model changes, while
 * the static {@link #render} entry point lets a caller draw a one-shot
 * editor directly from explicit bounds.
 *
 * <p>Rendering is shared across {@code int}, {@code float}, and
 * {@code double} via a {@link Number}-typed value field — the only
 * thing the three numeric types disagree on is the step size and the
 * post-step rounding to keep float drift from making "1.1 + 0.1" read as
 * {@code 1.2000001}. Each type therefore gets its own {@code apply*Click}
 * method (both as a static and as an instance flavor).
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

    /**
     * Current value displayed in the editor. Boxed because the same
     * editor instance can be repointed at a different numeric type as
     * long as the host knows which {@code apply*Click} variant to call;
     * type-specific rounding still happens through the static helpers.
     */
    private Number value;

    /**
     * Opacity multiplier baked into every color the editor draws — used
     * by {@code NodeWidget} for drag-time dimming when the active drag
     * type doesn't match the property's type. Defaults to fully opaque
     * so a layout-only host without a drag concept gets the obvious
     * behavior for free.
     */
    private float alpha = 1f;

    public NumericPropertyEditor(int x, int y, int width, int height, Number value) {
        super(x, y, width, height);
        this.value = value;
    }

    public Number getValue() {
        return this.value;
    }

    /**
     * Update the displayed value. Hosts call this after the underlying
     * property changes (e.g., the user clicked one of the buttons and
     * the {@code Node}'s property map was written), so the next render
     * shows the new number without rebuilding the editor.
     */
    public void setValue(Number value) {
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
        // Instance render path — defer to the existing static so the
        // pixel-perfect layout logic stays in one place.
        render(graphics, Minecraft.getInstance().font,
                this.x, this.y, this.width, this.height,
                mouseX, mouseY, this.value, this.alpha);
    }

    // -- Instance click helpers --------------------------------------------

    /**
     * Apply a click using this editor's stored bounds, treating the
     * value as an int. Returns the post-click value the caller should
     * write back through to the model.
     */
    public int applyIntClick(double mouseX) {
        return applyIntClick(mouseX, this.x, this.width, this.value.intValue());
    }

    /** Apply a click using this editor's stored bounds, treating the value as a float. */
    public float applyFloatClick(double mouseX) {
        return applyFloatClick(mouseX, this.x, this.width, this.value.floatValue());
    }

    /** Apply a click using this editor's stored bounds, treating the value as a double. */
    public double applyDoubleClick(double mouseX) {
        return applyDoubleClick(mouseX, this.x, this.width, this.value.doubleValue());
    }

    // -- Static API --------------------------------------------------------

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

package dev.robotgryphon.screenlib.client.ui.widget.property;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import java.util.Objects;
import java.util.function.Consumer;

public final class NumericPropertyEditor extends PropertyEditor {

    /**
     * Width of the minus / plus hit area on each side of the value column.
     */
    public static final int BUTTON_WIDTH = 12;

    /**
     * Step size for float / double adjustments. Int always steps by 1.
     */
    private static final float FLOAT_STEP = 0.1f;
    private static final double DOUBLE_STEP = 0.1;

    /**
     * Glyph color for the minus and plus marks.
     */
    private static final int BUTTON_GLYPH_COLOR = 0xFFB0B3BC;
    /**
     * Glyph color when the cursor is hovering the button — brighter so the affordance pops.
     */
    private static final int BUTTON_GLYPH_HOVER_COLOR = 0xFFFFFFFF;

    /**
     * U+2212 (true minus sign) — visually wider than the ASCII hyphen, lines up with the plus.
     */
    private static final Component MINUS_GLYPH = Component.literal("−");
    private static final Component PLUS_GLYPH = Component.literal("+");

    /**
     * Current value displayed in the editor. Boxed because the same
     * editor instance can be repointed at a different numeric type as
     * long as the host knows which step variant to call; type-specific
     * rounding still happens inside {@link #step}.
     */
    private Number value;

    private final Codec<?> codec;
    private final Consumer<Number> onValueChanged;

    /**
     * Inline child layout — three primary children laid out horizontally
     * with no gap between them: the minus button strip, the value /
     * {@link EditBox} column, and the plus button strip. The button
     * strips are {@link SpacerElement}s — pure position holders that
     * the layout slots into place; the editor draws the glyphs and
     * dispatches their clicks based on the spacers' arranged bounds.
     * The middle child is the actual {@link EditBox}, so its position
     * and size follow the layout without the editor having to compute
     * the value column rectangle by hand.
     */
    private final SpacerElement minusSpacer;
    private final SpacerElement plusSpacer;
    private final EditBox editBox;
    private final EqualSpacingLayout layout;

    private boolean editing;

    public NumericPropertyEditor(int x, int y, int width, int height,
                                 Number value, Codec<?> codec,
                                 Consumer<Number> onValueChanged) {
        super(x, y, width, height);
        this.value = value;
        this.codec = codec;
        this.onValueChanged = onValueChanged;

        // Build the EditBox eagerly so the layout doesn't shift when an
        // edit opens — the box exists from frame 1, it just isn't
        // focused / rendered until {@link #enterEditMode} flips the
        // {@code editing} flag. Its width is whatever's left after the
        // two button strips claim their {@link #BUTTON_WIDTH} each.
        Font font = Minecraft.getInstance().font;
        this.editBox = new EditBox(font, 0, 0,
                Math.max(0, width - 2 * BUTTON_WIDTH), height,
                Component.empty());
        this.editBox.setBordered(false);
        this.editBox.setMaxLength(32);
        this.editBox.setTextColor(VALUE_TEXT_COLOR);
        this.editBox.setCentered(true);

        // Layout owns the [minus | value | plus] arrangement; the editor
        // just submits draw calls relative to each child's arranged
        // bounds. Picking exact widths (no remaining space) means
        // {@link EqualSpacingLayout} produces zero gap between the
        // three children, giving a tightly-packed strip that fills
        // the editor's full width.
        this.minusSpacer = new SpacerElement(BUTTON_WIDTH, height);
        this.plusSpacer = new SpacerElement(BUTTON_WIDTH, height);
        this.layout = new EqualSpacingLayout(x, y, width, height,
                EqualSpacingLayout.Orientation.HORIZONTAL);
        this.layout.addChild(this.minusSpacer);
        this.layout.addChild(this.editBox);
        this.layout.addChild(this.plusSpacer);
        this.layout.arrangeElements();

        // Nudge the {@link EditBox} down so its visible text baseline
        // matches the static value text rendered in the same column
        // when not editing. {@link EditBox#bordered}=false makes the
        // box draw text at {@code getY()} with no height-based
        // centering, while the static text goes through
        // {@link #textCenterY} (which adds the {@code (h - lineHeight)
        // / 2 + 1} optical nudge); without the shift here the box
        // text sits a few pixels above where the +/- glyphs and the
        // idle value text land. Applied once post-arrange: subsequent
        // {@link AbstractLayout#setY} calls shift every child by the
        // same delta, so the offset is preserved as the editor moves
        // around with its host node.
        int textOffset = (height - font.lineHeight) / 2 + 1;
        this.editBox.setY(this.editBox.getY() + textOffset);
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

    /**
     * Keep the inner layout's origin in sync with this widget's position
     * — {@link net.minecraft.client.gui.layouts.AbstractLayout#setX}
     * shifts every child by the delta, so the minus / plus spacers and
     * the {@link EditBox} all follow the editor around without needing
     * a re-arrange.
     */
    @Override
    public void setX(int x) {
        super.setX(x);
        this.layout.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.layout.setY(y);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics,
                                            int mouseX, int mouseY, float partialTick) {
        int editorX = getX();
        int editorY = getY();
        int editorWidth = getWidth();
        int editorHeight = getHeight();

        renderPill(graphics, editorX, editorY, editorWidth, editorHeight, BG_COLOR, this.alpha);

        graphics.nextStratum();

        int minusX = this.minusSpacer.getX();
        int plusX = this.plusSpacer.getX();
        boolean minusHovered = isInside(mouseX, mouseY, minusX, editorY, BUTTON_WIDTH, editorHeight);
        boolean plusHovered = isInside(mouseX, mouseY, plusX, editorY, BUTTON_WIDTH, editorHeight);

        final var font = Minecraft.getInstance().font;

        // Glyphs + value text all align at the same vertical baseline so
        // the row reads as a single horizontal strip.
        int textY = textCenterY(editorY, editorHeight, font);

        int buttonColor = ARGB.multiply(BUTTON_GLYPH_COLOR, ARGB.white(this.alpha));
        int buttonHoverColor = ARGB.multiply(BUTTON_GLYPH_HOVER_COLOR, ARGB.white(this.alpha));

        graphics.text(font, MINUS_GLYPH,
                minusX + BUTTON_WIDTH / 2 - font.width(MINUS_GLYPH) / 2,
                textY, minusHovered ? buttonHoverColor : buttonColor, false);
        graphics.text(font, PLUS_GLYPH,
                plusX + BUTTON_WIDTH / 2 - font.width(PLUS_GLYPH) / 2,
                textY, plusHovered ? buttonHoverColor : buttonColor, false);

        // Middle column — static text when idle, live {@link EditBox}
        // when the user has clicked through to edit. The text and the
        // box share the same column (the layout sized them identically),
        // so the visual position is identical between the two modes;
        // only the cursor / caret and keyboard interactivity differ.
        if (!this.editing) {
            int valueCenterX = this.editBox.getX() + this.editBox.getWidth() / 2;
            int valueColor = ARGB.multiply(VALUE_TEXT_COLOR, ARGB.white(this.alpha));
            graphics.centeredText(font, Component.literal(this.value.toString()), valueCenterX, textY, valueColor);
        } else {
            this.editBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    // -- Click handling ----------------------------------------------------

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (isInside(mouseX, mouseY,
                this.minusSpacer.getX(), this.minusSpacer.getY(),
                this.minusSpacer.getWidth(), this.minusSpacer.getHeight())) {
            applyStep(-1);
            return;
        }
        if (isInside(mouseX, mouseY,
                this.plusSpacer.getX(), this.plusSpacer.getY(),
                this.plusSpacer.getWidth(), this.plusSpacer.getHeight())) {
            applyStep(+1);
            return;
        }
        enterEditMode();
    }

    /**
     * Computes the next value for a single step in {@code direction}
     * ({@code +1} or {@code -1}) under this editor's codec, writes it
     * back through {@link #onValueChanged} when it actually moved, and
     * keeps the editor's local snapshot in sync so subsequent renders
     * pick up the change before the host's per-frame value refresh.
     */
    private void applyStep(int direction) {
        Number next = step(direction);
        if (!Objects.equals(next, this.value)) {
            this.value = next;
            this.onValueChanged.accept(next);
        }
    }

    /**
     * Computes the post-step value under this editor's codec — int adds
     * {@code direction}, float / double add {@code direction *
     * FLOAT_STEP} (or the double equivalent) and round to the nearest
     * tenth. The rounding pass kills the binary-fraction drift that
     * would otherwise show {@code 1.2000001}-style noise after a few
     * clicks. Reference-equality dispatch on the codec singletons —
     * {@link Codec#INT}, {@link Codec#FLOAT}, {@link Codec#DOUBLE} are
     * distinct instances — is both correct and cheaper than instance
     * checks on the boxed value. Unknown codecs no-op back to the
     * current value.
     */
    private Number step(int direction) {
        if (this.codec == Codec.INT) {
            return this.value.intValue() + direction;
        }
        if (this.codec == Codec.FLOAT) {
            return roundToTenth(this.value.floatValue() + direction * FLOAT_STEP);
        }
        if (this.codec == Codec.DOUBLE) {
            return roundToTenth(this.value.doubleValue() + direction * DOUBLE_STEP);
        }
        return this.value;
    }

    // -- Inline edit lifecycle --------------------------------------------

    /**
     * True while the in-place {@link EditBox} is showing and accepting input.
     */
    public boolean isEditing() {
        return this.editing;
    }

    /**
     * Opens the in-place text edit. Seeds the box with the current
     * value's {@code toString()}, parks the cursor at the end (so the
     * user can immediately type more digits without needing to navigate
     * the seeded text), and grabs focus. No-op if already editing.
     */
    public void enterEditMode() {
        if (this.editing) return;
        this.editing = true;
        this.editBox.setValue(this.value.toString());
        this.editBox.moveCursorToEnd(false);
        this.editBox.setFocused(true);
    }

    /**
     * Parses the box's text under this editor's codec and writes the
     * result through {@link #onValueChanged} when it differs from the
     * current value. Unparseable input (e.g., a stray comma) silently
     * keeps the prior value — the user can correct it next time;
     * either way the box closes and focus is dropped.
     */
    public void commitEdit() {
        if (!this.editing) return;
        String text = this.editBox.getValue().trim();
        try {
            Number parsed = null;
            if (this.codec == Codec.INT) {
                parsed = Integer.parseInt(text);
            } else if (this.codec == Codec.FLOAT) {
                parsed = Float.parseFloat(text);
            } else if (this.codec == Codec.DOUBLE) {
                parsed = Double.parseDouble(text);
            }
            if (parsed != null && !Objects.equals(parsed, this.value)) {
                this.value = parsed;
                this.onValueChanged.accept(parsed);
            }
        } catch (NumberFormatException ignored) {
            // Bad input keeps the prior value — the user retains their
            // attempted text long enough to read it (since the box
            // remains visible during this method) but next time the
            // editor opens it'll be seeded from the unchanged value.
        }
        this.editing = false;
        this.editBox.setFocused(false);
    }

    /**
     * Abandons the in-place edit without writing back. The value
     * stays at whatever it was before the edit opened, and the box
     * drops focus. No-op if no edit is in flight.
     */
    public void cancelEdit() {
        if (!this.editing) return;
        this.editing = false;
        this.editBox.setFocused(false);
    }

    /**
     * Forwards a keyboard event to the in-place editor. {@code Enter}
     * (and the numpad equivalent) commits, {@code Escape} cancels,
     * everything else routes into the {@link EditBox} for normal text
     * input — cursor movement, deletion, selection, etc. Returns
     * {@code false} when no edit is open so the canvas's higher-level
     * key handling continues.
     */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.editing) return false;
        if (event.isConfirmation()) {
            commitEdit();
            return true;
        }
        if (event.isEscape()) {
            cancelEdit();
            return true;
        }
        return this.editBox.keyPressed(event);
    }

    /**
     * Forwards a character event to the in-place editor. Returns
     * {@code false} when no edit is open so the canvas's higher-level
     * char handling continues.
     */
    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!this.editing) return false;
        return this.editBox.charTyped(event);
    }

    /**
     * Click-routing helper for the canvas's outside-click commit flow.
     * When an edit is open:
     * <ul>
     *   <li>A click inside the {@link EditBox} goes through to it for
     *       cursor placement / selection; the method returns
     *       {@code true} so the canvas knows to consume the click.</li>
     *   <li>A click outside the {@link EditBox} commits the edit and
     *       returns {@code false}, letting the caller continue to
     *       process the click (re-targeting it to a different row /
     *       node / pan, etc.).</li>
     * </ul>
     * Mouse coordinates must be in the same space the {@link EditBox}'s
     * bounds were last rendered into — typically canvas space.
     */
    public boolean handleClickWhileEditing(MouseButtonEvent event, boolean doubleClick) {
        if (!this.editing) return false;
        if (this.editBox.isMouseOver(event.x(), event.y())) {
            this.editBox.mouseClicked(event, doubleClick);
            return true;
        }
        commitEdit();
        return false;
    }

    private static float roundToTenth(float v) {
        return Math.round(v * 10f) / 10f;
    }

    private static double roundToTenth(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}

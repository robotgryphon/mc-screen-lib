package dev.robotgryphon.screenlib.client.ui.widget.property;

import dev.robotgryphon.screenlib.client.ui.widget.RoundedShapes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.ARGB;

import java.util.function.Consumer;

/**
 * Inline editor for a boolean-codec property. Renders as a small
 * pill-shaped toggle track with a circular thumb that sits at the left
 * end when the value is {@code false} and the right end when
 * {@code true} — the same shape mobile / Material-style switches use.
 * A click anywhere in the editor area flips the value; there's no
 * dedicated "slide" gesture because the value range is binary and the
 * snap-tap UX is what users already expect from switches at this size.
 *
 * <p>Inherits its bounds, hit test, and layout integration from
 * {@link PropertyEditor}, so a host can either drop an instance into a
 * Minecraft layout container or call the static {@link #render(
 * GuiGraphicsExtractor, int, int, int, int, int, int, boolean, float)}
 * directly from explicit row geometry (the pattern
 * {@link NumericPropertyEditor} and {@link DropdownEditor} also use).
 *
 * <p>Visual style targets the reference screenshot's switch: a sunken
 * gray track for off, a saturated blue track for on, and the thumb
 * inverting between a soft light-gray (off) and a deep node-body shade
 * (on) so the bright on-track always has a high-contrast thumb sitting
 * on top of it.
 */
public final class BooleanPropertyEditor extends PropertyEditor {

    /**
     * Width of the toggle track in pixels — picked compact so the
     * editor fits comfortably in the right edge of a property row's
     * value area without crowding the row's label.
     */
    private static final int TRACK_WIDTH = 20;
    /**
     * Height of the toggle track. Sits inside the
     * {@link dev.robotgryphon.screenlib.graph.Node#PROPERTY_PITCH}
     * 14-pixel row with a 2-pixel breathing strip above and below.
     */
    private static final int TRACK_HEIGHT = 10;
    /** Inset of the thumb from the track edge — small enough that the thumb dominates the pill visually. */
    private static final int THUMB_INSET = 1;
    /** Computed thumb diameter — track height minus inset on each side. */
    private static final int THUMB_SIZE = TRACK_HEIGHT - 2 * THUMB_INSET;
    /** Right-edge padding between the track and the editor's outer right edge. */
    private static final int RIGHT_PADDING = 2;

    /** Off-state track color — muted gray that reads as a "sunken" off switch against the node body. */
    private static final int TRACK_OFF_COLOR = 0xFF4A4A55;
    /** Off-state thumb color — soft mid-gray, distinct from the track without being loud. */
    private static final int THUMB_OFF_COLOR = 0xFFB0B3BC;
    /** On-state track color — saturated blue matching the reference screenshot's "active" tint. */
    private static final int TRACK_ON_COLOR = 0xFF4A90E2;
    /** On-state thumb color — deep node-body shade so the bright on-track reads through cleanly. */
    private static final int THUMB_ON_COLOR = 0xFF1F1F23;
    /** Subtle white overlay applied to the track when the cursor is anywhere over the editor area. */
    private static final int HOVER_OVERLAY = 0x22FFFFFF;

    /** Current value displayed by the switch. */
    private boolean value;

    /**
     * Callback invoked with the new value when the user clicks the
     * switch. Receives the post-toggle state ({@code !currentValue}) so
     * the host can route straight into a property write — no need to
     * read the editor's value back to compute the next.
     */
    private final Consumer<Boolean> onValueChanged;

    public BooleanPropertyEditor(int x, int y, int width, int height,
                                 boolean value, Consumer<Boolean> onValueChanged) {
        super(x, y, width, height);
        this.value = value;
        this.onValueChanged = onValueChanged;
    }

    public boolean getValue() {
        return this.value;
    }

    /**
     * Update the value shown by the switch. Hosts call this each frame
     * from their render path so external writes to the underlying
     * property (e.g., a value loaded from disk) flow into the editor
     * without rebuilding it. Click-time writes go through the
     * {@link #onValueChanged} callback instead, with this field updated
     * inside {@link #onClick} before the callback fires.
     */
    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics,
                                            int mouseX, int mouseY, float partialTick) {
        // Font isn't read by the switch — no value text — but matching
        // the other editors' signature keeps the call site symmetric.
        render(graphics, getX(), getY(), getWidth(), getHeight(),
                mouseX, mouseY, this.value, getAlpha());
    }

    /**
     * Click anywhere on the editor surface flips the value and fires
     * the callback. The bounds check is handled by
     * {@link AbstractWidget#mouseClicked} — when {@code onClick} fires
     * the click has already landed on the editor — so there's no
     * positional dispatch beyond "the whole pill is the target."
     */
    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.value = !this.value;
        this.onValueChanged.accept(this.value);
    }

    // -- Static API --------------------------------------------------------

    /**
     * Draws the switch:
     * <pre>
     * +-----------+        +-----------+
     * | (o)       |        |       (o) |
     * +-----------+        +-----------+
     *  (off, gray)         (on, blue)
     * </pre>
     * The track is right-aligned inside the editor area so the switch
     * lines up with the value column of every other editor type. The
     * thumb snaps to one of two positions inside the track — no
     * intermediate states, no animation; a click flips the value and
     * the next render lands the thumb on the opposite end.
     *
     * <p>{@code mouseX}/{@code mouseY} drive a soft hover overlay on
     * the track (not just the thumb), matching the "the whole pill is
     * a single click target" feel of the rest of the editors. The
     * {@code Font} parameter on the sibling editors' renders is
     * intentionally omitted here — the switch carries no text.
     */
    public static void render(GuiGraphicsExtractor graphics,
                              int editorX, int editorY, int editorWidth, int editorHeight,
                              int mouseX, int mouseY,
                              boolean value, float alpha) {
        // Track right-aligned in the editor area, vertically centered
        // on the row. RIGHT_PADDING leaves a sliver of breathing room
        // between the track and the node's outer border.
        int trackX = editorX + editorWidth - RIGHT_PADDING - TRACK_WIDTH;
        int trackY = editorY + (editorHeight - TRACK_HEIGHT) / 2;

        int trackColor = value ? TRACK_ON_COLOR : TRACK_OFF_COLOR;
        int thumbColor = value ? THUMB_ON_COLOR : THUMB_OFF_COLOR;
        // On: thumb pinned to the right. Off: thumb pinned to the left.
        int thumbX = value
                ? trackX + TRACK_WIDTH - THUMB_INSET - THUMB_SIZE
                : trackX + THUMB_INSET;
        int thumbY = trackY + THUMB_INSET;

        // Track pill — corner radius equals half the track height so
        // both ends round into perfect semicircles.
        int trackRadius = TRACK_HEIGHT / 2;
        RoundedShapes.fillRoundedRect(graphics, trackX, trackY,
                trackX + TRACK_WIDTH, trackY + TRACK_HEIGHT,
                trackRadius,
                ARGB.multiply(trackColor, ARGB.white(alpha)));

        // Hover overlay — applied to the track when the cursor is
        // anywhere within the editor area (not just the visual track),
        // so the user gets early feedback that the click target is
        // live before their cursor lands exactly on the pill.
        if (isInside(mouseX, mouseY, editorX, editorY, editorWidth, editorHeight)) {
            RoundedShapes.fillRoundedRect(graphics, trackX, trackY,
                    trackX + TRACK_WIDTH, trackY + TRACK_HEIGHT,
                    trackRadius,
                    ARGB.multiply(HOVER_OVERLAY, ARGB.white(alpha)));
        }

        // Thumb — corner radius equals half the thumb size so the
        // square renders as a circle inside the pill.
        RoundedShapes.fillRoundedRect(graphics, thumbX, thumbY,
                thumbX + THUMB_SIZE, thumbY + THUMB_SIZE,
                THUMB_SIZE / 2,
                ARGB.multiply(thumbColor, ARGB.white(alpha)));
    }
}

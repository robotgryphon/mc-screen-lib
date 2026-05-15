package dev.robotgryphon.screenlib.client.ui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.util.ARGB;

import java.util.function.Consumer;

/**
 * Shared base for the inline editors that render inside a property row's
 * value area — currently {@link NumericPropertyEditor} (the [− value +]
 * spinner) and {@link DropdownEditor} (the value-and-chevron trigger).
 *
 * <p>Implements both {@link LayoutElement} and {@link Renderable} so an
 * editor can be dropped straight into a Minecraft layout container the
 * same way any other widget would be ({@code HeaderAndFooterLayout},
 * grids, frames, etc.). The required state — bounds rectangle plus a
 * subclass-supplied value — lives on the instance; getters and setters
 * follow the {@code LayoutElement} contract verbatim so layout passes can
 * move and resize an editor without knowing what kind it is.
 *
 * <p>The class also keeps the original static helpers
 * ({@link #isInside(double, double, int, int, int, int)},
 * {@link #textCenterY(int, int, Font)}, {@link #renderPill}, and the
 * static {@code render(...)} / {@code apply*Click(...)} on each subclass).
 * Existing callers that draw an editor directly from a {@code NodeWidget}'s
 * row geometry don't have to allocate an instance just to render one
 * row — they keep their tight per-frame loop and pass bounds explicitly.
 * The instance API is additive: useful when an editor needs to live in a
 * layout, ignorable when it doesn't.
 *
 * <p>Subclasses are expected to construct themselves with a bounds
 * rectangle and a current value, expose setters for both (so a host can
 * reposition or update the value between frames without throwing the
 * editor away), and implement {@link #extractRenderState} by delegating
 * to their own static {@code render(...)} method with the instance's
 * stored fields.
 */
public abstract class PropertyEditor implements LayoutElement, Renderable {

    /**
     * Corner radius applied to every editor's pill background. Smaller
     * than the node's outer radius so the editor reads as a control
     * "inside" the node rather than a second card sitting on top.
     */
    public static final int CORNER_RADIUS = 2;

    /**
     * Default pill background — the sunk shade both editors share when
     * idle. Editors that want a hover-elevated background (the dropdown
     * trigger) override with their own color at the call site.
     */
    protected static final int BG_COLOR = 0xCC1A1B22;

    /** Bright white for the value text in the middle of any editor. */
    protected static final int VALUE_TEXT_COLOR = 0xFFFFFFFF;

    // -- Bounds state ------------------------------------------------------

    protected int x;
    protected int y;
    protected int width;
    protected int height;

    protected PropertyEditor(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // -- LayoutElement -----------------------------------------------------

    @Override public int getX() { return this.x; }
    @Override public int getY() { return this.y; }
    @Override public int getWidth() { return this.width; }
    @Override public int getHeight() { return this.height; }

    @Override public void setX(int x) { this.x = x; }
    @Override public void setY(int y) { this.y = y; }

    /**
     * Resize the editor without moving its top-left corner. Used by
     * hosts that need to fit the editor into a known column width;
     * height typically stays at the property row pitch but is exposed
     * for symmetry.
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Editors don't contain {@link AbstractWidget} children — they draw
     * themselves directly using {@link GuiGraphicsExtractor} — so the
     * layout system has nothing to visit here. The empty implementation
     * is required because {@code visitWidgets} has no default on
     * {@link LayoutElement}.
     */
    @Override public void visitWidgets(Consumer<AbstractWidget> visitor) {}

    // -- Instance-flavored helpers ----------------------------------------

    /** Bounds test against this editor's current rectangle. */
    public boolean isInside(double mouseX, double mouseY) {
        return isInside(mouseX, mouseY, this.x, this.y, this.width, this.height);
    }

    /** Vertical text baseline centered against this editor's current rectangle. */
    protected int textCenterY(Font font) {
        return textCenterY(this.y, this.height, font);
    }

    /** Pill background drawn at this editor's current bounds. */
    protected void renderPill(GuiGraphicsExtractor graphics, int color, float alpha) {
        renderPill(graphics, this.x, this.y, this.width, this.height, color, alpha);
    }

    // -- Static helpers ----------------------------------------------------

    /**
     * Bounds test — true when the screen-space point lands inside the
     * editor's outer rectangle. Available as a static so callers that
     * already have the row geometry on hand can skip allocating an
     * editor instance just to perform the test.
     */
    public static boolean isInside(double mouseX, double mouseY,
                                   int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    /**
     * The y-coordinate to pass to {@code graphics.text} so the text
     * lands vertically centered inside the editor. The {@code +1} is
     * an optical-center nudge — the font's pixel grid has a small bias
     * toward the top of the line height, so the geometric center sits
     * one pixel low without it.
     */
    protected static int textCenterY(int y, int height, Font font) {
        return y + (height - font.lineHeight) / 2 + 1;
    }

    /**
     * Draws the editor's rounded pill background. {@code color} is the
     * pill's fill color (usually {@link #BG_COLOR}, or a state-specific
     * shade for hover/disabled/etc.); {@code alpha} is the row's
     * effective alpha so the pill participates in any drag-time dimming
     * the surrounding {@code NodeWidget} is doing.
     */
    protected static void renderPill(GuiGraphicsExtractor graphics,
                                     int x, int y, int width, int height,
                                     int color, float alpha) {
        RoundedShapes.fillRoundedRect(graphics, x, y, x + width, y + height,
                CORNER_RADIUS,
                ARGB.multiply(color, ARGB.white(alpha)));
    }
}

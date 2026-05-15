package dev.robotgryphon.screenlib.client.ui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;

/**
 * Shared base for the inline editors that render inside a property row's
 * value area — currently {@link NumericPropertyEditor} (the [− value +]
 * spinner) and {@link DropdownEditor} (the value-and-chevron trigger).
 *
 * <p>The two editors had drifted into duplicating each other's pill
 * background fill, hit-test bounds, and vertical text-centering math.
 * Hoisting those into a single base lets the corner radius, background
 * color, and "pill shape" live in one place, so a change to the visual
 * style (e.g., bumping the radius or switching the background tone) only
 * has to land here.
 *
 * <p>The class stays abstract and only carries static helpers — the
 * editors aren't instance-based today (each one is a stateless utility
 * with a public {@code render(...)} entry point), and a constructor-
 * required base class would have rippled out into every call site with
 * no benefit. Subclasses inherit the protected statics by class
 * extension; outside callers still go through the subclass's own
 * static API ({@code NumericPropertyEditor.render(...)},
 * {@code DropdownEditor.isInside(...)}, etc.).
 *
 * <p>Subclasses are sealed shut with a private constructor on each side
 * — there's nothing to construct, the extension is purely so the
 * shared statics are reachable without qualifying the base class name.
 */
public abstract class PropertyEditor {

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

    protected PropertyEditor() {}

    /**
     * Bounds test — true when the screen-space point lands inside the
     * editor's outer rectangle. Inherited by every subclass so callers
     * can also write {@code NumericPropertyEditor.isInside(...)} or
     * {@code DropdownEditor.isInside(...)} interchangeably.
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

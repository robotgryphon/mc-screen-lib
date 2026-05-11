package dev.robotgryphon.screenlib.client.ui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Material-style chip widget. Visual structure, left to right:
 *
 * <pre>
 *   ┌──────────────────────────────────────┐
 *   │  ●  CATEGORY  Value             ×    │
 *   └──────────────────────────────────────┘
 * </pre>
 *
 * <ul>
 *   <li>Rounded-rectangle background.</li>
 *   <li>Small colored dot — caller-supplied color (typically the data type's
 *       palette color, e.g. green for {@code block_pos}).</li>
 *   <li>{@code category} text, rendered in ALL CAPS regardless of casing in
 *       the supplied {@link Component}.</li>
 *   <li>{@code value} text, rendered as-is.</li>
 *   <li>Optional {@code ×} dismiss button on the right. Present only when an
 *       {@code onRemove} callback is supplied; clicking it fires the callback.</li>
 * </ul>
 *
 * <p>Sized automatically from the active client font — the chip measures its
 * category and value at construction and reports a {@link #getWidth} that
 * fits both with the right margins. Callers position it via {@link #setX} /
 * {@link #setY}; the {@link ChipHolder} container does that automatically.
 */
public class Chip extends AbstractWidget {

    /** Fixed chip height — chosen to fit MC's bitmap font with some breathing room. */
    public static final int CHIP_HEIGHT = 14;

    /** Corner-radius for the rounded background. */
    private static final int CORNER_RADIUS = 3;
    /** Horizontal padding inside the chip (each side). */
    private static final int PADDING_X = 5;
    /** Radius of the colored dot (so the dot is {@code 2*r+1} pixels wide/tall). */
    private static final int DOT_RADIUS = 2;
    /** Gap between the dot and the start of the category text. */
    private static final int DOT_GAP = 4;
    /** Gap between the category text and the value text. */
    private static final int LABEL_GAP = 4;
    /** Gap between the value text and the dismiss button. */
    private static final int DISMISS_GAP = 4;
    /** Width reserved for the dismiss-button hit region (drawn glyph centered inside). */
    private static final int DISMISS_W = 6;

    private static final int BG_NORMAL = 0xFF2F2F38;
    private static final int BG_HOVER = 0xFF3F3F49;
    private static final int BORDER_COLOR = 0xFF50505A;
    private static final int CATEGORY_COLOR = 0xFFB5B5C0;
    private static final int VALUE_COLOR = 0xFFFFFFFF;
    private static final int DISMISS_COLOR = 0xFFC0C0C8;
    private static final int DISMISS_HOVER_COLOR = 0xFFFFFFFF;

    /**
     * Back-reference to the holder that owns this chip. Final, set at
     * construction; combined with the private constructor + package-private
     * builder, this guarantees every Chip belongs to exactly one holder for
     * its entire life. Dismiss always calls {@code holder.removeChip(this)}
     * — there's no way to install a callback that does anything else.
     */
    private final ChipHolder holder;
    private final int dotColor;
    private final Component category;
    private final Component value;
    private final boolean dismissable;
    /**
     * Opaque caller-supplied payload. Lets the chip carry metadata (e.g. the
     * filter clause it represents) without polluting the widget's API with
     * per-feature fields. Read via {@link #data}; type is the caller's
     * responsibility.
     */
    private final @org.jspecify.annotations.Nullable Object data;
    /** Pre-measured text widths so layout / hit-testing don't re-measure every frame. */
    private final int catTextWidth;
    private final int valTextWidth;

    /**
     * Private — chips can only be constructed through {@link Builder#build}
     * (called by {@link ChipHolder}). This enforces the invariant that every
     * chip belongs to a holder; there is no way to obtain a free-floating
     * {@code Chip}.
     */
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

    /**
     * Caller-supplied payload captured at build time. Typed by the caller —
     * the chip just holds it. Useful for letting the holder's owner iterate
     * {@code chips()} and recover whatever metadata it associated with each
     * chip (e.g., the filter clause a chip represents).
     */
    public @org.jspecify.annotations.Nullable Object data() {
        return this.data;
    }

    /** Entry point for the fluent builder; see {@link Builder}. */
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
        // The only click target inside the chip is the dismiss button. Clicks
        // anywhere else on the chip are accepted (so the parent doesn't see
        // them) but do nothing; chips don't yet expose a primary action.
        if (this.dismissable && hitDismiss(event.x(), event.y())) {
            // Dismiss is hardwired — it always removes from the owning holder.
            // There's no user-supplied callback that could divert this.
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

        // Border + fill — drawn as two filled rounded rects so the visible
        // 1-pixel border is just the outer fill peeking around the inner one.
        fillRoundedRect(graphics, x, y, w, h, CORNER_RADIUS, BORDER_COLOR);
        fillRoundedRect(graphics, x + 1, y + 1, w - 2, h - 2,
                Math.max(0, CORNER_RADIUS - 1), hovered ? BG_HOVER : BG_NORMAL);

        // Layout cursor walks the chip left to right.
        int cursor = x + PADDING_X;

        // Dot, vertically centered on the chip.
        int dotCx = cursor + DOT_RADIUS;
        int dotCy = y + h / 2;
        fillDot(graphics, dotCx, dotCy, DOT_RADIUS, this.dotColor);
        cursor += (2 * DOT_RADIUS + 1) + DOT_GAP;

        Font font = Minecraft.getInstance().font;
        int textY = y + (h - font.lineHeight) / 2 + 1;

        // ALL-CAPS category, then the value, then (optionally) the dismiss glyph.
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
        // Casefold via getString — loses inline component styling, but chip
        // categories are short tag-style labels where that's a non-issue.
        return Component.literal(c.getString().toUpperCase(Locale.ROOT));
    }

    /**
     * Stair-step rounded rectangle. {@code radius} controls how many pixels
     * are cut at each corner — {@code r=1} cuts one corner pixel, {@code r=3}
     * cuts a 2-pixel step plus a 1-pixel step before reaching the full edge.
     * Cheap, pixel-clean, and looks fine at chip sizes.
     */
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

    /**
     * Filled "dot": a {@code (2r+1) × (2r+1)} square with its four corner
     * pixels trimmed for a softer, more circular silhouette at small sizes.
     */
    private static void fillDot(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int trim = (Math.abs(dy) == r) ? 1 : 0;
            graphics.fill(cx - r + trim, cy + dy, cx + r + 1 - trim, cy + dy + 1, color);
        }
    }

    /**
     * Fluent builder for {@link Chip}. Public so callers can configure
     * chips at their leisure, but {@link #build(ChipHolder)} is
     * package-private — the only way to actually obtain a Chip instance is
     * to pass the builder to a {@link ChipHolder} via
     * {@code holder.addChip(builder)} or
     * {@code holder.addChip(b -> ...)}. This guarantees every chip is
     * bound to a holder for its entire life.
     *
     * <p>Typical usage:
     * <pre>{@code
     * holder.addChip(b -> b
     *     .dotColor(0xFF55D784)
     *     .category(Component.translatable("filter.category.input"))
     *     .value(Component.literal("Position"))
     *     .dismissable());
     * }</pre>
     *
     * <p>Both {@link #category} and {@link #value} take a {@link Component} —
     * there's intentionally no {@code String} overload, so callers compose
     * styled components (translatables, color/format runs, etc.) without
     * having to wrap-in-{@code Component.literal} at every site.
     *
     * <p>The dot color defaults to opaque white; chips are non-dismissable
     * unless {@link #dismissable()} is called. The dismiss action itself is
     * hardwired inside {@link Chip} to call {@code holder.removeChip(this)}
     * — callers cannot supply their own callback, so there's no way for a
     * chip's "remove" affordance to do anything other than what it says.
     */
    public static final class Builder {
        /** Sensible default so a builder with only category + value still renders something visible. */
        private int dotColor = 0xFFFFFFFF;
        private Component category;
        private Component value;
        private boolean dismissable;
        private @org.jspecify.annotations.Nullable Object data;

        private Builder() {
        }

        /** ARGB color of the leading dot. */
        public Builder dotColor(int color) {
            this.dotColor = color;
            return this;
        }

        /**
         * Category label — rendered in ALL CAPS by the chip. Accepts a
         * {@link Component} only (no {@code String} overload) so styling and
         * translation are first-class at the call site.
         */
        public Builder category(Component category) {
            this.category = category;
            return this;
        }

        /** Value label — rendered as-is. */
        public Builder value(Component value) {
            this.value = value;
            return this;
        }

        /**
         * Marks the chip as dismissable: the rendered chip gets a {@code ×}
         * glyph on the right, and clicking it removes the chip from its
         * owning holder. The remove action is fixed and cannot be diverted
         * to other behavior — that's the whole point of the invariant.
         */
        public Builder dismissable() {
            this.dismissable = true;
            return this;
        }

        /**
         * Attaches an opaque caller-supplied payload to the chip, retrievable
         * later via {@link Chip#data()}. The chip itself doesn't interpret
         * the value — typing is the caller's job.
         */
        public Builder data(@org.jspecify.annotations.Nullable Object data) {
            this.data = data;
            return this;
        }

        /**
         * Package-private. Invoked by {@link ChipHolder} when the builder is
         * handed to one of its {@code addChip} overloads.
         *
         * @throws IllegalStateException if either {@link #category} or
         *         {@link #value} was not provided
         */
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

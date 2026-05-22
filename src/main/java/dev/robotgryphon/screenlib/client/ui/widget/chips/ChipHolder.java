package dev.robotgryphon.screenlib.client.ui.widget.chips;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Horizontal container for {@link Chip}s. Chips can be added or removed at
 * any time; the holder re-lays them out left-to-right with a small gap on
 * every change, and keeps its own reported width in sync with the row so
 * the parent screen's bounds-based hit-tests reach every chip.
 *
 * <p>Manages its children's rendering and event dispatch directly rather
 * than going through {@code AbstractContainerWidget} — the holder is added
 * to its parent screen as a regular renderable widget; its chips do not
 * need to be. Chips render in declaration order; events dispatch in reverse
 * so the rightmost chip wins overlapping clicks (chips don't overlap today,
 * but the convention matches stock widget containers).
 */
public class ChipHolder extends AbstractWidget {

    /** Pixels between adjacent chips in the row. */
    private static final int CHIP_GAP = 4;

    private final List<Chip> chips = new ArrayList<>();
    /**
     * Optional listener fired whenever the chip set changes (add, remove,
     * clear). Owners of the holder use this to re-derive whatever state
     * depends on the active chips — e.g., re-applying a filter.
     */
    private @Nullable Runnable onChange;

    /**
     * @param x left edge of the row in screen coordinates
     * @param y top edge of the row in screen coordinates
     */
    public ChipHolder(int x, int y) {
        super(x, y, 0, Chip.CHIP_HEIGHT, Component.empty());
    }

    /**
     * Installs a listener invoked after any chip is added, removed, or
     * cleared. The listener is the only outward signal of chip mutation;
     * the holder doesn't fire callbacks for layout-only changes.
     */
    public ChipHolder setOnChange(@Nullable Runnable onChange) {
        this.onChange = onChange;
        return this;
    }

    /**
     * Builds {@code builder} into a {@link Chip} bound to this holder and
     * appends it to the row. The holder takes over positioning — callers
     * shouldn't set the chip's x/y themselves.
     *
     * <p>The chip's dismiss button (when {@link Chip.Builder#dismissable()}
     * was called) is hardwired inside {@code Chip} to remove the chip from
     * this exact holder. There's no callback the caller can supply — the
     * dismiss affordance always does what its glyph promises.
     *
     * @return the newly-added chip
     */
    public Chip addChip(Chip.Builder builder) {
        return insertChip(builder.build(this));
    }

    /**
     * Hands the configurator a fresh {@link Chip.Builder}, lets it fill in
     * the caller-specific fields, then builds and appends the resulting chip
     * bound to this holder.
     *
     * <p>The configurator usually only has to set {@code category},
     * {@code value}, and optionally {@link Chip.Builder#dotColor(int)} and
     * {@link Chip.Builder#dismissable()}.
     *
     * @return the newly-added chip
     */
    public Chip addChip(Consumer<Chip.Builder> configurator) {
        Chip.Builder builder = Chip.builder();
        configurator.accept(builder);
        return insertChip(builder.build(this));
    }

    /** Common tail of both {@code addChip} overloads. */
    private Chip insertChip(Chip chip) {
        this.chips.add(chip);
        layoutChips();
        notifyChange();
        return chip;
    }

    /**
     * Removes {@code chip} if present. Subsequent chips shift left to close
     * the gap.
     */
    public boolean removeChip(@Nullable Chip chip) {
        if (chip == null || !this.chips.remove(chip)) {
            return false;
        }
        layoutChips();
        notifyChange();
        return true;
    }

    /** Empties the row. */
    public void clearChips() {
        if (this.chips.isEmpty()) {
            return;
        }
        this.chips.clear();
        layoutChips();
        notifyChange();
    }

    private void notifyChange() {
        if (this.onChange != null) {
            this.onChange.run();
        }
    }

    /**
     * Current chips in insertion order. Returns a fresh {@link Stream} —
     * the underlying list is the holder's mutable state, so callers should
     * collect or process eagerly rather than holding the stream across
     * mutations.
     */
    public Stream<Chip> chips() {
        return Collections.unmodifiableList(this.chips).stream();
    }

    public int chipCount() {
        return this.chips.size();
    }

    public boolean isEmpty() {
        return this.chips.isEmpty();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        layoutChips();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        layoutChips();
    }

    private void layoutChips() {
        int cursor = this.getX();
        int chipY = this.getY();
        for (Chip chip : this.chips) {
            chip.setX(cursor);
            chip.setY(chipY);
            cursor += chip.getWidth() + CHIP_GAP;
        }
        // Holder width tracks the row so the parent screen's bounds-based
        // hit-test reaches every chip. Trailing CHIP_GAP belongs to the
        // *next* chip and doesn't count toward our width.
        int totalWidth = this.chips.isEmpty()
                ? 0
                : (cursor - this.getX() - CHIP_GAP);
        this.setWidth(totalWidth);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.active || !this.visible) {
            return false;
        }
        // Reverse order: chips on the right (drawn last, conceptually on top)
        // get first claim on overlapping clicks. Matches stock container
        // dispatch conventions and is forward-compatible with any future
        // overlapping-chip rendering.
        for (int i = this.chips.size() - 1; i >= 0; i--) {
            Chip chip = this.chips.get(i);
            if (chip.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        for (Chip chip : this.chips) {
            chip.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // Narration is per-chip; the holder itself is silent.
    }
}

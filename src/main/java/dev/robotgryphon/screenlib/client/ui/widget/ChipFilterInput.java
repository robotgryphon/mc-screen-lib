package dev.robotgryphon.screenlib.client.ui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Composite filter input: combines the responsibilities of a search field
 * and a {@link ChipHolder} into one widget, with a typed suggestions popup
 * sourced from a {@link ChipSuggestionsProvider}.
 *
 * <p>Layout (top to bottom, within the widget's static bounds):
 * <ol>
 *   <li>A row containing a {@link CycleButton} (picks the active
 *       <em>filter type</em>) and an {@link EditBox} (stages a value).</li>
 *   <li>A {@link ChipHolder} showing every committed filter.</li>
 * </ol>
 *
 * <p>While the search field is focused and non-empty, the provider is
 * queried with the current filter type + raw input and the returned list is
 * rendered as a popup <em>below</em> the widget's static bounds. The popup
 * extends past {@link #getHeight} into whatever's below, drawn at an
 * elevated stratum so it overlaps other widgets. {@link #isMouseOver} is
 * widened to include the popup region so clicks on it reach this widget.
 *
 * <p>Picking a suggestion (click or Enter) commits a {@link Chip} into the
 * holder. The chip's {@code data} slot stores a {@link ChipFilter} record
 * pairing the active filter type with the suggestion's opaque value, which
 * consumers recover by streaming {@link #filters()}.
 *
 * @param <F> the filter-type identifier the host cycles through
 */
public class ChipFilterInput<F> extends AbstractWidget {

    /** Width of the filter-type {@link CycleButton}. */
    private static final int MODE_BUTTON_W = 56;
    /** Height of the search row (cycle + edit). */
    private static final int SEARCH_ROW_H = 16;
    /** Gap between the search row and the chip row. */
    private static final int ROW_GAP = 4;
    /** Horizontal spacing between the cycle button and the search field. */
    private static final int FILTER_SPACING = 4;

    /** Height of each suggestion row in the popup. */
    private static final int SUGGESTION_ROW_H = 12;
    /** Vertical gap between the widget's bottom edge and the popup's top edge. */
    private static final int SUGGESTION_GAP = 4;
    /** Padding inside the popup, top/bottom and around row content. */
    private static final int SUGGESTION_PADDING = 3;
    /** Radius of the colored dot on each suggestion row. */
    private static final int SUGGESTION_DOT_RADIUS = 2;
    /** Gap between the suggestion dot and the label text. */
    private static final int SUGGESTION_DOT_GAP = 5;
    /** Hard cap on how many suggestions appear at once — providers can also cap. */
    private static final int MAX_SUGGESTIONS = 8;

    private static final int POPUP_BG = 0xEE1F1F23;
    private static final int POPUP_BORDER = 0xFF50505A;
    private static final int POPUP_HOVER = 0x33FFFFFF;
    private static final int POPUP_TEXT = 0xFFE0E0E0;

    private final List<F> modes;
    private final Function<F, Component> modeLabel;
    private final Function<F, Integer> modeColor;
    private final ChipSuggestionsProvider<F> provider;

    private final CycleButton<F> modeButton;
    private final EditBox searchField;
    private final ChipHolder chipHolder;

    private F currentMode;
    private List<ChipSuggestion> activeSuggestions = List.of();
    private @Nullable Runnable onChange;

    /**
     * @param x            left edge in screen coordinates
     * @param y            top edge in screen coordinates
     * @param width        total widget width; the search field takes whatever the cycle button doesn't
     * @param initialMode  filter type the cycle button starts on
     * @param modes        every filter type the cycle button will cycle through
     * @param modeLabel    display name per filter type (used by the cycle button and by chips' category text)
     * @param modeColor    fallback chip dot color used when a chip is committed without a suggestion
     *                     (e.g., raw-text Enter when the provider returned no matches)
     * @param provider     suggestion source consulted on every keystroke + filter-type change
     */
    public ChipFilterInput(int x, int y, int width,
                           F initialMode,
                           List<F> modes,
                           Function<F, Component> modeLabel,
                           Function<F, Integer> modeColor,
                           ChipSuggestionsProvider<F> provider) {
        super(x, y, width, SEARCH_ROW_H + ROW_GAP + Chip.CHIP_HEIGHT, Component.empty());
        this.modes = List.copyOf(modes);
        this.modeLabel = modeLabel;
        this.modeColor = modeColor;
        this.provider = provider;
        this.currentMode = initialMode;

        Font font = Minecraft.getInstance().font;
        this.modeButton = CycleButton.<F>builder(this.modeLabel::apply, initialMode)
                .withValues(this.modes)
                .displayOnlyValue()
                .create(x, y, MODE_BUTTON_W, SEARCH_ROW_H, Component.empty(),
                        (b, value) -> {
                            this.currentMode = value;
                            refreshSuggestions();
                            // The staged text now reads under a different filter type;
                            // consumers AND-ing chips + staged text need to re-evaluate.
                            fireOnChange();
                        });

        int searchX = x + MODE_BUTTON_W + FILTER_SPACING;
        int searchW = Math.max(0, width - MODE_BUTTON_W - FILTER_SPACING);
        this.searchField = new EditBox(font, searchX, y, searchW, SEARCH_ROW_H, Component.empty());
        this.searchField.setHint(Component.literal("Search…"));
        this.searchField.setMaxLength(64);
        this.searchField.setResponder(s -> {
            refreshSuggestions();
            // Staged text changed → consumers may want to live-filter on it.
            fireOnChange();
        });

        int chipY = y + SEARCH_ROW_H + ROW_GAP;
        this.chipHolder = new ChipHolder(x, chipY);
        this.chipHolder.setOnChange(this::fireOnChange);
    }

    public ChipFilterInput<F> setOnChange(@Nullable Runnable onChange) {
        this.onChange = onChange;
        return this;
    }

    /**
     * Stream of every committed filter, in insertion order. Each
     * {@link ChipFilter} pairs the filter type that was active when the
     * chip was created with the opaque value the suggestion (or raw text)
     * carried.
     */
    public Stream<ChipFilter<F>> filters() {
        return this.chipHolder.chips()
                .map(this::chipFilterOf)
                .filter(f -> f != null);
    }

    @SuppressWarnings("unchecked")
    private @Nullable ChipFilter<F> chipFilterOf(Chip chip) {
        Object data = chip.data();
        return (data instanceof ChipFilter<?> filter) ? (ChipFilter<F>) filter : null;
    }

    /** Currently-selected filter type (the cycle button's value). */
    public F currentMode() {
        return this.currentMode;
    }

    /** Raw text currently in the search field. */
    public String stagedText() {
        return this.searchField.getValue();
    }

    /** Direct access to the inner chip holder — useful for visit / clear / etc. */
    public ChipHolder chipHolder() {
        return this.chipHolder;
    }

    // -- Suggestion management ---------------------------------------------

    private void refreshSuggestions() {
        // Popup only shows while the user is typing; an unfocused or empty
        // field collapses it so the bare grid below stays visible.
        String input = this.searchField.getValue();
        if (!this.searchField.isFocused()) {
            this.activeSuggestions = List.of();
            return;
        }
        List<ChipSuggestion> result = this.provider.suggest(this.currentMode, input);
        if (result.size() > MAX_SUGGESTIONS) {
            result = result.subList(0, MAX_SUGGESTIONS);
        }
        this.activeSuggestions = result;
    }

    private void commitSuggestion(ChipSuggestion suggestion) {
        F mode = this.currentMode;
        Component category = this.modeLabel.apply(mode);
        this.chipHolder.addChip(b -> b
                .dotColor(suggestion.dotColor())
                .category(category)
                .value(suggestion.label())
                .dismissable()
                .data(new ChipFilter<>(mode, suggestion.value())));
        this.searchField.setValue("");
        this.activeSuggestions = List.of();
        // chipHolder's onChange already fires the dialog-level callback via
        // its own listener — no need to fire fireOnChange ourselves here.
    }

    private boolean handleEnter() {
        // Pick the topmost suggestion if there is one; otherwise commit the
        // raw text as a chip with no opaque value so free-text filters still
        // work for consumers that handle null.
        if (!this.activeSuggestions.isEmpty()) {
            commitSuggestion(this.activeSuggestions.get(0));
            return true;
        }
        String text = this.searchField.getValue().trim();
        if (text.isEmpty()) {
            return false;
        }
        F mode = this.currentMode;
        this.chipHolder.addChip(b -> b
                .dotColor(this.modeColor.apply(mode))
                .category(this.modeLabel.apply(mode))
                .value(Component.literal(text))
                .dismissable()
                .data(new ChipFilter<>(mode, text)));
        this.searchField.setValue("");
        this.activeSuggestions = List.of();
        return true;
    }

    // -- Geometry helpers --------------------------------------------------

    private int suggestionPopupY() {
        return this.getY() + SEARCH_ROW_H + SUGGESTION_GAP;
    }

    private int suggestionPopupHeight() {
        if (this.activeSuggestions.isEmpty()) {
            return 0;
        }
        return this.activeSuggestions.size() * SUGGESTION_ROW_H + 2 * SUGGESTION_PADDING;
    }

    /** Returns the index of the suggestion under the mouse, or -1 if none. */
    private int suggestionAt(double mouseX, double mouseY) {
        if (this.activeSuggestions.isEmpty()) {
            return -1;
        }
        int x = this.getX();
        int y = suggestionPopupY();
        int w = this.getWidth();
        int h = suggestionPopupHeight();
        if (mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + h) {
            return -1;
        }
        int offset = (int) (mouseY - y - SUGGESTION_PADDING);
        if (offset < 0) {
            return -1;
        }
        int idx = offset / SUGGESTION_ROW_H;
        return (idx >= 0 && idx < this.activeSuggestions.size()) ? idx : -1;
    }

    private static boolean within(double mouseX, double mouseY, AbstractWidget w) {
        return mouseX >= w.getX() && mouseX < w.getX() + w.getWidth()
                && mouseY >= w.getY() && mouseY < w.getY() + w.getHeight();
    }

    // -- Event dispatch ---------------------------------------------------

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Static bounds always count.
        if (super.isMouseOver(mouseX, mouseY)) {
            return true;
        }
        // Suggestion overlay extends below the widget; include it so the
        // screen's dispatch routes overlay clicks back into mouseClicked.
        if (!this.activeSuggestions.isEmpty()) {
            int y = suggestionPopupY();
            int h = suggestionPopupHeight();
            return mouseX >= this.getX() && mouseX < this.getX() + this.getWidth()
                    && mouseY >= y && mouseY < y + h;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.active || !this.visible) {
            return false;
        }
        double mx = event.x();
        double my = event.y();

        // Suggestion popup takes priority when visible — a click on it
        // commits a chip immediately.
        int sugIdx = suggestionAt(mx, my);
        if (sugIdx >= 0) {
            commitSuggestion(this.activeSuggestions.get(sugIdx));
            return true;
        }

        // Forward to sub-widgets in z-order: cycle button, search field, chip
        // holder. We have to set search-field focus by hand: AbstractWidget's
        // mouseClicked only fires onClick (which for EditBox is just cursor
        // positioning) — it doesn't toggle `focused`. The screen above us
        // sees `filterInput` as the leaf widget and never reaches in to call
        // setFocused on the search field directly, so it falls to us.
        if (within(mx, my, this.modeButton) && this.modeButton.mouseClicked(event, doubleClick)) {
            this.searchField.setFocused(false);
            this.activeSuggestions = List.of();
            return true;
        }
        if (within(mx, my, this.searchField)) {
            this.searchField.setFocused(true);
            this.searchField.mouseClicked(event, doubleClick);
            refreshSuggestions();
            return true;
        }
        if (this.chipHolder.mouseClicked(event, doubleClick)) {
            this.searchField.setFocused(false);
            this.activeSuggestions = List.of();
            return true;
        }

        // Click landed inside our bounds but on empty space — unfocus the
        // search field so the popup collapses, but still claim the click so
        // the screen keeps treating us as the focused widget rather than
        // walking on to another child.
        this.searchField.setFocused(false);
        this.activeSuggestions = List.of();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchField.isFocused()) {
            // Enter (main or numpad) commits the staged text / top suggestion
            // into a chip. Handled before forwarding so EditBox doesn't get
            // a chance to claim it (it wouldn't, but order is explicit).
            int key = event.key();
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (handleEnter()) {
                    return true;
                }
            }
            if (this.searchField.keyPressed(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.searchField.isFocused()) {
            return this.searchField.charTyped(event);
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            // Losing screen focus collapses the popup so the user doesn't see
            // a stale list hanging in the content area.
            this.searchField.setFocused(false);
            this.activeSuggestions = List.of();
        }
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        relayoutSubWidgets();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        relayoutSubWidgets();
    }

    private void relayoutSubWidgets() {
        int x = this.getX();
        int y = this.getY();
        this.modeButton.setX(x);
        this.modeButton.setY(y);
        this.searchField.setX(x + MODE_BUTTON_W + FILTER_SPACING);
        this.searchField.setY(y);
        this.chipHolder.setX(x);
        this.chipHolder.setY(y + SEARCH_ROW_H + ROW_GAP);
    }

    private void fireOnChange() {
        if (this.onChange != null) {
            this.onChange.run();
        }
    }

    // -- Render ------------------------------------------------------------

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Inline pass: sub-widgets only. The suggestion popup is drawn by
        // the host screen via {@link #renderSuggestionOverlay(GuiGraphicsExtractor, int, int)}
        // AFTER every other widget has rendered, so it always paints on top
        // of the grid that lives below us in the layout.
        this.modeButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.searchField.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.chipHolder.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Paints the suggestion popup if it has any active entries. Meant to be
     * called by the host screen after {@code super.extractRenderState} so it
     * lands on top of every other widget in the screen's render list.
     *
     * <p>Why this isn't inside {@link #extractWidgetRenderState}: stratum
     * advancement (via {@code graphics.nextStratum()}) is global — bumping
     * the stratum here would also bump every widget rendered after us into
     * the same higher stratum, and z-order within a stratum is render-call
     * order, so the grid below would still paint over the popup. Drawing
     * the popup last is the only way to guarantee it sits on top.
     */
    public void renderSuggestionOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.activeSuggestions.isEmpty()) {
            return;
        }
        renderSuggestionPopup(graphics, mouseX, mouseY);
    }

    private void renderSuggestionPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = this.getX();
        int y = suggestionPopupY();
        int w = this.getWidth();
        int h = suggestionPopupHeight();

        // Two-fill border trick — outer fill is the border color, inner fill
        // is the body, giving a crisp 1px outline without scan-line outlines.
        graphics.fill(x, y, x + w, y + h, POPUP_BORDER);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, POPUP_BG);

        Font font = Minecraft.getInstance().font;
        for (int i = 0; i < this.activeSuggestions.size(); i++) {
            ChipSuggestion s = this.activeSuggestions.get(i);
            int rowY = y + SUGGESTION_PADDING + i * SUGGESTION_ROW_H;

            boolean hovered = mouseX >= x + 1 && mouseX < x + w - 1
                    && mouseY >= rowY && mouseY < rowY + SUGGESTION_ROW_H;
            if (hovered) {
                graphics.fill(x + 1, rowY, x + w - 1, rowY + SUGGESTION_ROW_H, POPUP_HOVER);
            }

            // Colored dot + label, vertically centered within the row.
            int dotCx = x + SUGGESTION_PADDING + SUGGESTION_DOT_RADIUS;
            int dotCy = rowY + SUGGESTION_ROW_H / 2;
            fillDot(graphics, dotCx, dotCy, SUGGESTION_DOT_RADIUS, s.dotColor());

            int textX = dotCx + SUGGESTION_DOT_RADIUS + 1 + SUGGESTION_DOT_GAP;
            int textY = rowY + (SUGGESTION_ROW_H - font.lineHeight) / 2 + 1;
            graphics.text(font, s.label(), textX, textY, POPUP_TEXT, false);
        }
    }

    private static void fillDot(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
        // Same trimmed-corner square as Chip's dot for visual consistency.
        for (int dy = -r; dy <= r; dy++) {
            int trim = (Math.abs(dy) == r) ? 1 : 0;
            graphics.fill(cx - r + trim, cy + dy, cx + r + 1 - trim, cy + dy + 1, color);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    /**
     * A single chip's filter state: the filter type that was active when the
     * chip was committed plus the opaque value supplied by the suggestion
     * (or the raw text, for free-text Enter commits).
     *
     * <p>Consumers iterating {@link ChipFilterInput#filters()} cast
     * {@link #value} to whatever type they put there in the suggestion.
     */
    public record ChipFilter<F>(F mode, @Nullable Object value) {
    }
}

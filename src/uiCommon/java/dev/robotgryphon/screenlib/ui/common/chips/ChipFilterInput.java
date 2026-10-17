package dev.robotgryphon.screenlib.ui.common.chips;

import com.mojang.blaze3d.platform.InputConstants;
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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public class ChipFilterInput<F> extends AbstractWidget {
    private static final int MODE_BUTTON_W = 56;
    private static final int SEARCH_ROW_H = 16;
    private static final int ROW_GAP = 4;
    private static final int FILTER_SPACING = 4;

    private static final int SUGGESTION_ROW_H = 12;
    private static final int SUGGESTION_GAP = 4;
    private static final int SUGGESTION_PADDING = 3;
    private static final int SUGGESTION_DOT_RADIUS = 2;
    private static final int SUGGESTION_DOT_GAP = 5;
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
    private final Map<Chip, ChipFilter<F>> chipFilters = new IdentityHashMap<>();

    private F currentMode;
    private List<ChipSuggestion> activeSuggestions = List.of();
    private @Nullable Runnable onChange;

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
                            fireOnChange();
                        });

        int searchX = x + MODE_BUTTON_W + FILTER_SPACING;
        int searchW = Math.max(0, width - MODE_BUTTON_W - FILTER_SPACING);
        this.searchField = new EditBox(font, searchX, y, searchW, SEARCH_ROW_H, Component.empty());
        this.searchField.setHint(Component.literal("Search…"));
        this.searchField.setMaxLength(64);
        this.searchField.setResponder(s -> {
            refreshSuggestions();
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

    public Stream<ChipFilter<F>> filters() {
        return this.chipHolder.chips()
                .map(this.chipFilters::get)
                .filter(Objects::nonNull);
    }

    private void addFilterChip(int dotColor, Component value, ChipFilter<F> filter) {
        Chip chip = this.chipHolder.addChip(b -> b
                .dotColor(dotColor)
                .category(this.modeLabel.apply(filter.mode()))
                .value(value)
                .dismissable());
        this.chipFilters.put(chip, filter);
    }

    public F currentMode() {
        return this.currentMode;
    }

    public String stagedText() {
        return this.searchField.getValue();
    }

    public ChipHolder chipHolder() {
        return this.chipHolder;
    }

    private void refreshSuggestions() {
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
        this.addFilterChip(suggestion.dotColor(), suggestion.label(), new ChipFilter<>(this.currentMode, suggestion.value()));
        this.searchField.setValue("");
        this.activeSuggestions = List.of();
    }

    private boolean handleEnter() {
        if (!this.activeSuggestions.isEmpty()) {
            commitSuggestion(this.activeSuggestions.get(0));
            return true;
        }
        String text = this.searchField.getValue().trim();
        if (text.isEmpty()) {
            return false;
        }
        F mode = this.currentMode;
        this.addFilterChip(this.modeColor.apply(mode), Component.literal(text), new ChipFilter<>(mode, text));
        this.searchField.setValue("");
        this.activeSuggestions = List.of();
        return true;
    }

    private int suggestionPopupY() {
        return this.getY() + SEARCH_ROW_H + SUGGESTION_GAP;
    }

    private int suggestionPopupHeight() {
        if (this.activeSuggestions.isEmpty()) {
            return 0;
        }
        return this.activeSuggestions.size() * SUGGESTION_ROW_H + 2 * SUGGESTION_PADDING;
    }

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

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY)) {
            return true;
        }
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

        int sugIdx = suggestionAt(mx, my);
        if (sugIdx >= 0) {
            commitSuggestion(this.activeSuggestions.get(sugIdx));
            return true;
        }

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

        this.searchField.setFocused(false);
        this.activeSuggestions = List.of();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchField.isFocused()) {
            int key = event.key();
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
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
        Set<Chip> current = Collections.newSetFromMap(new IdentityHashMap<>());
        this.chipHolder.chips().forEach(current::add);
        this.chipFilters.keySet().retainAll(current);
        if (this.onChange != null) {
            this.onChange.run();
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.modeButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.searchField.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.chipHolder.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

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

            int dotCx = x + SUGGESTION_PADDING + SUGGESTION_DOT_RADIUS;
            int dotCy = rowY + SUGGESTION_ROW_H / 2;
            fillDot(graphics, dotCx, dotCy, SUGGESTION_DOT_RADIUS, s.dotColor());

            int textX = dotCx + SUGGESTION_DOT_RADIUS + 1 + SUGGESTION_DOT_GAP;
            int textY = rowY + (SUGGESTION_ROW_H - font.lineHeight) / 2 + 1;
            graphics.text(font, s.label(), textX, textY, POPUP_TEXT, false);
        }
    }

    private static void fillDot(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int trim = (Math.abs(dy) == r) ? 1 : 0;
            graphics.fill(cx - r + trim, cy + dy, cx + r + 1 - trim, cy + dy + 1, color);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    public record ChipFilter<F>(F mode, @Nullable Object value) {
    }
}

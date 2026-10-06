package dev.robotgryphon.screenlib.client.ui.widget.chips;

import java.util.List;

@FunctionalInterface
public interface ChipSuggestionsProvider<F> {
    List<ChipSuggestion> suggest(F filterType, String input);
}

package dev.robotgryphon.screenlib.ui.common.chips;

import java.util.List;

@FunctionalInterface
public interface ChipSuggestionsProvider<F> {
    List<ChipSuggestion> suggest(F filterType, String input);
}

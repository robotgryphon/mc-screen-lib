package dev.robotgryphon.screenlib.client.ui.widget.chips;

import java.util.List;

/**
 * Strategy for producing the suggestions a {@link ChipFilterInput} should
 * show under its search field as the user types.
 *
 * <p>Called by the host widget on every keystroke and on every filter-mode
 * change, with the currently-selected filter type and the user's raw input.
 * The implementation returns display-ready {@link ChipSuggestion}s already
 * filtered against the input — the widget doesn't second-guess the results
 * and renders them in the order returned.
 *
 * <p>The {@code F} type parameter is the host's filter-type enum (or any
 * value class — a {@code FilterMode} enum, a {@code String}, a registry
 * {@code Holder}, etc.). The provider receives the active value verbatim,
 * so a single provider can switch on it to produce different result kinds
 * for each filter type:
 *
 * <pre>{@code
 * ChipSuggestionsProvider<FilterMode> provider = (mode, input) -> switch (mode) {
 *     case NAME  -> matchingNodeNames(input);
 *     case INPUT -> matchingInputTypes(input); // returns PropertyDefinition-backed suggestions
 * };
 * }</pre>
 *
 * @param <F> the filter-type identifier the host widget cycles through
 */
@FunctionalInterface
public interface ChipSuggestionsProvider<F> {

    /**
     * Returns the suggestions to display for {@code input} under
     * {@code filterType}. Implementations should:
     * <ul>
     *   <li>Return an empty list when there are no matches (the widget hides
     *       the popup automatically).</li>
     *   <li>Cap the result list themselves if they don't want the popup to
     *       grow unboundedly — the widget will render whatever is returned.</li>
     *   <li>Filter case-insensitively if appropriate; the widget passes the
     *       user's input through verbatim.</li>
     * </ul>
     */
    List<ChipSuggestion> suggest(F filterType, String input);
}

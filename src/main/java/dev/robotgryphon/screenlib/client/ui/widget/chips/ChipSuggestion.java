package dev.robotgryphon.screenlib.client.ui.widget.chips;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Display-ready suggestion produced by a {@link ChipSuggestionsProvider} —
 * one entry in the popup list a {@link ChipFilterInput} shows under its
 * search field while the user is typing.
 *
 * <p>When the user picks a suggestion (click or Enter), the host
 * {@link ChipFilterInput} turns it into a {@link Chip} whose:
 * <ul>
 *   <li>category text = the active filter type's label (the input's responsibility);</li>
 *   <li>value text    = {@link #label};</li>
 *   <li>dot color     = {@link #dotColor};</li>
 *   <li>data slot     = a {@code ChipFilterInput.ChipFilter} wrapping the
 *                       suggestion's {@link #value} alongside the filter type.</li>
 * </ul>
 *
 * <p>{@link #value} is an opaque payload — the suggestions provider is free
 * to put whatever it needs there ({@code PropertyDefinition}, a registry
 * {@code Holder}, a raw {@code String}, etc.), and consumers casting back
 * out via {@code chipFilterInput.filters()} are responsible for the type
 * match.
 *
 * @param label    display text for the suggestion (and the chip's value text once committed)
 * @param dotColor ARGB color for the chip's leading dot
 * @param value    opaque payload the consumer recovers via {@code chip.data()}
 */
public record ChipSuggestion(Component label, int dotColor, @Nullable Object value) {

    /** Convenience for a suggestion that has no separate opaque value — the label is the value. */
    public static ChipSuggestion of(Component label, int dotColor) {
        return new ChipSuggestion(label, dotColor, null);
    }
}

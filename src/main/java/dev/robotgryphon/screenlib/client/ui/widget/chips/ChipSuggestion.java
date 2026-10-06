package dev.robotgryphon.screenlib.client.ui.widget.chips;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public record ChipSuggestion(Component label, int dotColor, @Nullable Object value) {
    public static ChipSuggestion of(Component label, int dotColor) {
        return new ChipSuggestion(label, dotColor, null);
    }
}

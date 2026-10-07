package dev.robotgryphon.screenlib.client.ui.widget.property;

import net.minecraft.resources.ResourceKey;

public final class PropertyValues {
    private PropertyValues() {}

    public static String display(Object value) {
        if (value instanceof ResourceKey<?> key) {
            return key.identifier().toString();
        }
        return value.toString();
    }
}

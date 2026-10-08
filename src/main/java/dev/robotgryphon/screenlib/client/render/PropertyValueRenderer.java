package dev.robotgryphon.screenlib.client.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public interface PropertyValueRenderer<T> {
    Class<T> valueType();

    int width();

    boolean render(GuiGraphicsExtractor graphics, T value, ScreenRectangle area, int mouseX, int mouseY, float alpha);

    default boolean tooltip(GuiGraphicsExtractor graphics, T value, ScreenRectangle area,
                            int mouseX, int mouseY, int screenX, int screenY) {
        return false;
    }
}

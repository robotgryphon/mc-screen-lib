package dev.robotgryphon.screenlib.ui.api.toolbar;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

public interface ToolbarButton {
    int SIZE = 10;

    Component tooltip(ToolbarContext context);

    void render(GuiGraphicsExtractor graphics, ScreenRectangle area, ToolbarContext context);

    void onClick(ToolbarContext context, ScreenRectangle area);

    default int width(ToolbarContext context) {
        return SIZE;
    }

    default boolean isActive(ToolbarContext context) {
        return false;
    }
}

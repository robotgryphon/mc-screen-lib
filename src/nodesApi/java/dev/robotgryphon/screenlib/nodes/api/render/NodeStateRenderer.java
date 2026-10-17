package dev.robotgryphon.screenlib.nodes.api.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;

public interface NodeStateRenderer<S> {
    Class<S> stateType();

    int width();

    int height();

    void render(GuiGraphicsExtractor graphics, S state, ScreenRectangle area, int mouseX, int mouseY, float alpha);

    default void mouseMoved(S state, ScreenRectangle area, double mouseX, double mouseY) {
    }

    default boolean mouseClicked(S state, ScreenRectangle area, MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    default boolean mouseScrolled(S state, ScreenRectangle area, double mouseX, double mouseY,
                                  double scrollX, double scrollY) {
        return false;
    }

    default void tooltip(GuiGraphicsExtractor graphics, S state, ScreenRectangle area,
                         int mouseX, int mouseY, int screenX, int screenY) {
    }
}

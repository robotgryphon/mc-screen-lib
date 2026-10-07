package dev.robotgryphon.screenlib.client.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.Optional;

public interface NodeStateRenderer<S> {
    void render(GuiGraphicsExtractor graphics, S state, RenderArea area, int mouseX, int mouseY, float alpha);

    default void mouseMoved(S state, RenderArea area, double mouseX, double mouseY) {
    }

    default boolean mouseClicked(S state, RenderArea area, MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    default boolean mouseScrolled(S state, RenderArea area, double mouseX, double mouseY,
                                  double scrollX, double scrollY) {
        return false;
    }

    default Optional<NodeTooltip> tooltip(S state, RenderArea area, double mouseX, double mouseY) {
        return Optional.empty();
    }
}

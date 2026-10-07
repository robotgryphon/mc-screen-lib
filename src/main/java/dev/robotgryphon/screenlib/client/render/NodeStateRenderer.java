package dev.robotgryphon.screenlib.client.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface NodeStateRenderer<S> {
    void render(GuiGraphicsExtractor graphics, S state, int x, int y, int width, int height, float alpha);
}

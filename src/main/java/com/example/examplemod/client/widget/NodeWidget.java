package com.example.examplemod.client.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class NodeWidget extends AbstractWidget {
    public NodeWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    @Override
    protected void renderWidget(@NonNull GuiGraphics guiGraphics, int i, int i1, float v) {

    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput narration) {

    }

    @Override
    protected void onDrag(MouseButtonEvent event, double mouseX, double mouseY) {
//        if (parent.connectingConnector != null) return;

        setX((int) (mouseX + event.x()));
        setY((int) (mouseY + event.y()));

        updateNodePos();
    }

    private void updateNodePos() {
//        node.guiX = getX() + width / 2;
//        node.guiY = getY() + height / 2;
    }
}

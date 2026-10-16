package dev.robotgryphon.testmod.client;

import dev.robotgryphon.screenlib.client.ui.widget.panel.Panel;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.ToolbarStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

public class PlaceholderPanel extends Panel {
    private static final Component EMPTY = Component.literal("Nothing here yet");

    public PlaceholderPanel(Component title) {
        super(160, 120, title);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, ScreenRectangle content, int mouseX, int mouseY) {
        graphics.text(Minecraft.getInstance().font, EMPTY, content.left(), content.top() + 4, ToolbarStyle.TEXT, false);
    }
}

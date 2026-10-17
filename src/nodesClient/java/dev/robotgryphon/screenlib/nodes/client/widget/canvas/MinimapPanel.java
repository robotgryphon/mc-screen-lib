package dev.robotgryphon.screenlib.nodes.client.widget.canvas;

import com.mojang.blaze3d.platform.InputConstants;
import dev.robotgryphon.screenlib.ui.api.panel.Panel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class MinimapPanel extends Panel {
    public static final int WIDTH = 128;
    public static final int HEIGHT = 96;

    private final CanvasPanel owner;
    private boolean dragging;

    public MinimapPanel(CanvasPanel owner) {
        super(WIDTH, HEIGHT, Component.literal("Minimap"));
        this.owner = owner;
    }

    private void centerOn(double x, double y) {
        CanvasMinimap.centerOn(this.owner.canvas(), this.contentArea(), x, y);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, ScreenRectangle content, int mouseX, int mouseY) {
        CanvasMinimap.render(graphics, content, this.owner.canvas());
    }

    @Override
    protected boolean contentClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        this.dragging = true;
        this.centerOn(event.x(), event.y());
        return true;
    }

    @Override
    protected boolean contentDragged(MouseButtonEvent event, double dx, double dy) {
        if (!this.dragging) {
            return false;
        }
        this.centerOn(event.x(), event.y());
        return true;
    }

    @Override
    protected boolean contentReleased(MouseButtonEvent event) {
        boolean wasDragging = this.dragging;
        this.dragging = false;
        return wasDragging;
    }
}

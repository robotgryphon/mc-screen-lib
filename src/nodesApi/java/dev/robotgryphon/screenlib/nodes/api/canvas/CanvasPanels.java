package dev.robotgryphon.screenlib.nodes.api.canvas;

import dev.robotgryphon.screenlib.nodes.api.spi.NodesClientProvider;
import dev.robotgryphon.screenlib.ui.api.panel.PanelType;
import net.minecraft.network.chat.Component;

public final class CanvasPanels {
    private CanvasPanels() {}

    public static PanelType<?> forMenu(CanvasMenu menu, Component title) {
        return NodesClientProvider.SLOT.get().canvasPanels(menu, title);
    }
}

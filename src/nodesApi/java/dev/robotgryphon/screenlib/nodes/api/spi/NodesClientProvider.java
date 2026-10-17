package dev.robotgryphon.screenlib.nodes.api.spi;

import dev.robotgryphon.screenlib.core.ServiceSlot;
import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasMenu;
import dev.robotgryphon.screenlib.ui.api.panel.PanelType;
import net.minecraft.network.chat.Component;

public interface NodesClientProvider {
    ServiceSlot<NodesClientProvider> SLOT = new ServiceSlot<>("ScreenLib nodes client provider");

    PanelType<?> canvasPanels(CanvasMenu menu, Component title);
}

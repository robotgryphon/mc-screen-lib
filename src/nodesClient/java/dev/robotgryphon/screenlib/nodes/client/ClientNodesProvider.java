package dev.robotgryphon.screenlib.nodes.client;

import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasMenu;
import dev.robotgryphon.screenlib.nodes.api.spi.NodesClientProvider;
import dev.robotgryphon.screenlib.nodes.client.widget.canvas.CanvasPanel;
import dev.robotgryphon.screenlib.nodes.common.menu.CanvasContainerMenu;
import dev.robotgryphon.screenlib.ui.api.panel.PanelType;
import net.minecraft.network.chat.Component;

final class ClientNodesProvider implements NodesClientProvider {
    @Override
    public PanelType<?> canvasPanels(CanvasMenu menu, Component title) {
        if (!(menu instanceof CanvasContainerMenu container)) {
            throw new IllegalArgumentException("Unsupported canvas menu: " + menu);
        }
        CanvasSession session = new CanvasSession(container);
        return host -> new CanvasPanel(host, session, title);
    }
}

package dev.robotgryphon.screenlib.client.ui.widget.toolbar;

import dev.robotgryphon.screenlib.client.ui.widget.ContextMenu;
import dev.robotgryphon.screenlib.client.ui.widget.panel.Panel;
import dev.robotgryphon.screenlib.client.ui.widget.panel.PanelType;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;

public interface ToolbarContext {
    void openMenu(ContextMenu menu);

    boolean isPanelOpen(ResourceKey<PanelType<?>> panel);

    void togglePanel(ResourceKey<PanelType<?>> panel);

    <P extends Panel> Optional<P> panel(Class<P> type);
}

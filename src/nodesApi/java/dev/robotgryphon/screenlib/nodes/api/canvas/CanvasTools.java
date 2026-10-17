package dev.robotgryphon.screenlib.nodes.api.canvas;

import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.ui.api.panel.PanelType;
import dev.robotgryphon.screenlib.ui.api.panel.PanelTypes;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarButton;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarButtons;
import net.minecraft.resources.ResourceKey;

public final class CanvasTools {
    private CanvasTools() {}

    public static final ResourceKey<ToolbarButton> FIT = button("fit");
    public static final ResourceKey<ToolbarButton> ZOOM = button("zoom");
    public static final ResourceKey<ToolbarButton> MINIMAP = button("minimap");
    public static final ResourceKey<ToolbarButton> PROVIDED = button("provided");

    public static final ResourceKey<PanelType<?>> ADD_NODE_PANEL =
            ResourceKey.create(PanelTypes.REGISTRY_KEY, ScreenLib.id("add_node"));
    public static final ResourceKey<PanelType<?>> PROVIDED_PANEL =
            ResourceKey.create(PanelTypes.REGISTRY_KEY, ScreenLib.id("provided"));

    private static ResourceKey<ToolbarButton> button(String name) {
        return ResourceKey.create(ToolbarButtons.REGISTRY_KEY, ScreenLib.id(name));
    }
}

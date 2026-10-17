package dev.robotgryphon.testmod.client;

import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasTools;
import dev.robotgryphon.screenlib.ui.api.UiHost;
import dev.robotgryphon.screenlib.ui.api.layout.HorizontalAnchor;
import dev.robotgryphon.screenlib.ui.api.layout.UiLayout;
import dev.robotgryphon.screenlib.ui.api.layout.VerticalAnchor;
import dev.robotgryphon.screenlib.ui.api.panel.PanelType;
import dev.robotgryphon.screenlib.ui.api.panel.PanelTypes;
import dev.robotgryphon.screenlib.ui.api.toolbar.IconToolbarButton;
import dev.robotgryphon.screenlib.ui.api.toolbar.Toolbars;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarButton;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarButtons;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarIcons;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarOrientation;
import dev.robotgryphon.testmod.common.TestMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class TestToolbars {
    private TestToolbars() {}

    public static final DeferredRegister<ToolbarButton> BUTTONS = DeferredRegister.create(ToolbarButtons.REGISTRY_KEY, TestMod.MOD_ID);

    public static final DeferredRegister<PanelType<?>> PANELS = DeferredRegister.create(PanelTypes.REGISTRY_KEY, TestMod.MOD_ID);

    public static final ResourceKey<PanelType<?>> FAVORITES_PANEL = panel("favorites", "Favorite nodes");

    public static final ResourceKey<ToolbarButton> FAVORITES = register("favorites",
            IconToolbarButton.panelTab(ToolbarIcons.FAVORITES, Component.literal("Favorite nodes"), FAVORITES_PANEL));
    public static final ResourceKey<ToolbarButton> ADD_NODE = register("add_node",
            IconToolbarButton.panelTab(ToolbarIcons.ADD_NODE, Component.literal("Add node"), CanvasTools.ADD_NODE_PANEL));

    private static ResourceKey<PanelType<?>> panel(String name, String title) {
        PanelType<PlaceholderPanel> type = host -> new PlaceholderPanel(Component.literal(title));
        return PANELS.register(name, () -> type).key();
    }

    private static ResourceKey<ToolbarButton> register(String name, ToolbarButton button) {
        return BUTTONS.register(name, () -> button).key();
    }

    public static void addTo(UiLayout layout, UiHost host) {
        layout.anchor(Toolbars.create(host, ToolbarOrientation.HORIZONTAL, List.of(List.of(CanvasTools.PROVIDED, FAVORITES, ADD_NODE))),
                HorizontalAnchor.LEFT, VerticalAnchor.TOP, 5, 5);
    }
}

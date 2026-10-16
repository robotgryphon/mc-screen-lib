package dev.robotgryphon.testmod.client;

import dev.robotgryphon.screenlib.client.ui.core.UiHost;
import dev.robotgryphon.screenlib.client.ui.core.layout.HorizontalAnchor;
import dev.robotgryphon.screenlib.client.ui.core.layout.UiScreenLayout;
import dev.robotgryphon.screenlib.client.ui.core.layout.VerticalAnchor;
import dev.robotgryphon.screenlib.client.ui.widget.canvas.CanvasTools;
import dev.robotgryphon.screenlib.client.ui.widget.panel.PanelType;
import dev.robotgryphon.screenlib.client.ui.widget.panel.PanelTypes;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.IconToolbarButton;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.Toolbar;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.ToolbarButton;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.ToolbarButtons;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.ToolbarIcons;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.ToolbarOrientation;
import dev.robotgryphon.testmod.TestMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class TestToolbars {
    private TestToolbars() {}

    public static final DeferredRegister<ToolbarButton> BUTTONS = DeferredRegister.create(ToolbarButtons.REGISTRY_KEY, TestMod.MOD_ID);

    public static final DeferredRegister<PanelType<?>> PANELS = DeferredRegister.create(PanelTypes.REGISTRY_KEY, TestMod.MOD_ID);

    public static final ResourceKey<PanelType<?>> PROVIDED_PANEL = panel("provided", "Provided nodes");
    public static final ResourceKey<PanelType<?>> FAVORITES_PANEL = panel("favorites", "Favorite nodes");

    public static final ResourceKey<ToolbarButton> PROVIDED = register("provided",
            IconToolbarButton.panelTab(ToolbarIcons.PROVIDED, Component.literal("Provided nodes"), PROVIDED_PANEL));
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

    public static void addTo(UiScreenLayout layout, UiHost host) {
        layout.anchor(new Toolbar(host, ToolbarOrientation.HORIZONTAL, List.of(List.of(PROVIDED, FAVORITES, ADD_NODE))),
                HorizontalAnchor.LEFT, VerticalAnchor.TOP, 5, 5);
    }
}

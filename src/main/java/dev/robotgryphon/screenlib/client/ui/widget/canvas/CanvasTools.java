package dev.robotgryphon.screenlib.client.ui.widget.canvas;

import dev.robotgryphon.screenlib.ScreenLib;
import dev.robotgryphon.screenlib.client.ui.widget.ContextMenu;
import dev.robotgryphon.screenlib.client.ui.widget.panel.PanelType;
import dev.robotgryphon.screenlib.client.ui.widget.panel.PanelTypes;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.IconToolbarButton;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.Toolbar;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.ToolbarButton;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.ToolbarButtons;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.ToolbarContext;
import dev.robotgryphon.screenlib.client.ui.widget.toolbar.ToolbarIcons;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class CanvasTools {
    private CanvasTools() {}

    private static final float[] ZOOM_PRESETS = {0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 3f, 4f};

    private static final DeferredRegister<ToolbarButton> BUTTONS = DeferredRegister.create(ToolbarButtons.REGISTRY_KEY, ScreenLib.MOD_ID);

    private static final DeferredRegister<PanelType<?>> PANELS = DeferredRegister.create(PanelTypes.REGISTRY_KEY, ScreenLib.MOD_ID);

    public static final ResourceKey<PanelType<?>> ADD_NODE_PANEL = panel("add_node", AddNodePanel::new);

    public static final ResourceKey<ToolbarButton> FIT = button("fit", IconToolbarButton
            .builder(ToolbarIcons.FIT, Component.literal("Fit to content"))
            .onClick((context, area) -> context.panel(CanvasPanel.class).ifPresent(panel -> panel.canvas().fitToContent()))
            .build());

    public static final ResourceKey<ToolbarButton> ZOOM = button("zoom", IconToolbarButton
            .builder(ToolbarIcons.ZOOM, Component.literal("Zoom"))
            .onClick(CanvasTools::openZoomMenu)
            .build());

    public static final ResourceKey<ToolbarButton> MINIMAP = button("minimap", IconToolbarButton
            .builder(ToolbarIcons.MINIMAP, Component.literal("Toggle minimap"))
            .active(context -> context.panel(CanvasPanel.class).map(CanvasPanel::isMinimapOpen).orElse(false))
            .onClick((context, area) -> context.panel(CanvasPanel.class).ifPresent(CanvasPanel::toggleMinimap))
            .build());

    public static void register(IEventBus modBus) {
        BUTTONS.register(modBus);
        PANELS.register(modBus);
    }

    private static ResourceKey<PanelType<?>> panel(String name, PanelType<?> type) {
        return PANELS.register(name, () -> type).key();
    }

    private static ResourceKey<ToolbarButton> button(String name, ToolbarButton button) {
        return BUTTONS.register(name, () -> button).key();
    }

    private static void openZoomMenu(ToolbarContext context, ScreenRectangle area) {
        context.panel(CanvasPanel.class).ifPresent(panel -> {
            List<ContextMenu.Item> items = new ArrayList<>();
            for (float preset : ZOOM_PRESETS) {
                items.add(new ContextMenu.Item(Component.literal(Math.round(preset * 100) + "%"),
                        () -> panel.canvas().zoomTo(preset)));
            }
            items.add(new ContextMenu.Item(Component.literal("Fit to content"), () -> panel.canvas().fitToContent()));
            context.openMenu(ContextMenu.leftOf(area.left() - Toolbar.GAP * 2, area.bottom(), items));
        });
    }
}

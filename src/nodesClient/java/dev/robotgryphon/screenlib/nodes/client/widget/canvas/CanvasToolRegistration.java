package dev.robotgryphon.screenlib.nodes.client.widget.canvas;

import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasTools;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarStyle;
import dev.robotgryphon.screenlib.core.ScreenLib;
import dev.robotgryphon.screenlib.ui.api.widget.ContextMenu;
import dev.robotgryphon.screenlib.ui.api.panel.PanelType;
import dev.robotgryphon.screenlib.ui.api.panel.PanelTypes;
import dev.robotgryphon.screenlib.ui.api.toolbar.IconToolbarButton;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarButton;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarButtons;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarContext;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarIcons;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class CanvasToolRegistration {
    private CanvasToolRegistration() {}

    private static final float[] ZOOM_PRESETS = {0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 3f, 4f};

    private static final DeferredRegister<ToolbarButton> BUTTONS = DeferredRegister.create(ToolbarButtons.REGISTRY_KEY, ScreenLib.MOD_ID);

    private static final DeferredRegister<PanelType<?>> PANELS = DeferredRegister.create(PanelTypes.REGISTRY_KEY, ScreenLib.MOD_ID);

    public static void register(IEventBus modBus) {
        PANELS.register(CanvasTools.ADD_NODE_PANEL.identifier().getPath(), () -> (PanelType<AddNodePanel>) AddNodePanel::new);
        button(CanvasTools.FIT, IconToolbarButton
                .builder(ToolbarIcons.FIT, Component.literal("Fit to content"))
                .onClick((context, area) -> context.panel(CanvasPanel.class).ifPresent(panel -> panel.canvas().fitToContent()))
                .build());
        button(CanvasTools.ZOOM, IconToolbarButton
                .builder(ToolbarIcons.ZOOM, Component.literal("Zoom"))
                .onClick(CanvasToolRegistration::openZoomMenu)
                .build());
        button(CanvasTools.MINIMAP, IconToolbarButton
                .builder(ToolbarIcons.MINIMAP, Component.literal("Toggle minimap"))
                .active(context -> context.panel(CanvasPanel.class).map(CanvasPanel::isMinimapOpen).orElse(false))
                .onClick((context, area) -> context.panel(CanvasPanel.class).ifPresent(CanvasPanel::toggleMinimap))
                .build());
        BUTTONS.register(modBus);
        PANELS.register(modBus);
    }

    private static void button(ResourceKey<ToolbarButton> key, ToolbarButton button) {
        BUTTONS.register(key.identifier().getPath(), () -> button);
    }

    private static void openZoomMenu(ToolbarContext context, ScreenRectangle area) {
        context.panel(CanvasPanel.class).ifPresent(panel -> {
            List<ContextMenu.Item> items = new ArrayList<>();
            for (float preset : ZOOM_PRESETS) {
                items.add(new ContextMenu.Item(Component.literal(Math.round(preset * 100) + "%"),
                        () -> panel.canvas().zoomTo(preset)));
            }
            items.add(new ContextMenu.Item(Component.literal("Fit to content"), () -> panel.canvas().fitToContent()));
            context.openMenu(ContextMenu.leftOf(area.left() - ToolbarStyle.GAP * 2, area.bottom(), items));
        });
    }
}

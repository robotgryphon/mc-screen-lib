package dev.robotgryphon.screenlib.ui.api.spi;

import dev.robotgryphon.screenlib.core.ServiceSlot;
import dev.robotgryphon.screenlib.ui.api.UiScreenBuilder;
import dev.robotgryphon.screenlib.ui.api.UiScreens;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarButton;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarContext;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarOrientation;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;

public interface UiProvider {
    ServiceSlot<UiProvider> SLOT = new ServiceSlot<>("ScreenLib UI provider");

    UiScreenBuilder screen(Component title);

    <M extends AbstractContainerMenu> void registerMenuScreen(RegisterMenuScreensEvent event, MenuType<M> type,
                                                              UiScreens.MenuScreenFactory<M> factory);

    AbstractWidget toolbar(ToolbarContext context, ToolbarOrientation orientation,
                           List<List<ResourceKey<ToolbarButton>>> groups);
}

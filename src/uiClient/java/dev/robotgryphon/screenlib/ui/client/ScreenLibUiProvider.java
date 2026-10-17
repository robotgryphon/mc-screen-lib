package dev.robotgryphon.screenlib.ui.client;

import dev.robotgryphon.screenlib.ui.api.UiScreenBuilder;
import dev.robotgryphon.screenlib.ui.api.UiScreens;
import dev.robotgryphon.screenlib.ui.api.spi.UiProvider;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarButton;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarContext;
import dev.robotgryphon.screenlib.ui.api.toolbar.ToolbarOrientation;
import dev.robotgryphon.screenlib.ui.client.toolbar.Toolbar;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;

final class ScreenLibUiProvider implements UiProvider {
    @Override
    public UiScreenBuilder screen(Component title) {
        return UiScreen.builder(title);
    }

    @Override
    public <M extends AbstractContainerMenu> void registerMenuScreen(RegisterMenuScreensEvent event, MenuType<M> type,
                                                                     UiScreens.MenuScreenFactory<M> factory) {
        event.<M, MenuUiScreen<M>>register(type, (menu, inventory, title) -> builder(factory.create(menu, inventory, title)).build(menu));
    }

    @Override
    public AbstractWidget toolbar(ToolbarContext context, ToolbarOrientation orientation,
                                  List<List<ResourceKey<ToolbarButton>>> groups) {
        return new Toolbar(context, orientation, groups);
    }

    private static UiScreen.Builder builder(UiScreenBuilder builder) {
        if (builder instanceof UiScreen.Builder screen) {
            return screen;
        }
        throw new IllegalArgumentException("Unsupported screen builder: " + builder);
    }
}

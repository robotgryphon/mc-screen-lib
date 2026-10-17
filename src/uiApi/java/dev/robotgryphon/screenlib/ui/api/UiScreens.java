package dev.robotgryphon.screenlib.ui.api;

import dev.robotgryphon.screenlib.ui.api.spi.UiProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class UiScreens {
    private UiScreens() {}

    @FunctionalInterface
    public interface MenuScreenFactory<M extends AbstractContainerMenu> {
        UiScreenBuilder create(M menu, Inventory inventory, Component title);
    }

    public static UiScreenBuilder builder(Component title) {
        return UiProvider.SLOT.get().screen(title);
    }

    public static <M extends AbstractContainerMenu> void register(RegisterMenuScreensEvent event, MenuType<M> type,
                                                                  MenuScreenFactory<M> factory) {
        UiProvider.SLOT.get().registerMenuScreen(event, type, factory);
    }
}

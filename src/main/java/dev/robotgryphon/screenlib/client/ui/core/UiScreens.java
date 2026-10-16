package dev.robotgryphon.screenlib.client.ui.core;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class UiScreens {
    private UiScreens() {}

    @FunctionalInterface
    public interface MenuScreenFactory<M extends AbstractContainerMenu> {
        UiScreen.Builder create(M menu, Inventory inventory, Component title);
    }

    public static <M extends AbstractContainerMenu> void register(RegisterMenuScreensEvent event, MenuType<M> type,
                                                                  MenuScreenFactory<M> factory) {
        event.<M, MenuUiScreen<M>>register(type, (menu, inventory, title) -> factory.create(menu, inventory, title).build(menu));
    }
}

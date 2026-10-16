package dev.robotgryphon.screenlib.client.ui.core;

import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class MenuUiScreen<M extends AbstractContainerMenu> extends UiScreen implements MenuAccess<M> {
    private final M menu;

    MenuUiScreen(UiScreen.Builder builder, M menu) {
        super(builder);
        this.menu = menu;
    }

    @Override
    public M getMenu() {
        return this.menu;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        } else {
            super.onClose();
        }
    }
}

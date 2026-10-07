package dev.robotgryphon.testmod.menu;

import dev.robotgryphon.screenlib.menu.CanvasMenu;
import dev.robotgryphon.screenlib.menu.CanvasSource;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record TestScreenMenuProvider(CanvasSource source) implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new TestScreenMenu(containerId, inv, this.source);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        CanvasMenu.writeClientSideData(menu, buffer);
    }
}

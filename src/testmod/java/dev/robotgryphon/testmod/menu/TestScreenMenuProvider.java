package dev.robotgryphon.testmod.menu;

import dev.robotgryphon.screenlib.graph.canvas.CanvasSupplier;
import dev.robotgryphon.screenlib.menu.CanvasMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.function.Predicate;

public record TestScreenMenuProvider(CanvasSupplier source, Predicate<Player> validFor) implements MenuProvider {
    public TestScreenMenuProvider(CanvasSupplier source) {
        this(source, player -> true);
    }

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new TestScreenMenu(containerId, inv, this.source, this, this.validFor);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        CanvasMenu.writeClientSideData(menu, buffer);
    }
}

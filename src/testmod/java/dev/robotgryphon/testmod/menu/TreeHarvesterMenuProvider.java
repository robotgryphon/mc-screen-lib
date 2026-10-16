package dev.robotgryphon.testmod.menu;

import dev.robotgryphon.screenlib.graph.canvas.CanvasSupplier;
import dev.robotgryphon.screenlib.menu.CanvasMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

public record TreeHarvesterMenuProvider(CanvasSupplier source, BlockEntity blockEntity, Component title)
        implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return this.title;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TreeHarvesterMenu(containerId, inventory, this.source, this, this.blockEntity);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        CanvasMenu.writeClientSideData(menu, buffer);
    }
}

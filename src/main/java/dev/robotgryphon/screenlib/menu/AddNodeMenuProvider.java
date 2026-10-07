package dev.robotgryphon.screenlib.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record AddNodeMenuProvider(CanvasSource source, AddNodeData data) implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return Component.literal("Add Node");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new AddNodeMenu(containerId, inv, this.source, this.data);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        AddNodeData.STREAM_CODEC.encode(buffer, ((AddNodeMenu) menu).data());
    }
}

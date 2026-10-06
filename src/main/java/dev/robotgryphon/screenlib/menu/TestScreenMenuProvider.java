package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.graph.CanvasState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

public record TestScreenMenuProvider(Level level) implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new TestScreenMenu(containerId, inv, this.level);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        TestScreenMenu testMenu = (TestScreenMenu) menu;
        ByteBufCodecs.fromCodecWithRegistries(CanvasState.CODEC)
                .encode(buffer, testMenu.state());
    }
}

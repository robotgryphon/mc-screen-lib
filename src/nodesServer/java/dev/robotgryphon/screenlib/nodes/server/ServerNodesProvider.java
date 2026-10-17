package dev.robotgryphon.screenlib.nodes.server;

import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasDefinition;
import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasMenu;
import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasSupplier;
import dev.robotgryphon.screenlib.nodes.api.spi.NodesProvider;
import dev.robotgryphon.screenlib.nodes.common.menu.CanvasContainerMenu;
import dev.robotgryphon.screenlib.nodes.server.canvas.ServerCanvasSupplier;
import dev.robotgryphon.screenlib.nodes.server.menu.ServerCanvasMenuBackend;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

final class ServerNodesProvider implements NodesProvider {
    @Override
    public CanvasSupplier canvasSupplier(CanvasDefinition canvas) {
        return new ServerCanvasSupplier(canvas);
    }

    @Override
    public MenuType<CanvasMenu> createMenuType() {
        AtomicReference<MenuType<CanvasMenu>> self = new AtomicReference<>();
        MenuType<CanvasMenu> type = IMenuTypeExtension.create(
                (containerId, inventory, data) -> new CanvasContainerMenu(self.get(), containerId, data));
        self.set(type);
        return type;
    }

    @Override
    public void openMenu(ServerPlayer player, MenuType<CanvasMenu> type, CanvasSupplier canvas, Component title,
                         Predicate<Player> stillValid) {
        player.openMenu(new Opener(type, canvas, title, stillValid));
    }

    private record Opener(MenuType<CanvasMenu> type, CanvasSupplier canvas, Component title,
                          Predicate<Player> stillValid) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return this.title;
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new CanvasContainerMenu(this.type, containerId, ServerCanvasMenuBackend.of(this.canvas, player),
                    this.stillValid);
        }

        @Override
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            CanvasContainerMenu.writeClientSideData(menu, buffer);
        }
    }
}

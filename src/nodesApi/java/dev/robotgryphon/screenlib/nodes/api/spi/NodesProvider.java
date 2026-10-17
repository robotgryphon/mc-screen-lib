package dev.robotgryphon.screenlib.nodes.api.spi;

import dev.robotgryphon.screenlib.core.ServiceSlot;
import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasDefinition;
import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasMenu;
import dev.robotgryphon.screenlib.nodes.api.canvas.CanvasSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Predicate;

public interface NodesProvider {
    ServiceSlot<NodesProvider> SLOT = new ServiceSlot<>("ScreenLib nodes provider");

    CanvasSupplier canvasSupplier(CanvasDefinition canvas);

    MenuType<CanvasMenu> createMenuType();

    void openMenu(ServerPlayer player, MenuType<CanvasMenu> type, CanvasSupplier canvas, Component title,
                  Predicate<Player> stillValid);
}

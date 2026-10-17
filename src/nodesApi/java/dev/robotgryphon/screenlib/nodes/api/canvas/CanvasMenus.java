package dev.robotgryphon.screenlib.nodes.api.canvas;

import dev.robotgryphon.screenlib.nodes.api.spi.NodesProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Predicate;

public final class CanvasMenus {
    private CanvasMenus() {}

    public static MenuType<CanvasMenu> createType() {
        return NodesProvider.SLOT.get().createMenuType();
    }

    public static void open(ServerPlayer player, MenuType<CanvasMenu> type, CanvasSupplier canvas, Component title,
                            Predicate<Player> stillValid) {
        NodesProvider.SLOT.get().openMenu(player, type, canvas, title, stillValid);
    }
}

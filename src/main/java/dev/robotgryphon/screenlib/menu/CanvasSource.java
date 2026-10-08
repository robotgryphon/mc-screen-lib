package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.graph.CanvasState;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public interface CanvasSource {
    Level level();

    CanvasState load();

    void save(CanvasState state);

    MenuProvider menu();

    default List<HostedNode> hostedNodes() {
        return List.of();
    }

    default boolean stillValid(Player player) {
        return true;
    }
}

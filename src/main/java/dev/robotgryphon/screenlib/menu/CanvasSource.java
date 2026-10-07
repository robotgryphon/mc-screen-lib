package dev.robotgryphon.screenlib.menu;

import dev.robotgryphon.screenlib.graph.CanvasState;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface CanvasSource {
    Level level();

    CanvasState load();

    void save(CanvasState state);

    MenuProvider menu();

    default boolean stillValid(Player player) {
        return true;
    }
}

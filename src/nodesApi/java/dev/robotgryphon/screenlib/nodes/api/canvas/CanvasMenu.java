package dev.robotgryphon.screenlib.nodes.api.canvas;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jspecify.annotations.Nullable;

public abstract class CanvasMenu extends AbstractContainerMenu {
    protected CanvasMenu(@Nullable MenuType<?> type, int containerId) {
        super(type, containerId);
    }
}

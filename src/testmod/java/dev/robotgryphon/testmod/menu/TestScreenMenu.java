package dev.robotgryphon.testmod.menu;

import dev.robotgryphon.screenlib.menu.CanvasMenu;
import dev.robotgryphon.screenlib.menu.CanvasSource;
import dev.robotgryphon.testmod.TestModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class TestScreenMenu extends CanvasMenu {
    public TestScreenMenu(int containerId, Inventory inv, CanvasSource source) {
        super(TestModMenus.TEST_SCREEN.get(), containerId, inv, source);
    }

    public TestScreenMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf data) {
        super(TestModMenus.TEST_SCREEN.get(), containerId, data);
    }
}

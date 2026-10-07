package dev.robotgryphon.testmod.client;

import dev.robotgryphon.screenlib.client.ui.CanvasScreen;
import dev.robotgryphon.testmod.menu.TestScreenMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TestScreen extends CanvasScreen<TestScreenMenu> {
    public TestScreen(TestScreenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
